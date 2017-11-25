package version2.prototype.processor.ModisNBARV6;


import version2.prototype.processor.ProcessData;
import version2.prototype.processor.Reproject;

public class ModisNBARV6Reproject extends Reproject{

    public ModisNBARV6Reproject(ProcessData data, Boolean deleteInputDirectory) {
        super(data, deleteInputDirectory);
        NoProj =  false;
    }

}