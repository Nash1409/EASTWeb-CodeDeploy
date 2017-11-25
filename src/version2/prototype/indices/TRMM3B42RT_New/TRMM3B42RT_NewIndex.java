package version2.prototype.indices.TRMM3B42RT_New;

import java.io.File;
import java.util.List;

import version2.prototype.indices.IndicesFramework;

public class TRMM3B42RT_NewIndex extends IndicesFramework
{
    private final int INPUT = 0;

    public TRMM3B42RT_NewIndex(List<File> inputFiles, File outputFile, Integer noDataValue)
    {
        super(inputFiles, outputFile, noDataValue);
    }

    @Override
    protected double calculatePixelValue(double[] values) throws Exception {
        if (values[INPUT] == noDataValue) {
            //   System.out.println("novalue : " + values[INPUT]);
            //            return -3.4028234663852886E38;
            return noDataValue;
        } else {
            return values[INPUT];
        }
    }

    @Override
    protected String className() {
        return getClass().getName();
    }

}
