/**
 *
 */
package version2.prototype.Scheduler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.TaskState;
import version2.prototype.PluginMetaData.PluginMetaDataCollection;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.download.ListDatesFiles;
import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.ProgressUpdater;

/**
 * @author michael.devos
 *
 */
public class SchedulerStatusContainer {
    /**
     * Scheduler instance assigned unique identifier.
     */
    public final int SchedulerID;
    /**
     * Project name associated with the Scheduler.
     */
    public final ProjectInfoFile projectMetaData;
    /**
     * Plugin metadata objects use.
     */
    public final PluginMetaDataCollection pluginMetaDataCollection;
    /**
     * Start date the Scheduler created with.
     */
    public final LocalDate startDate;

    private final Config configInstance;
    private ProgressUpdater progressUpdater;
    protected TreeMap<String, TreeMap<String, Double>> downloadProgressesByData;
    protected TreeMap<String, Double> processorProgresses;
    protected TreeMap<String, Double> indicesProgresses;
    protected TreeMap<String, TreeMap<Integer, Double>> summaryProgresses;
    private List<String> log;
    private TaskState state;
    private boolean projectUpToDate;
    private LocalDateTime lastModifiedTime;

    // ProcessWorker information
    private TreeMap<ProcessName,Integer> workersInQueuePerProcess;
    private TreeMap<ProcessName,Integer> activeWorkersPerProcess;
    private boolean schedulerWorking;

    /**
     * Creates a SchedulerStatusContainer
     * @param configInstance
     * @param SchedulerID
     * @param startDate
     * @param progressUpdater
     * @param projectMetaData
     * @param pluginMetaDataCollection
     * @param log
     * @param state
     * @param downloadProgressesByData
     * @param processorProgresses
     * @param indicesProgresses
     * @param summaryProgresses
     * @param projectUpToDate
     * @param lastModifiedTime
     */
    public SchedulerStatusContainer(Config configInstance, int SchedulerID, LocalDate startDate, LocalDate endDate, ProgressUpdater progressUpdater, ProjectInfoFile projectMetaData,
            PluginMetaDataCollection pluginMetaDataCollection, List<String> log, TaskState state, TreeMap<String, TreeMap<String, Double>> downloadProgressesByData,
            TreeMap<String, Double> processorProgresses, TreeMap<String, Double> indicesProgresses, TreeMap<String, TreeMap<Integer, Double>> summaryProgresses,
            boolean projectUpToDate, LocalDateTime lastModifiedTime)
    {
        this.configInstance = configInstance;
        this.SchedulerID = SchedulerID;
        this.startDate = startDate;
        this.progressUpdater = progressUpdater;
        this.projectMetaData = projectMetaData;
        this.pluginMetaDataCollection = pluginMetaDataCollection;
        this.downloadProgressesByData = downloadProgressesByData;
        this.processorProgresses = processorProgresses;
        this.indicesProgresses = indicesProgresses;
        this.summaryProgresses = summaryProgresses;
        this.log = log;
        this.state = state;
        this.projectUpToDate = projectUpToDate;
        this.lastModifiedTime = lastModifiedTime;

        workersInQueuePerProcess = new TreeMap<ProcessName,Integer>();
        workersInQueuePerProcess.put(ProcessName.DOWNLOAD, 0);
        workersInQueuePerProcess.put(ProcessName.PROCESSOR, 0);
        workersInQueuePerProcess.put(ProcessName.INDICES, 0);
        workersInQueuePerProcess.put(ProcessName.SUMMARY, 0);

        activeWorkersPerProcess = new TreeMap<ProcessName,Integer>();
        activeWorkersPerProcess.put(ProcessName.DOWNLOAD, 0);
        activeWorkersPerProcess.put(ProcessName.PROCESSOR, 0);
        activeWorkersPerProcess.put(ProcessName.INDICES, 0);
        activeWorkersPerProcess.put(ProcessName.SUMMARY, 0);

        schedulerWorking = false;
    }

