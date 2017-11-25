package version2.prototype.processor;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;

import version2.prototype.util.GdalUtils;

public class ReadTifFile
{
    public static void main(String args[])
    {
        GdalUtils.register();

        synchronized (GdalUtils.lockObject)
        {
            String tifFile = "c:\\test\\TRMM\\output.tif";
            Dataset baseDS= gdal.Open(tifFile);

            Band baseBand = baseDS.GetRasterBand(1);

            int xSize = baseBand.GetXSize();
            int ySize = baseBand.GetYSize();

            double[] baseArr = new double[xSize * ySize];
            baseBand.ReadRaster(0, 0, xSize, ySize, baseArr);

            int count = 0;
            int count_nodata = 0;

            for (int i = 0; i < 50000; i++)
            {
                if ((baseArr[i] !=0 ) && (baseArr[i] != -9999) )
                {//System.out.println(baseArr[i]);
                    count++;
                }

                if (baseArr[i] < 0 )
                {System.out.println(baseArr[i]);
                count_nodata++;
                }
            }
            System.out.println("count : " + count);
            System.out.println("count no data : " + count_nodata);


            baseDS.delete();

        }
    }
}
