package version2.prototype.indices.Modis_MOD11A1_C6;

import java.io.File;
import java.util.List;

import version2.prototype.indices.IndicesFramework;

public class Modis_MOD11A1_C6Night extends IndicesFramework {
    private final int INPUT;

    public Modis_MOD11A1_C6Night(List<File> inputFiles, File outputFile, Integer noDataValue)
    {
        super(inputFiles, outputFile, noDataValue);

        int tempINPUT = -1;
        for(int i=0; i < mInputFiles.length; i++)
        {
            if(mInputFiles[i].getName().toLowerCase().contains(new String("night")))
            {
                tempINPUT = i;
            }

            if(tempINPUT > -1) {
                break;
            }
        }

        INPUT = tempINPUT;
    }

    /**
     * Valid input value range: 7500 to 65535
     * Valid output value range: all values
     */
    @Override
    protected double calculatePixelValue(double[] values) {
        if (values[INPUT] < 7500 || values[INPUT] > 65535 || values[INPUT] == noDataValue) {
            //        if(values[INPUT] == GdalUtils.NoValue) {
            //            return -3.4028234663852886E38;
            return noDataValue;
        } else {
            return (values[INPUT] * 0.02) - 273.16;
        }
    }

    @Override
    protected String className() {
        return getClass().getName();
    }
}
