package version2.prototype.processor.ModisLSTV6;

import version2.prototype.processor.ProcessData;
import version2.prototype.processor.Reproject;

public class ModisLSTV6Reproject extends Reproject{

    public ModisLSTV6Reproject(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        NoProj =  false;
    }

}