    /**
     * Creates a default SchedulerStatusContainer with everything set to either the current time, 0, false, or the associated value given.
     * @param configInstance
     * @param SchedulerID
     * @param startDate
     * @param progressUpdater
     * @param projectMetaData
     * @param pluginMetaDataCollection
     * @param state
     */
    public SchedulerStatusContainer(Config configInstance, int SchedulerID, LocalDate startDate, LocalDate endDate, ProgressUpdater progressUpdater, ProjectInfoFile projectMetaData,
            PluginMetaDataCollection pluginMetaDataCollection, TaskState state)
    {
        this.configInstance = configInstance;
        this.SchedulerID = SchedulerID;
        this.startDate = startDate;
        this.progressUpdater = progressUpdater;
        this.projectMetaData = projectMetaData;
        this.pluginMetaDataCollection = pluginMetaDataCollection;
        ArrayList<ProjectInfoSummary> summariesTemp = new ArrayList<ProjectInfoSummary>();
        for(ProjectInfoSummary pfs : projectMetaData.GetSummaries()) {
            summariesTemp.add(new ProjectInfoSummary(pfs.GetZonalSummary(), pfs.GetTemporalSummaryCompositionStrategyClassName(), pfs.GetID()));
        }
        this.state = state;
        log = Collections.synchronizedList(new ArrayList<String>(1));
        projectUpToDate = false;


        downloadProgressesByData = new TreeMap<String, TreeMap<String, Double>>();
        TreeMap<String, Double> downloadDataProgressInit;
        processorProgresses = new TreeMap<String, Double>();
        indicesProgresses = new TreeMap<String, Double>();
        summaryProgresses = new TreeMap<String, TreeMap<Integer, Double>>();
        TreeMap<Integer, Double> summaryProgressesPluginResults;
        TreeMap<Integer, Boolean> resultsUpToDatePluginResults;
        String pluginName;
        for(ProjectInfoPlugin plugin : projectMetaData.GetPlugins())
        {
            pluginName = plugin.GetName();

            // Setup Download progresses
            downloadDataProgressInit = new TreeMap<String, Double>();
            downloadDataProgressInit.put("data", 0.0);
            for(String dataName : pluginMetaDataCollection.pluginMetaDataMap.get(pluginName).ExtraDownloadFiles)
            {
                dataName = dataName.toLowerCase();
                downloadDataProgressInit.put(dataName, 0.0);
            }
            downloadProgressesByData.put(pluginName, downloadDataProgressInit);

            // Setup Processor progresses
            processorProgresses.put(pluginName, 0.0);

            // Setup Indices progresses
            indicesProgresses.put(pluginName, 0.0);

            // Setup Summary progresses
            summaryProgressesPluginResults = new TreeMap<Integer, Double>();
            resultsUpToDatePluginResults = new TreeMap<Integer, Boolean>();
            for(ProjectInfoSummary summary : projectMetaData.GetSummaries())
            {
                summaryProgressesPluginResults.put(summary.GetID(), 0.0);
                resultsUpToDatePluginResults.put(summary.GetID(), false);
            }
            summaryProgresses.put(pluginName, summaryProgressesPluginResults);
        }

        updateLastModifiedTime();

        workersInQueuePerProcess = new TreeMap<ProcessName,Integer>();
        workersInQueuePerProcess.put(ProcessName.DOWNLOAD, 0);
        workersInQueuePerProcess.put(ProcessName.PROCESSOR, 0);
        workersInQueuePerProcess.put(ProcessName.INDICES, 0);
        workersInQueuePerProcess.put(ProcessName.SUMMARY, 0);

        activeWorkersPerProcess = new TreeMap<ProcessName,Integer>();
        activeWorkersPerProcess.put(ProcessName.DOWNLOAD, 0);
        activeWorkersPerProcess.put(ProcessName.PROCESSOR, 0);
        activeWorkersPerProcess.put(ProcessName.INDICES, 0);
        activeWorkersPerProcess.put(ProcessName.SUMMARY, 0);

        schedulerWorking = false;
    }

