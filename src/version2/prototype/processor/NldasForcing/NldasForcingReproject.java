package version2.prototype.processor.NldasForcing;

import version2.prototype.processor.ProcessData;
import version2.prototype.processor.Reproject;

public class NldasForcingReproject extends Reproject {

    public NldasForcingReproject(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        NoProj =  false;
    }


}
