package version2.prototype.processor.IMERG;

import version2.prototype.processor.ProcessData;
import version2.prototype.processor.Reproject;

public class IMERGReproject extends Reproject{

    public IMERGReproject(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        NoProj =  false;
    }

}
