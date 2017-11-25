package version2.prototype.processor.ModisNBARV6;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.commons.io.FilenameUtils;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import version2.prototype.processor.Filter;
import version2.prototype.processor.ProcessData;
import version2.prototype.util.GdalUtils;

public class ModisNBARV6Filter extends Filter{
    private Integer noDataValue;
    private int [] dataBands;

    public ModisNBARV6Filter(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        noDataValue = data.getNoDataValue();
        dataBands = data.getDataBands();
    }

    @Override
    protected double filterValue(double value)
    {
        return 0;
    }

    @Override
    /*
     *  <QualityControl>
            <Level>Highest (QA flag = 0)</Level>
            <Level>Moderate (QA flag = 0 or 1)</Level>
            <Level>Low (QA flag = 0,1,or 2)</Level>
            <Level>NoScreening (QA flag = 0,1,2,or 3)</Level>
        </QualityControl>

        Data bands: {1,2,3,4,5,6,7}
        QC bands: {1,2,3,4,5,6,7}

        each is a separate .tif file after mozaicking, such as band8.tif
     */
    protected void filterByQCFlag(String qcLevel) throws Exception
    {
        GdalUtils.register();

        synchronized (GdalUtils.lockObject)
        {
            Dataset dataDS = null;
            Dataset qcDS = null;

            String dPath = Paths.get(inputFiles[0].getAbsolutePath()).getParent().toString();
            String qcPath = Paths.get(qcFiles[0].getAbsolutePath()).getParent().toString();

            for (int i = 0;  i < 7; i++)
            {
                dataDS = gdal.Open(String.format
                        ("%s" + File.separator + "%s", dPath, "Band"+dataBands[i]+".tif"));
                qcDS = gdal.Open(String.format
                        ("%s" + File.separator + "%s", qcPath, "Band"+qcBands[i]+".tif"));

                Band dataBand = dataDS.GetRasterBand(1);
                Band qcBand = qcDS.GetRasterBand(1);

                double [] gTrans = dataDS.GetGeoTransform();
                String proj = dataDS.GetProjection();
                Hashtable<?, ?> mData = dataDS.GetMetadata_Dict();

                int xSize = dataBand.getXSize();
                int ySize = dataBand.getYSize();

                int [] data = new int[xSize * ySize];
                dataBand.ReadRaster(0, 0, xSize, ySize, data);

                int [] qc = new int[xSize * ySize];
                qcBand.ReadRaster(0, 0, xSize, ySize, qc);

                dataDS.delete();
                qcDS.delete();

                // change the names to Band1.tif, Band2.tif, and such for the indix calc
                Dataset outputDS =
                        gdal.GetDriverByName("GTiff").Create
                        (outputFolder + File.separator + "Band"+(i+1)+".tif",
                                xSize, ySize, 1, gdalconstConstants.GDT_Int32);

                outputDS.SetGeoTransform(gTrans);
                outputDS.SetProjection(proj);
                outputDS.SetMetadata(mData);

                for (int k = 0; k < xSize * ySize; k++)
                {
                    /*Highest (QA flag = 0)
                      Moderate (QA flag = 0 or 1)
                      Low (QA flag = 0,1,or 2)
                      NoScreening (QA flag = 0,1,2,or 3)
                     */

                    switch (qcLevel)
                    {
                    case "NoScreening (QA flag = 0,1,2,or 3)":
                        if (qc[k] > 3) {
                            data[k] = noDataValue;
                        }
                        break;
                    case "Low (QA flag = 0,1,or 2)":
                        if (qc[k] > 2) {
                            data[k] = noDataValue;
                        }
                        break;
                    case "Moderate (QA flag = 0 or 1)":
                        if (qc[k] > 1) {
                            data[k] = noDataValue;
                        }
                        break;
                    case "Highest (QA flag = 0)":
                        if (qc[k] > 0) {
                            data[k] = noDataValue;
                        }
                        break;
                    }
                    // < 1 or > 32766 is no data value
                    if ((data[k] < 1) || (data[k] > 32766)) {
                        data[k] = noDataValue;
                    }
                }

                outputDS.GetRasterBand(1).WriteRaster(0, 0, xSize, ySize, data);
                outputDS.GetRasterBand(1).SetNoDataValue(noDataValue);
                outputDS.GetRasterBand(1).ComputeStatistics(false);
                outputDS.delete();
            }
        }
        GdalUtils.errorCheck();
    }

}

