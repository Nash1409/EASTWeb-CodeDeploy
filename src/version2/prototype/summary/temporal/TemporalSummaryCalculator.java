package version2.prototype.summary.temporal;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.FileSystem;
import version2.prototype.util.IndicesFileMetaData;
import version2.prototype.Process;

/**
 * Calculates the temporal summary for given raster files using a shared stored list of files to create composites from.
 *
 * @author michael.devos
 *
 */
public class TemporalSummaryCalculator {
    protected final Config configInstance;
    //protected final DatabaseConnection con;
    protected final Connection con;
    protected final Process process;
    protected final ProjectInfoFile projectInfo;
    protected final String pluginName;
    protected final IndicesFileMetaData inputFile;
    protected int daysPerInputData;
    protected final TemporalSummaryRasterFileStore fileStore;

    protected final InterpolateStrategy intStrategy;
    protected final MergeStrategy mergeStrategy;
    protected final ProjectInfoSummary summary;

    /**
     * Creates a TemporalSummaryCalculator. Uses a shared TemporalSummaryRasterFileStore and breaks apart and combines files depending on the values for
     * daysPerInputData and daysPerOutputData.
     *
     * @param configInstance
     * @param con
     * @param process  - the calling/creating Process instance (owner)
     * @param projectInfo
     * @param pluginName  - name of current plugin
     * @param inputFile  - IndicesFileMetaData object of input raster file
     * @param daysPerInputData  - number of days the inRasterFile contains data for
     * @param intStrategy  - interpolation strategy (method for splitting apart data files into multiple days if they are of more than 1)
     * @param mergeStrategy  - merge strategy for combining multiple files into a single one representing more days than any single file
     * @param fileStore  - common storage object to hold files waiting to be merged together into a single composite
     * @param summary ProjectInfoSummary object for current summarization
     */
    public TemporalSummaryCalculator(Config configInstance, Connection con, Process process, ProjectInfoFile projectInfo, String pluginName, IndicesFileMetaData inputFile,
            int daysPerInputData, TemporalSummaryRasterFileStore fileStore, InterpolateStrategy intStrategy, MergeStrategy mergeStrategy, ProjectInfoSummary summary) {
        this.configInstance = configInstance;
        this.con = con;
        this.process = process;
        this.projectInfo = projectInfo;
        this.pluginName = pluginName;
        this.inputFile = inputFile;
        this.daysPerInputData = daysPerInputData;
        this.fileStore = fileStore;
        this.intStrategy = intStrategy;
        this.mergeStrategy = mergeStrategy;
        this.summary = summary;
    }

    /**
     * Run temporal summary calculation.
     *
     * @return metadata of created raster file if composite created, already existing raster file if no temporal calculation needed, or null if additional files
     * required for composite to be created
     * @throws Exception
     */
    public DataFileMetaData calculate() throws Exception {
        DataFileMetaData output = null;
        ArrayList<File> inputFileSet = new ArrayList<File>();

        // Check if interpolation is needed
        TemporalSummaryCompositionStrategy temp = fileStore.compStrategy;
        if(temp.getDaysInThisComposite(LocalDate.ofYearDay(inputFile.year, inputFile.day)) % daysPerInputData == 0) {
            inputFileSet.add(new File(inputFile.dataFilePath));
        }

        /* Interpolation is not supported currently. */

        //        else {
        //            // If the given interpolation strategy is able to complete the required composites..
        //            if(fileStore.compStrategy.getDaysInThisComposite(inDataDate.getCalendar()) % intStrategy.GetResultingNumOfFiles() == 0) {
        //                inputFileSet = intStrategy.Interpolate(inRasterFile, daysPerInputData);
        //            }
        //            // Else, if it isn't then the default daily interpolation strategy.
        //            else {
        //
        //            }
        //        }

        TemporalSummaryComposition tempComp;
        for(File inRaster : inputFileSet)
        {
            tempComp = fileStore.addFile(inRaster, new DataDate(inputFile.day, inputFile.year), daysPerInputData, inputFile.indexNm, process);

            if(tempComp != null)
            {
                ArrayList<File> files = new ArrayList<File>(tempComp.files.size());
                for(FileDatePair fdPair : tempComp.files) {
                    files.add(fdPair.file);
                }
                String outputFilePath = FileSystem.GetProcessWorkerTempDirectoryPath(projectInfo.GetFullPath(), pluginName, ProcessName.SUMMARY) +
                        "Summary " + summary.GetID() + "\\" + inputFile.indexNm + "\\" +
                        String.format("%04d_%03d.tif",
                                tempComp.startDate.getYear(),
                                tempComp.startDate.getDayOfYear()
                                );
                new File(outputFilePath).mkdirs();
                output = mergeStrategy.Merge(configInstance, con, process, projectInfo, pluginName, inputFile.indexNm, tempComp.startDate, files.toArray(new File[0]), outputFilePath);
            }
        }
        return output;
    }
}
