package version2.prototype.processor.TRMM3B42_New;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import version2.prototype.processor.Convert;
import version2.prototype.processor.ProcessData;
import version2.prototype.util.GdalUtils;

public class TRMM3B42_NewConvert extends Convert
{
    /* private String myfile;
    private String outfile;
     */
    private Integer noDataValue;
    private final double [] gTran = new double[] {
            0.125, 0.25, 0,
            -49.8750000, 0, 0.25
    };
    private final String projInfo = "GEOGCS[\"WGS 84\",DATUM[\"WGS_1984\",SPHEROID[\"WGS 84\",6378137,298.257223563,AUTHORITY[\"EPSG\",\"7030\"]],AUTHORITY[\"EPSG\",\"6326\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4326\"]]";

    public TRMM3B42_NewConvert(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        noDataValue = data.getNoDataValue();
    }


    @Override
    protected void convertFiles() throws Exception
    {
        if (inputFiles == null)
        { System.out.println("file does not exist");}

        for (File f:inputFiles)
        {
            String fileName = FilenameUtils.getBaseName(f.getName());

            String outFile = outputFolder + File.separator + fileName +".tif";

            GdalUtils.NetCDF2Tiff(f.getAbsolutePath(), outFile, gTran, projInfo, noDataValue);
        }
    }


}


