package version2.prototype.summary.temporal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import version2.prototype.ErrorLog;
import version2.prototype.Process;


/**
 * Represents a temporal summary composition either partial or complete. Allows for file to be added to it until full.
 *
 * @author michael.devos
 *
 */
public class TemporalSummaryComposition {
    public final LocalDate startDate;
    public final ArrayList<FileDatePair> files;
    private final TemporalSummaryCompositionStrategy strategy;

    /**
     * Creates a TemporalSummaryComposition object that will use the given strategy to create composites and initialized with a single FileDatePair.
     *
     * @param strategy  - a strategy pattern to use to combine raster files
     * @param FDPair  - first FileDatePair to insert into the composite
     * @throws Exception
     */
    public TemporalSummaryComposition(TemporalSummaryCompositionStrategy strategy, FileDatePair FDPair) throws Exception
    {
        this.strategy = strategy;
        files = new ArrayList<FileDatePair>(1);
        files.add(FDPair);
        startDate = strategy.getStartDate(FDPair.date.getLocalDate());
    }

    /**
     * Adds a file to this class's composite file listing.
     *
     * @param FDPair  - FileDatePair to add to file listing
     * @param process  - the owning/calling Process instance
     */
    public void addFilePair(FileDatePair FDPair, Process process)
    {
        if((strategy.getDaysInThisComposite(startDate) > files.size()) && (FDPair.date.getLocalDate().compareTo(startDate) >= 0))
        {
            files.add(FDPair);
            Collections.sort(files, new AscComp());
        } else {
            ErrorLog.add(process, "Failed to add raster file to temporal composite. File Date=" + FDPair.date.getLocalDate() + ". Composite info: {Size=" + files.size() + ", Completion Size=" +
                    strategy.getDaysInThisComposite(startDate) + ", Start Date=" + startDate + "}",
                    new Exception("Failed to add raster file to temporal composite. File Date=" + FDPair.date.getLocalDate() + ". Composite info: {Size=" + files.size() + ", Completion Size=" +
                            strategy.getDaysInThisComposite(startDate) + ", Start Date=" + startDate + "}"));
        }
    }

    /**
     * Checks if the composite is full.
     *
     * @return returns true if the composite is full, otherwise returns false
     * @throws Exception
     */
    public boolean compositeFull() throws Exception
    {
        if(strategy.getDaysInThisComposite(startDate) == files.size()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true if this list contains the specified element.
     * @param FDPair  - element whose presence in this list is to be tested
     * @return true if this composite contains the specified element
     */
    public boolean contains(FileDatePair FDPair)
    {
        return files.contains(FDPair);
    }

    /**
     * Used to compare FileDatePairs within composite in order to sort them.
     *
     * @author michael.devos
     *
     */
    public class AscComp implements Comparator<FileDatePair>
    {
        @Override
        public int compare(FileDatePair arg0, FileDatePair arg1) {
            return arg0.date.compareTo(arg1.date);
        }
    }
}
