package version2.prototype.processor.IMERG_RT;

import version2.prototype.processor.ProcessData;
import version2.prototype.processor.Reproject;

public class IMERG_RTReproject extends Reproject{

    public IMERG_RTReproject(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        NoProj =  false;
    }

}
