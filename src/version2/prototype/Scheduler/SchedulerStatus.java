package version2.prototype.Scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import jdk.nashorn.internal.ir.annotations.Immutable;
import version2.prototype.TaskState;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;

/**
 * Records the status of a Scheduler instance at a single point in time. Object is immutable.
 *
 * @author michael.devos
 *
 */
@Immutable public class SchedulerStatus {
    /**
     * Scheduler instance assigned unique identifier.
     */
    public final int SchedulerID;
    /**
     * Project name associated with the Scheduler.
     */
    public final String ProjectName;
    /**
     * ProjectInfoPlugin list originally parsed from project info metadata file the Scheduler was instantiated to run with.
     */
    public final ArrayList<ProjectInfoPlugin> PluginInfo;
    /**
     * ProjectInfoSummary list originally parsed from project info metadata file the Scheduler was instantiated to run with. Each have an ID attributed to them within the project metadata file.
     */
    public final ArrayList<ProjectInfoSummary> Summaries;
    /**
     * The running TaskState of the Scheduler.
     */
    public final TaskState State;
    /**
     * True if number of results published equals number of expected results calculated from project metadata and plugin metadata. False, otherwise.
     */
    public final boolean ProjectUpToDate;
    /**
     * A timestamp depicting the last time this SchedulerStatus object was updated by its Scheduler.
     */
    public final LocalDateTime LastModifiedTime;
    /**
     * True if there are any active workers at time of status retrieval.
     */
    public final boolean schedulerWorking;
    /**
     * A timestamp depicting the time this SchedulerStatus object was created/retrieved.
     */
    public final LocalDateTime RetrievedTime;

    private final TreeMap<String, TreeMap<String, Double>> downloadProgressesByData;
    private final TreeMap<String, Double> processorProgresses;
    private final TreeMap<String, Double> indicesProgresses;
    private final TreeMap<String, TreeMap<Integer, Double>> summaryProgresses;

    // ProcessWorker information
    private final TreeMap<ProcessName,Integer> workersInQueuePerProcess;
    private final TreeMap<ProcessName,Integer> activeWorkersPerProcess;

    // Status Log
    private final List<String> log;
    private int logReaderPos;

    /**
     *
     * @param SchedulerID
     * @param projectMetaData
     * @param downloadProgressesByData
     * @param processorProgresses
     * @param indicesProgresses
     * @param summaryProgresses
     * @param log
     * @param State
     * @param ProjectUpToDate
     * @param workersInQueuePerProcess
     * @param activeWorkersPerProcess
     * @param LastModifiedTime
     * @param schedulerWorking
     * @param RetrievedTime
     */
    public SchedulerStatus(int SchedulerID, ProjectInfoFile projectMetaData, TreeMap<String, TreeMap<String, Double>> downloadProgressesByData, TreeMap<String, Double> processorProgresses,
            TreeMap<String, Double> indicesProgresses, TreeMap<String, TreeMap<Integer, Double>> summaryProgresses, List<String> log, TaskState State, boolean ProjectUpToDate,
            TreeMap<ProcessName,Integer>  workersInQueuePerProcess, TreeMap<ProcessName,Integer> activeWorkersPerProcess, LocalDateTime LastModifiedTime, boolean schedulerWorking,
            LocalDateTime RetrievedTime)
    {
        this.SchedulerID = SchedulerID;
        ProjectName = projectMetaData.GetProjectName();
        PluginInfo = projectMetaData.GetPlugins();
        Summaries = projectMetaData.GetSummaries();
        this.downloadProgressesByData = cloneTreeMapStringStringDouble(downloadProgressesByData);
        this.processorProgresses = cloneTreeMapStringDouble(processorProgresses);
        this.indicesProgresses = cloneTreeMapStringDouble(indicesProgresses);
        this.summaryProgresses = cloneTreeMapStringIntegerDouble(summaryProgresses);
        this.log = new ArrayList<String>(log);
        logReaderPos = 0;
        this.State = State;
        this.ProjectUpToDate = ProjectUpToDate;
        this.workersInQueuePerProcess = cloneTreeMapProcessNameInteger(workersInQueuePerProcess);
        this.activeWorkersPerProcess = cloneTreeMapProcessNameInteger(activeWorkersPerProcess);
        this.schedulerWorking = schedulerWorking;

        this.LastModifiedTime = LastModifiedTime;
        this.RetrievedTime = RetrievedTime;
    }

    /**
     * Copy constructor.
     * @param statusToCopy
     */
    public SchedulerStatus(SchedulerStatus statusToCopy)
    {
        SchedulerID = new Integer(statusToCopy.SchedulerID);
        ProjectName = new String(statusToCopy.ProjectName);
        PluginInfo = new ArrayList<ProjectInfoPlugin>(statusToCopy.PluginInfo);
        Summaries = new ArrayList<ProjectInfoSummary>(statusToCopy.Summaries);
        downloadProgressesByData = cloneTreeMapStringStringDouble(statusToCopy.downloadProgressesByData);
        processorProgresses = cloneTreeMapStringDouble(statusToCopy.processorProgresses);
        indicesProgresses = cloneTreeMapStringDouble(statusToCopy.indicesProgresses);
        summaryProgresses = cloneTreeMapStringIntegerDouble(statusToCopy.summaryProgresses);
        log = new ArrayList<String>(statusToCopy.log);
        logReaderPos = 0;
        State = statusToCopy.State;
        ProjectUpToDate = statusToCopy.ProjectUpToDate;
        workersInQueuePerProcess = cloneTreeMapProcessNameInteger(statusToCopy.GetWorkersInQueuePerProcess());
        activeWorkersPerProcess = cloneTreeMapProcessNameInteger(statusToCopy.GetActiveWorkersPerProcess());
        schedulerWorking = statusToCopy.schedulerWorking;

        LastModifiedTime = statusToCopy.LastModifiedTime;
        RetrievedTime = statusToCopy.RetrievedTime;
    }

