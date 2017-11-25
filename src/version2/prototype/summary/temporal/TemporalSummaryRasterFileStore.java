package version2.prototype.summary.temporal;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import version2.prototype.DataDate;
import version2.prototype.Process;

/**
 * Storage object for temporal summary raster files. Used as shared storage between temporal summary calculations.
 *
 * @author michael.devos
 *
 */
public class TemporalSummaryRasterFileStore {
    /**
     * The TemporalSummaryCompositionStrategy object created for this summary as specified in the project metadata.
     */
    public final TemporalSummaryCompositionStrategy compStrategy;
    private Map<String, ArrayList<TemporalSummaryComposition>> compositionsMap;

    /**
     * Creates a TemporalSummaryRasterFileStore utilizing the given composition strategy.
     *
     * @param compStrategy  - temporal summary composition strategy to use when combining raster files
     */
    public TemporalSummaryRasterFileStore(TemporalSummaryCompositionStrategy compStrategy)
    {
        this.compStrategy = compStrategy;
        compositionsMap = new TreeMap<String, ArrayList<TemporalSummaryComposition>>();
    }

    /**
     * Adds a file to the storage.
     *
     * @param f  - raster file to add
     * @param d  - Datadate associated to the raster file
     * @param daysPerInputData - returns the composition made full by the new file if there is such a composition, otherwise null.
     * @param indexName  - index the file was calculated from
     * @param process  - calling Process instance
     * @return if file store can create a complete composite with the newly added file a new TemporalSummaryComposition is returned of the newly completed composite
     * while removing used files from the storage.
     * @throws Exception
     */
    public synchronized TemporalSummaryComposition addFile(File f, DataDate d, int daysPerInputData, String indexName, Process process) throws Exception
    {
        ArrayList<TemporalSummaryComposition> compositions = getCompositionList(indexName);
        TemporalSummaryComposition modifiedComp = null;
        LocalDate lDate = compStrategy.getStartDate(d.getLocalDate());
        int i;
        boolean matched = false;
        for(i=0; i < compositions.size(); i++) {
            if(compositions.get(i).startDate.compareTo(lDate) == 0)
            {
                matched = true;
                break;
            }
        }

        if(matched)
        {
            FileDatePair newFDP = new FileDatePair(f, d);

            if(!compositions.get(i).contains(newFDP)) {
                compositions.get(i).addFilePair(newFDP, process);
                if(compositions.get(i).compositeFull()) {
                    modifiedComp = compositions.remove(i);
                }
            }
        }
        else
        {
            TemporalSummaryComposition tempComp = new TemporalSummaryComposition(compStrategy, new FileDatePair(f, d));
            if(compStrategy.getDaysInThisComposite(d.getLocalDate()) == 1) {
                modifiedComp = tempComp;
            } else {
                compositions.add(tempComp);
            }
        }

        return modifiedComp;
    }

    private ArrayList<TemporalSummaryComposition> getCompositionList(String indexName)
    {
        ArrayList<TemporalSummaryComposition> compositionList = compositionsMap.get(indexName);

        if(compositionList == null)
        {
            compositionList = new ArrayList<TemporalSummaryComposition>(0);
            compositionsMap.put(indexName, compositionList);
        }

        return compositionList;
    }
}