    /**
     * Get last modified time of this container.
     * @return LocalDateTime object representing the last time something in this container was modified
     */
    public LocalDateTime GetLastModifiedTime() { return lastModifiedTime; }

    /**
     * Gets the stored state of the Scheduler.
     * @return current TaskState of Scheduler
     */
    public TaskState GetState() { return state; }

    /**
     * Updates the download progress for the given plugin and data name.
     * @param dataName  - the name of the file type being downloaded (e.g. "Data" or "Qc")
     * @param pluginName  - the plugin title gotten from the plugin metadata to calculate progress in relation to
     * @param listDatesFiles  - reference to the ListDatesFiles object to use
     * @param modisTileNames  - list of modis tiles included
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateDownloadProgressByData(String dataName, String pluginName, ListDatesFiles listDatesFiles, ArrayList<String> modisTileNames, Statement stmt) throws SQLException
    {
        progressUpdater.UpdateDBDownloadExpectedCount(pluginName, dataName, listDatesFiles, modisTileNames, stmt);
        updateLastModifiedTime();
    }

    /**
     * Updates the processor progress for the given plugin.
     * @param pluginName  - name of plugin whose processor progress to change
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateProcessorProgress(String pluginName, Statement stmt) throws SQLException
    {
        progressUpdater.UpdateDBProcessorExpectedCount(pluginName, stmt);
        updateLastModifiedTime();
    }

    /**
     * Updates the indices progress for the given plugin.
     * @param pluginName  - name of plugin whose processor progress to change
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateIndicesProgress(String pluginName, Statement stmt) throws SQLException
    {
        progressUpdater.UpdateDBIndicesExpectedCount(pluginName, stmt);
        updateLastModifiedTime();
    }

    /**
     * Updates the summary progress for the summary attributed to the given ID for the named plugin.
     * @param summaryIDNum  - ID attribute value to calculate progress for gotten from project metadata
     * @param compStrategy  - TemporalSummaryCompositionStrategy object to use in calculating total expecting results in temporal summary cases
     * @param daysPerInputData  - number of days each input file represents
     * @param pluginInfo  - reference to a ProjectInfoPlugin object to use
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateSummaryProgress(int summaryIDNum, TemporalSummaryCompositionStrategy compStrategy, int daysPerInputData, ProjectInfoPlugin pluginInfo, Statement stmt)
            throws SQLException
    {
        progressUpdater.UpdateDBSummaryExpectedCount(summaryIDNum, compStrategy, daysPerInputData, pluginInfo, stmt);
        updateLastModifiedTime();
    }

    /**
     * Updates the stored state of the Scheduler.
     * @param state  - TaskState to assign to the SchedulerStatueContainer
     */
    public void UpdateSchedulerTaskState(TaskState state)
    {
        this.state = state;
        updateLastModifiedTime();
    }

    /**
     * Adds a string to the log.
     * @param logText  - the string to add to the log
     */
    public void AddToLog(String logText)
    {
        log.add(logText);
        updateLastModifiedTime();
    }

    /**
     * Registers the addition of a ProcessWorker in the count of queued ProcessWorkers for the named Process.
     * @param name
     */
    public void AddWorker(ProcessName name)
    {
        workersInQueuePerProcess.put(name, workersInQueuePerProcess.get(name) + 1);
        updateLastModifiedTime();
    }

    /**
     * Registers the removal of a ProcessWorker in the count of queued ProcessWorkers for the named Process.
     * @param name
     */
    public void SubtractWorker(ProcessName name)
    {
        workersInQueuePerProcess.put(name, workersInQueuePerProcess.get(name) - 1);
        updateLastModifiedTime();
    }

    /**
     * Registers the execution of a queued ProcessWorker in the count of active ProcessWorkers for the named Process.
     * @param name
     */
    public void AddActiveWorker(ProcessName name)
    {
        activeWorkersPerProcess.put(name, activeWorkersPerProcess.get(name) + 1);
        updateLastModifiedTime();
    }

