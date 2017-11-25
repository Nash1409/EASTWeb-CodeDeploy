package version2.prototype.summary.temporal.MergeStrategies;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;


import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.summary.temporal.MergeStrategy;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.GdalUtils;
import version2.prototype.util.Schemas;
import version2.prototype.Process;

/**
 * Concrete MergeStrategy. Represents a merging based on averages of values in raster files.
 *
 * @author michael.devos
 *
 */
public class AvgGdalRasterFileMerge implements MergeStrategy {

    @Override
    public DataFileMetaData Merge(Config configInstance, Connection con, Process process, ProjectInfoFile projectInfo, String pluginName, String indexNm, LocalDate firstDate, File[] rasterFiles,
            String outputFilePath) throws Exception {

        DataFileMetaData mergedFile = null;
        GdalUtils.register();

        new File(outputFilePath).delete();

        synchronized (GdalUtils.lockObject) {
            // Create output copy based on rasterFiles[0]
            Dataset rasterDs = gdal.Open(rasterFiles[0].getPath());
            GdalUtils.errorCheck();
            Dataset avgRasterDs = gdal.GetDriverByName("GTiff").CreateCopy(outputFilePath, rasterDs);
            GdalUtils.errorCheck();
            rasterDs.delete();

            // Populate avgArray with first file's data
            int xSize = avgRasterDs.GetRasterXSize();
            int ySize = avgRasterDs.GetRasterYSize();
            double[] avgArray = new double[ySize * xSize];
            if(avgRasterDs.GetRasterBand(1).ReadRaster(0, 0, xSize, ySize, avgArray) != 0) {
                ErrorLog.add(process, "Can't read the Raster band : " + rasterFiles[0].getPath(), new Exception("Can't read the Raster band : " + rasterFiles[0].getPath()));
            }

            // Initialize arrays
            int index;
            double[] tempArray = new double[ySize * xSize];
            int[] pixelsPerPos = new int[tempArray.length];
            /*for(int y=0; y < ySize; y++)
            {
                for(int x=0; x < xSize; x++)
                {
                    index = y * xSize + x;
                    avgArray[index] = 0;
                    pixelsPerPos[index] = 0;
                }
            }*/
            // Above lines can be replaced by the following lines - roberto.villegas
            Arrays.fill(tempArray, 0.0);
            Arrays.fill(pixelsPerPos, 0);

            // Sum up values from the rest of the files
            for(int i=1; i < rasterFiles.length; i++)
            {
                rasterDs = gdal.Open(rasterFiles[i].getPath());
                if(rasterDs.GetRasterBand(1).ReadRaster(0, 0, xSize, ySize, tempArray) != 0) {
                    ErrorLog.add(process, "Can't read the Raster band : " + rasterFiles[i].getPath(), new Exception("Can't read the Raster band : " + rasterFiles[i].getPath()));
                }

                for(int y=0; y < ySize; y++)
                {
                    for(int x=0; x < xSize; x++)
                    {
                        index = y * xSize + x;
                        if(tempArray[index] != process.pluginMetaData.NoDataValue) {
                            avgArray[index] += tempArray[index];
                            pixelsPerPos[index] += 1;
                        }
                    }
                }
                rasterDs.delete();
            }

            // Average values
            for(int y=0; y < ySize; y++) {
                for(int x=0; x < xSize; x++) {
                    index = y * xSize + x;
                    if(pixelsPerPos[index] != 0) {
                        avgArray[index] = avgArray[index] / pixelsPerPos[index];
                    } else {
                        avgArray[index] = process.pluginMetaData.NoDataValue;
                    }
                }
            }

            // Write averaged array to raster file
            //            synchronized (GdalUtils.lockObject) {
            avgRasterDs.GetRasterBand(1).WriteRaster(0, 0, xSize, ySize, avgArray);
            avgRasterDs.GetRasterBand(1).SetNoDataValue(process.pluginMetaData.NoDataValue);
            //            }

            tempArray = null;
            avgArray = null;
            avgRasterDs.delete();


            //Statement stmt1 = con1.createStatement();
            Statement stmt = con.createStatement();
            try{
                //Schemas.getDateGroupID(configInstance.getGlobalSchema(), firstDate, stmt1);
                int dateGroupID = Schemas.getDateGroupID(configInstance.getGlobalSchema(), firstDate, stmt);
                mergedFile = new DataFileMetaData(outputFilePath, dateGroupID, firstDate.getYear(), firstDate.getDayOfYear(), indexNm);
            } finally {
                stmt.close();
                //stmt1.close();
                //con1.close();
            }
            con.close();
        }
        return mergedFile;
    }
}

