package version2.prototype.processor.IMERG;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Hashtable;

import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.processor.Convert;
import version2.prototype.processor.ProcessData;
import version2.prototype.util.GdalUtils;
import version2.prototype.util.UnGunZip;

public class IMERGConvert extends Convert
{
    private Integer noDataValue;

    private final double [] gTran = new double[] {
            -179.9499969,
            0.1000000,
            0.0000000,
            89.9499969,
            0.0000000,
            -0.1
    };

    public IMERGConvert(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        noDataValue = data.getNoDataValue();
    }

    @Override
    // scale the precipitation rate
    public void convertFiles() throws Exception
    {
        GdalUtils.register();
        synchronized (GdalUtils.lockObject)
        {

            if (inputFiles == null)
            { System.out.println("file does not exist");}

            for (File f:inputFiles){

                DataInputStream dis = new DataInputStream(new FileInputStream(f));
                String fileBaseName = FilenameUtils.getBaseName(f.getName());

                Dataset inputDS = null;
                String fName = f.getAbsolutePath();

                if (FilenameUtils.getExtension(f.getName()).equalsIgnoreCase("gz"))
                {
                    String orgfile = fName;
                    fName = orgfile.substring(0, orgfile. lastIndexOf('.'));
                    new UnGunZip(orgfile, fName);
                }
                System.out.println("fName "+fName);
                inputDS = gdal.Open(fName);

                Band b = inputDS.GetRasterBand(1);

                int xSize = b.getXSize();
                int ySize = b.getYSize();

                int[] inputArray = new int[xSize*ySize];
                double[] outArray = new double[xSize*ySize];
                b.ReadRaster(0, 0, xSize, ySize, inputArray);
                inputDS.delete();

                // accumulation = value * 0.1 * 24
                for(int i = 0; i < (xSize*ySize); i++)
                {
                    // 9999 for missing value
                    if (inputArray[i] < 9999) {
                        outArray[i] = inputArray[i] * 0.1 * 24;
                    } else {
                        outArray[i] = inputArray[i];
                    }
                }

                File outputFile = new File (outputFolder + File.separator + fileBaseName +".tif");

                try {
                    outputFile.createNewFile();
                } catch (IOException e) {
                    ErrorLog.add(Config.getInstance(), "IMERG.convert error while creating new file.", e);
                }

                Dataset outputDS = gdal.GetDriverByName("GTiff").Create(
                        outputFile.getAbsolutePath(),
                        xSize, ySize,
                        1,
                        gdalconstConstants.GDT_Float32);

                outputDS.SetGeoTransform(gTran);
                outputDS.SetProjection("GEOGCS[\"GCS_GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],PRIMEM[\"'Greenwich'\",0.0],UNIT[\"Degree'\",0.0174532925199433]]");

                outputDS.GetRasterBand(1).WriteRaster(0, 0, xSize, ySize, outArray);

                outputDS.GetRasterBand(1).SetNoDataValue(noDataValue);
                outputDS.GetRasterBand(1).ComputeStatistics(false);
                outputDS.delete();

                dis.close();
            }
        }
    }
}