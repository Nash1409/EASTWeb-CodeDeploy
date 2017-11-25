package version2.prototype.processor.ModisNBARV6;

import version2.prototype.processor.Mozaic;
import version2.prototype.processor.ProcessData;

public class ModisNBARV6MozaicQC extends Mozaic{

    public ModisNBARV6MozaicQC(ProcessData data, Boolean deleteInputDirectory) throws InterruptedException {
        super(data, deleteInputDirectory);
    }

    @Override
    protected int[] getBands() {
        return new int[] {1, 2, 3, 4, 5, 6, 7};
    }

    @Override
    protected String getBandNamePattern(){

        //pattern: MOD_Grid_BRDF:Nadir_Reflectance_Band(1|2|3|4|5|6|7)/
        String bandpattern = "MOD_Grid_BRDF:BRDF_Albedo_Band_Quality_Band"
                + "(" + String.valueOf(bands[0]);
        int bandsLength = bands.length;
        for (int i = 1; i<= bandsLength-1; i++)
        {
            bandpattern += "|" + String.valueOf(bands[i]);
        }
        bandpattern += ")";

        return bandpattern;

    }
}
