package version2.prototype.processor.ModisLSTV6;

import version2.prototype.processor.Mozaic;
import version2.prototype.processor.ProcessData;

public class ModisLSTV6Mozaic extends Mozaic{

    public ModisLSTV6Mozaic(ProcessData data, Boolean deleteInputDirectory) throws InterruptedException {
        super(data, deleteInputDirectory);
    }

    @Override
    protected int[] getBands() {
        return new int[] {1,2, 5, 6};
    }

    @Override
    protected String getBandNamePattern(){
        //pattern: SUBDATASET_(1|2|5|6)_NAME

        int bandsLength = bands.length;

        String bandpattern = "SUBDATASET_" + "(" + String.valueOf(bands[0]);
        for (int i = 1; i<= bandsLength-1; i++)
        {
            bandpattern += "|" + String.valueOf(bands[i]);
        }
        bandpattern += ")_NAME";

        return bandpattern;
    }
}
