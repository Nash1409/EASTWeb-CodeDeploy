package version2.prototype.indices.Modis_MYD11A1_C6;

import java.io.File;
import java.util.List;

import version2.prototype.indices.IndicesFramework;

public class Modis_MYD11A1_C6Mean extends IndicesFramework{
    private final int DAY_LST;
    private final int NIGHT_LST;

    public Modis_MYD11A1_C6Mean(List<File> inputFiles, File outputFile, Integer noDataValue)
    {
        super(inputFiles, outputFile, noDataValue);

        int tempDAY_LST = -1;
        int tempNIGHT_LST = -1;
        for(int i=0; i < mInputFiles.length; i++)
        {
            if(mInputFiles[i].getName().toLowerCase().contains(new String("day")))
            {
                tempDAY_LST = i;
            }
            else if(mInputFiles[i].getName().toLowerCase().contains(new String("night")))
            {
                tempNIGHT_LST = i;
            }

            if(tempDAY_LST > -1 && tempNIGHT_LST > -1) {
                break;
            }
        }

        DAY_LST = tempDAY_LST;
        NIGHT_LST = tempNIGHT_LST;
    }

    /**
     * Valid input value range: 7500 to 65535
     * Valid output value range: all values
     */
    @Override
    protected double calculatePixelValue(double[] values) {
        //        if (values[DAY_LST] == GdalUtils.NoValue || values[NIGHT_LST] == GdalUtils.NoValue) {
        if (values[DAY_LST] == noDataValue || values[NIGHT_LST] == noDataValue
                || values[DAY_LST] < 7500 || values[DAY_LST] > 65535
                || values[NIGHT_LST] < 7500 || values[NIGHT_LST] > 65535) {
            //            return -3.4028234663852886E38;
            return noDataValue;
        } else {
            double day_lst = (values[DAY_LST] * 0.02) - 273.16;
            double night_lst = (values[NIGHT_LST] * 0.02) - 273.16;
            return (day_lst + night_lst) / 2;
        }
    }

    @Override
    protected String className() {
        return getClass().getName();
    }
}