    /**
     * Registers the completion of an executed queued ProcessWorker in the count of active ProcessWorkers for the named Process.
     * @param name
     */
    public void SubtractActiveWorker(ProcessName name)
    {
        activeWorkersPerProcess.put(name, activeWorkersPerProcess.get(name) - 1);
        updateLastModifiedTime();
    }

    /**
     * Gets the current state of this SchedulerStatusContainer as a SchedulerStatus object which erases the log as it instantiates the returned SchedulerStatus object.
     * @return SchedulerStatus representation of this object
     * @throws SQLException
     */
    public SchedulerStatus GetStatus() throws SQLException
    {
        // Update progresses
        String pluginName;
        double progress;
        DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        if(con != null)
        {
            Statement stmt = con.createStatement();
            for(ProjectInfoPlugin pluginInfo : projectMetaData.GetPlugins())
            {
                pluginName = pluginInfo.GetName();
                PluginMetaData pluginMetaData = pluginMetaDataCollection.pluginMetaDataMap.get(pluginName);

                // Setup Download progresses
                progress = progressUpdater.GetCurrentDownloadProgress("data", pluginName, startDate, pluginInfo.GetModisTiles(), stmt);
                downloadProgressesByData.get(pluginName).put("data", progress);
                for(String dataName : pluginMetaDataCollection.pluginMetaDataMap.get(pluginName).ExtraDownloadFiles)
                {
                    dataName = dataName.toLowerCase();
                    progress = progressUpdater.GetCurrentDownloadProgress(dataName, pluginName, startDate, pluginInfo.GetModisTiles(), stmt);
                    downloadProgressesByData.get(pluginName).put(dataName, progress);
                }

                // Setup Processor progresses
                progress = progressUpdater.GetCurrentProcessorProgress(pluginName, stmt);
                processorProgresses.put(pluginName, progress);

                // Setup Indices progresses
                progress = progressUpdater.GetCurrentIndicesProgress(pluginName, stmt);
                indicesProgresses.put(pluginName, progress);

                // Setup Summary progresses
                for(ProjectInfoSummary summary : projectMetaData.GetSummaries())
                {
                    if(summary.GetTemporalSummaryCompositionStrategyClassName() != null && !summary.GetTemporalSummaryCompositionStrategyClassName().isEmpty()) {
                        try {
                            Class<?> strategyClass = Class.forName("version2.prototype.summary.temporal.CompositionStrategies." + summary.GetTemporalSummaryCompositionStrategyClassName());
                            Constructor<?> ctorStrategy = strategyClass.getConstructor();
                            progress = progressUpdater.GetCurrentSummaryProgress(summary.GetID(), (TemporalSummaryCompositionStrategy)ctorStrategy.newInstance(),
                                    pluginMetaData.DaysPerInputData, pluginInfo, stmt);
                        } catch(ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            ErrorLog.add(configInstance, "Problem creating Temporal Summary Composition Strategy specified.", e);
                        }
                    } else {
                        progress = progressUpdater.GetCurrentSummaryProgress(summary.GetID(), null, pluginMetaData.DaysPerInputData, pluginInfo, stmt);
                    }
                    summaryProgresses.get(pluginName).put(summary.GetID(), progress);
                }
            }
            stmt.close();
            con.close();
        }

        // Clone progresses if necessary
        TreeMap<String, TreeMap<String, Double>> downloadProgressesByDataTemp;
        //        synchronized(downloadProgressesByData){
        downloadProgressesByDataTemp = cloneTreeMapStringStringDouble(downloadProgressesByData);
        //        }

        TreeMap<String, TreeMap<Integer, Double>> summaryProgressesTemp;
        //        synchronized(summaryProgresses){
        summaryProgressesTemp = cloneTreeMapStringIntegerDouble(summaryProgresses);
        //        }

        ArrayList<String> newLog;
        //        synchronized(log){
        newLog = new ArrayList<String>(log);
        //        }

        UpdateProjectIsUpToDate();

        UpdateSchedulerWorking();

        return new SchedulerStatus(SchedulerID, projectMetaData, downloadProgressesByDataTemp, processorProgresses, indicesProgresses, summaryProgressesTemp, newLog, state, projectUpToDate,
                workersInQueuePerProcess, activeWorkersPerProcess, lastModifiedTime, schedulerWorking, LocalDateTime.now());
    }

