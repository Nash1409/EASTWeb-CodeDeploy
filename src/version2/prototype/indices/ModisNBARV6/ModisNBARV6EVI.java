package version2.prototype.indices.ModisNBARV6;

import java.io.File;
import java.util.List;

import version2.prototype.indices.IndicesFramework;

/**
 * EVI = G * (NIR - RED)/(NIR + C1*RED - C2*BLUE + L) where L=1, C1=6, C2=7.5, and G=2.5
 *
 * http://wiki.landscapetoolbox.org/doku.php/remote_sensing_methods:enhanced_vegetation_index
 *
 *@author Isaiah Snell-Feikema
 */

/*
 *  1: Band 1: Red
 *  2: Band 2: NIR
 *  3: Band 3: Blue
 *  4: Band 4: Green
 *  5: Band 5: SWIR 1
 *  6: Band 6: SWIR 2
 *  7: Band 7: SWIR 3
 */
public class ModisNBARV6EVI extends IndicesFramework
{
    private static final double L = 1;
    private static final double C1 = 6;
    private static final double C2 = 7.5;
    private static final double G = 2.5;

    private final int RED;
    private final int NIR;
    private final int BLUE;

    public ModisNBARV6EVI(List<File> inputFiles, File outputFile, Integer noDataValue)
    {
        super(inputFiles, outputFile, noDataValue);

        int tempRED = -1;
        int tempNIR = -1;
        int tempBLUE = -1;
        for(int i=0; i < mInputFiles.length; i++)
        {
            if(mInputFiles[i].getName().toLowerCase().contains(new String("band2")))
            {
                tempNIR = i;
            }
            else if(mInputFiles[i].getName().toLowerCase().contains(new String("band1")))
            {
                tempRED = i;
            }
            else if(mInputFiles[i].getName().toLowerCase().contains(new String("band3")))
            {
                tempBLUE = i;
            }

            if(tempNIR > -1 && tempBLUE > -1 && tempRED > -1) {
                break;
            }
        }

        RED = tempRED;
        NIR = tempNIR;
        BLUE = tempBLUE;
    }

    /**
     * Valid input value range: 1 to 32766
     * Possible output value range: -163800.0 to 163770.0
     * Valid output value range: valid range is suspected to be near the same as possible output range
     */
    @Override
    protected double calculatePixelValue(double[] values) throws Exception {
        if (values[NIR] > 32766 || values[NIR] < 1 || values[RED] > 32766 || values[RED] < 1 || values[BLUE] > 32766 || values[BLUE] < 1 ||
                values[NIR] == noDataValue || values[RED] == noDataValue || values[BLUE] == noDataValue) {
            //            return -3.40282346639e+038;
            return noDataValue;
        } else {
            for(int i=0; i < values.length; i++) {
                values[i] = values[i] / 10000;
            }
            double top = G * (values[NIR] - values[RED]);
            double bottom = (values[NIR] + C1 * values[RED] - C2 * values[BLUE] + L);
            return top / bottom;
        }
    }

    @Override
    protected String className() {
        return getClass().getName();
    }

}