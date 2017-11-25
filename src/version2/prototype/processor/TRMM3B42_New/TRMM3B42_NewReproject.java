package version2.prototype.processor.TRMM3B42_New;

import version2.prototype.processor.ProcessData;
import version2.prototype.processor.Reproject;

// For reflection
public class TRMM3B42_NewReproject extends Reproject{

    public TRMM3B42_NewReproject(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        NoProj =  false;
    }

}