    private void UpdateSchedulerWorking()
    {
        boolean hasActive = false;

        Iterator<ProcessName> it = activeWorkersPerProcess.keySet().iterator();
        while(it.hasNext()) {
            if(activeWorkersPerProcess.get(it.next()) > 0) {
                hasActive = true;
            }
        }
        schedulerWorking = hasActive;
    }

    /**
     * Checks if the whole project is up to date or if any summary for any of the plugins being processed still has work to accomplish with the current information in this container.
     */
    private void UpdateProjectIsUpToDate()
    {
        boolean isUpToDate = true;
        Iterator<String> pluginsIt = downloadProgressesByData.keySet().iterator();
        String pluginName;
        while(isUpToDate && pluginsIt.hasNext())
        {
            pluginName = pluginsIt.next();

            // Check download progresses
            for(Double value : downloadProgressesByData.get(pluginName).values())
            {
                if(value.intValue() != 100)
                {
                    isUpToDate = false;
                }
            }

            // Check Summary progresses
            for(Double value : summaryProgresses.get(pluginName).values())
            {
                if(value.intValue() != 100)
                {
                    isUpToDate = false;
                }
            }
        }

        // Check Processor progresses
        for(Double value : processorProgresses.values())
        {
            if(value.intValue() != 100){
                isUpToDate = false;
            }
        }

        // Check Indices progresses
        for(Double value : indicesProgresses.values())
        {
            if(value.intValue() != 100){
                isUpToDate = false;
            }
        }

        projectUpToDate = isUpToDate;
        updateLastModifiedTime();
    }

    private TreeMap<String, TreeMap<Integer, Double>> cloneTreeMapStringIntegerDouble(TreeMap<String, TreeMap<Integer, Double>> input)
    {
        TreeMap<String, TreeMap<Integer, Double>> clone = new TreeMap<String, TreeMap<Integer, Double>>();
        Iterator<String> pluginsIt = input.keySet().iterator();
        Iterator<Integer> summaryProgressesIt;
        TreeMap<Integer, Double> pluginResults;
        String plugin;
        Integer summaryID;
        while(pluginsIt.hasNext())
        {
            plugin = pluginsIt.next();
            summaryProgressesIt = input.get(plugin).keySet().iterator();
            pluginResults = new TreeMap<Integer, Double>();
            while(summaryProgressesIt.hasNext())
            {
                summaryID = new Integer(summaryProgressesIt.next());
                pluginResults.put(summaryID, input.get(plugin).get(new Integer(summaryID)));
            }
            clone.put(new String(plugin), pluginResults);
        }
        return clone;
    }

    private TreeMap<String, TreeMap<String, Double>> cloneTreeMapStringStringDouble(TreeMap<String, TreeMap<String, Double>> input)
    {
        TreeMap<String, TreeMap<String, Double>> clone = new TreeMap<String, TreeMap<String, Double>>();
        Iterator<String> pluginsIt = input.keySet().iterator();
        Iterator<String> downloadProgressesIt;
        TreeMap<String, Double> downloadResults;
        String plugin;
        String dataName;
        while(pluginsIt.hasNext())
        {
            plugin = pluginsIt.next();
            downloadProgressesIt = input.get(plugin).keySet().iterator();
            downloadResults = new TreeMap<String, Double>();
            while(downloadProgressesIt.hasNext())
            {
                dataName = new String(downloadProgressesIt.next());
                downloadResults.put(dataName, new Double(input.get(plugin).get(dataName)));
            }
            clone.put(new String(plugin), downloadResults);
        }
        return clone;
    }

    private void updateLastModifiedTime() {
        lastModifiedTime = LocalDateTime.now();
    }
}
