package version2.prototype.processor.Modis_MYD11A1_C6;

import version2.prototype.processor.ProcessData;
import version2.prototype.processor.Reproject;

public class Modis_MYD11A1_C6Reproject extends Reproject{

    public Modis_MYD11A1_C6Reproject(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        NoProj =  false;
    }

}
