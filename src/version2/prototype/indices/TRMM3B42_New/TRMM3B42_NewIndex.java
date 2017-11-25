package version2.prototype.indices.TRMM3B42_New;

import java.io.File;
import java.util.List;

import version2.prototype.indices.IndicesFramework;

public class TRMM3B42_NewIndex extends IndicesFramework
{
    private final int INPUT = 0;

    public TRMM3B42_NewIndex(List<File> inputFiles, File outputFile, Integer noDataValue)
    {
        super(inputFiles, outputFile, noDataValue);
    }

    @Override
    public void calculate() throws Exception
    { super.calculate(); }

    @Override
    protected double calculatePixelValue(double[] values) throws Exception {
        if (values[INPUT] == 32767 || values[INPUT] == noDataValue) {
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
