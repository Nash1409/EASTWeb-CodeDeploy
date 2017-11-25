package version2.prototype.processor;


import java.time.LocalDate;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;



import version2.prototype.util.GdalUtils;

public class CompareTiffFiles
{
    public static void main(String args[])
    {
        GdalUtils.register();

        synchronized (GdalUtils.lockObject)
        {

            System.out.println (LocalDate.of(2014, 3, 18).getDayOfYear());
            String baseFile = "D:\\test_data\\trmmout.tif";
            String file2 = "D:\\testProjects\\TW_trmmRT\\Projects\\Project_TW\\TRMM3B42RT\\Processor\\Temp\\reproject\\2014\\077\\3B42RT_daily.2014.03.18.tif";

            //            String baseFile = "C:\\Users\\yi.liu\\Desktop\\tw_618\\reprojected\\modis-lst\\2014\\081\\LST_Day_1km.tif";
            //            String file2 = "D:\\testProjects\\TW2\\Projects\\Project_TW2\\ModisLST\\Processor\\Temp\\Reproject\\2014\\081\\day.tif";

            Dataset baseDS= gdal.Open(baseFile);
            Dataset DS2 = gdal.Open(file2);

            Band baseBand = baseDS.GetRasterBand(1);
            Band band2 = DS2.GetRasterBand(1);

            int xSize = baseBand.GetXSize();
            int ySize = baseBand.GetYSize();

            double[] baseArr = new double[xSize * ySize];
            baseBand.ReadRaster(0, 0, xSize, ySize, baseArr);

            double[] arr2 = new double[xSize * ySize];
            band2.ReadRaster(0, 0, xSize, ySize, arr2);

            for (int i = 0; i <= 2000; i++)
            {
                System.out.println(baseArr[i] + "   " + arr2[i]);
            }

            baseDS.delete();
            DS2.delete();

        }
    }


}