    /**
     * Gets status of new log entries list.
     * @return boolean - TRUE if new log entries are within this status, FALSE otherwise is list is null or empty
     */
    public boolean HasLogEntries() {
        if(log != null && log.size() > 0){
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reads the next log entry. Incrementing position counter.
     * @return String - next log entry if there is one, otherwise null if empty or all log entries read.
     */
    public String ReadNextLogEntry()
    {
        if(logReaderPos < log.size()) {
            return log.get(logReaderPos++);
        } else {
            return null;
        }
    }

    /**
     * Returns a String list of log entries either as a subset of the total list of the whole list depending on the current reader position in the list. After calling the reader will return null until
     * it is reset.
     * @return list of log entries strings from the current reader position. If reader hasn't been used or has been reset immediately prior to this call then this list will contain the entire list of new
     * log entries.
     */
    public List<String> ReadAllRemainingLogEntries()
    {
        if(logReaderPos < log.size())
        {
            ArrayList<String> logEntries = new ArrayList<String>();
            for(;logReaderPos < log.size(); logReaderPos++)
            {
                logEntries.add(log.get(logReaderPos));
            }
            return logEntries;
        } else {
            return new ArrayList<String>(0);
        }
    }

    /**
     * Resets the log reader to start position.
     */
    public void ResetLogReader()
    {
        logReaderPos = 0;
    }

    /**
     * Gets a map of plugin names to their named data to their download progress at the time specified by lastModifiedTime.
     * @return map of plugin names to their named data to their download progress at the time specified by lastModifiedTime
     */
    public TreeMap<String, TreeMap<String, Double>> GetDownloadProgressesByData() {
        return downloadProgressesByData;
        //return cloneTreeMapStringStringDouble(downloadProgressesByData,true);
    }
    /**
     * Gets a map of plugin names to their indices progress at the time specified by lastModifiedTime.
     * @return map of plugin names to their indices progress at the time specified by lastModifiedTime
     */
    public TreeMap<String, Double> GetProcessorProgresses() {
        return processorProgresses;
        //return cloneTreeMapStringDouble(processorProgresses,true);
    }

    /**
     * Gets a map of plugin names to their indices progress at the time specified by lastModifiedTime.
     * @return map of plugin names to their indices progress at the time specified by lastModifiedTime
     */
    public TreeMap<String, Double> GetIndicesProgresses() {
        return indicesProgresses;
        //return cloneTreeMapStringDouble(indicesProgresses,true);
    }

    /**
     * Gets a map of plugin names to their maps of summary IDs to their summary progress at the time specified by lastModifiedTime.
     * @return map of plugin names to their maps of summary IDs to their summary progress at the time specified by lastModifiedTime
     */
    public TreeMap<String, TreeMap<Integer, Double>> GetSummaryProgresses() {
        return cloneTreeMapStringIntegerDouble(summaryProgresses);
    }

    public TreeMap<ProcessName, Integer> GetWorkersInQueuePerProcess() {
        return cloneTreeMapProcessNameInteger(workersInQueuePerProcess);
    }

    public TreeMap<ProcessName, Integer> GetActiveWorkersPerProcess() {
        return cloneTreeMapProcessNameInteger(activeWorkersPerProcess);
    }

    /**
     * Overwriting of the original cloneTreeMapStringStringDouble
     * @param input
     * @return
     * @author roberto.villegas
     */
    private TreeMap<String, TreeMap<Integer, Double>> cloneTreeMapStringIntegerDouble(TreeMap<String, TreeMap<Integer, Double>> input){
        TreeMap<String, TreeMap<Integer, Double>> newTreeMap = new TreeMap<String, TreeMap<Integer, Double>>();
        newTreeMap.putAll(input);
        return newTreeMap;
    }

    /**
     * Overwriting of the original cloneTreeMapStringStringDouble
     * @param input
     * @return
     * @author roberto.villegas
     */
    private TreeMap<String, TreeMap<String,Double>> cloneTreeMapStringStringDouble(TreeMap<String,TreeMap<String,Double>> input){
        TreeMap <String, TreeMap<String, Double>> newTreeMap = new TreeMap<String, TreeMap<String, Double>>();
        newTreeMap.putAll(input);
        return newTreeMap;
    }

    /**
     * Overwriting of the original cloneTreeMapStringDouble
     * @param input
     * @return
     * @author roberto.villegas
     */
    private TreeMap<String, Double> cloneTreeMapStringDouble(TreeMap<String, Double> input){
        TreeMap<String, Double> newTreeMap = new TreeMap<String, Double>();
        newTreeMap.putAll(input);
        return newTreeMap;
    }

    /**
     * Overwriting of the original cloneTreeMapStringStringDouble
     * @param input
     * @return
     * @author roberto.villegas
     */
    private TreeMap<ProcessName, Integer> cloneTreeMapProcessNameInteger(TreeMap<ProcessName, Integer> input){
        TreeMap<ProcessName, Integer> newTreeMap = new TreeMap<ProcessName, Integer>();
        newTreeMap.putAll(input);
        return newTreeMap;
    }
}
