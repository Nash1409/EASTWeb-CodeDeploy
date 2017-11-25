package version2.prototype.Scheduler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Future;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.EASTWebManagerI;
import version2.prototype.ErrorLog;
import version2.prototype.GenericProcess;
import version2.prototype.Process;
import version2.prototype.ProcessWorker;
import version2.prototype.ProcessWorkerReturn;
import version2.prototype.TaskState;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.PluginMetaData.PluginMetaDataCollection;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.download.DownloadFactory;
import version2.prototype.download.ListDatesFiles;
import version2.prototype.download.LocalDownloader;
import version2.prototype.indices.IndicesWorker;
import version2.prototype.processor.ProcessorWorker;
import version2.prototype.summary.Summary;
import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;
import version2.prototype.util.DatabaseCache;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.FileSystem;
import version2.prototype.util.GeneralUIEventObject;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.ProgressUpdater;
import version2.prototype.util.Schemas;

/**
 * @author michael.devos
 *
 */
public class Scheduler {
    /**
     * SchedulerData object this Scheduler was created with.
     */
    public final SchedulerData data;
    /**
     * ProjectInfoFile object gotten from SchedulerData during Scheduler creation.
     */
    public final ProjectInfoFile projectInfoFile;
    /**
     * PluginMetaDataCollection object gotten from SchedulerData during Scheduler creation.
     */
    public final PluginMetaDataCollection pluginMetaDataCollection;

    private final int ID;
    private final Config configInstance;
    private final EASTWebManagerI manager;
    protected boolean clearIntermediateFiles;
    protected SchedulerStatusContainer statusContainer;
    protected ArrayList<LocalDownloader> localDownloaders;
    protected ArrayList<Process> processorProcesses;
    protected ArrayList<Process> indicesProcesses;
    protected ArrayList<Process> summaryProcesses;
    protected ArrayList<DatabaseCache> downloadCaches;
    protected ArrayList<DatabaseCache> processorCaches;
    protected ArrayList<DatabaseCache> indicesCaches;

    /**
     * Creates and sets up a Scheduler instance with the given project data. Does not start the Scheduler and Processes.
     * To start processing call start().
     *
     * @param data  - SchedulerData describing the project to setup for
     * @param myID  - a unique ID for this Scheduler instance
     * @param manager  - reference to the EASTWebManager creating this Scheduler
     * @param configInstance
     */
    public Scheduler(SchedulerData data, int myID, EASTWebManagerI manager, Config configInstance)
    {
        this(data, myID, TaskState.STOPPED, manager, configInstance, new ProgressUpdater(configInstance, data.projectInfoFile, data.pluginMetaDataCollection));
    }

    /**
     * Creates and sets up a Scheduler instance with the given project data. Does not start the Scheduler and Processes.
     * To start processing call start().
     *
     * @param data  - SchedulerData describing the project to setup for
     * @param myID  - a unique ID for this Scheduler instance
     * @param initState  - Initial TaskState to set this Scheduler to.
     * @param manager  - reference to the EASTWebManager creating this Scheduler
     * @param configInstance
     */
    public Scheduler(SchedulerData data, int myID, TaskState initState, EASTWebManagerI manager, Config configInstance)
    {
        this(data, myID, initState, manager, configInstance, new ProgressUpdater(configInstance, data.projectInfoFile, data.pluginMetaDataCollection));
    }

    /**
     * Creates and sets up a Scheduler instance with the given project data. Sets the Scheduler's running state to the given TaskState.
     * Call Start() eventually, if initState is TaskState.STOPPED, to start the project processing.
     *
     * @param data  - SchedulerData describing the project to setup for
     * @param myID  - a unique ID for this Scheduler instance
     * @param initState  - Initial TaskState to set this Scheduler to.
     * @param manager  - reference to the EASTWebManager creating this Scheduler
     * @param configInstance
     * @param progressUpdater
     */
    public Scheduler(SchedulerData data, int myID, TaskState initState, EASTWebManagerI manager, Config configInstance, ProgressUpdater progressUpdater)
    {
        System.out.println("Setting up new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "'.");
        this.data = data;
        projectInfoFile = data.projectInfoFile;
        pluginMetaDataCollection = data.pluginMetaDataCollection;
        clearIntermediateFiles = data.clearIntermediateFiles;

        statusContainer = new SchedulerStatusContainer(configInstance, myID, projectInfoFile.GetStartDate(), projectInfoFile.GetEndDate(), progressUpdater, projectInfoFile, pluginMetaDataCollection, initState);
        localDownloaders = new ArrayList<LocalDownloader>(1);
        processorProcesses = new ArrayList<Process>(1);
        indicesProcesses = new ArrayList<Process>(1);
        summaryProcesses = new ArrayList<Process>(1);
        downloadCaches = new ArrayList<DatabaseCache>(1);
        processorCaches = new ArrayList<DatabaseCache>(1);
        indicesCaches = new ArrayList<DatabaseCache>(1);
        ID = myID;
        this.configInstance = configInstance;
        this.manager = manager;

        // Make sure directory layout is set up
        File temp;
        try{
            temp = new File(FileSystem.GetRootDirectoryPath(projectInfoFile));
            temp.mkdirs();
            if(!temp.exists()) {
                throw new FileNotFoundException("Directory structure \"" + temp.getCanonicalPath() + "\" doesn't exist");
            }
            temp = new File(FileSystem.GetProjectDirectoryPath(projectInfoFile.GetFullPath()));
            temp.mkdirs();
            if(!temp.exists()) {
                throw new FileNotFoundException("Directory structure \"" + temp.getCanonicalPath() + "\" doesn't exist");
            }
        } catch(NullPointerException | SecurityException | IOException e) {
            ErrorLog.add("Problem while setting up directories.", e);
        } catch(Exception e){
            ErrorLog.add("Problem while setting up directories.", e);
        }

        PluginMetaData pluginMetaData;
        DatabaseConnection con = null;
        try {
            // Setup project
            con = DatabaseConnector.getConnection(configInstance);
            if(con == null) {
                return;
            }

            for(ProjectInfoPlugin item: data.projectInfoFile.GetPlugins())
            {
                pluginMetaData = pluginMetaDataCollection.pluginMetaDataMap.get(item.GetName());
                new File(FileSystem.GetGlobalDownloadDirectory(configInstance, item.GetName(), pluginMetaData.Download.name)).mkdirs();

                Schemas.CreateProjectPluginSchema(con, configInstance.getGlobalSchema(), projectInfoFile, item.GetName(), configInstance.getSummaryCalculations(),
                        configInstance.getSummaryTempCompStrategies(), pluginMetaData.DaysPerInputData, pluginMetaData.Download.filesPerDay, true);
                // Schemas.CreateSchema(configInstance.getGlobalSchema(), projectInfoFile, item.GetName(), configInstance.getSummaryCalculations(),
                //         configInstance.getSummaryTempCompStrategies(), pluginMetaData.DaysPerInputData, pluginMetaData.Download.filesPerDay, true);
                SetupProcesses(item, pluginMetaData);
            }
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException | ParseException | ParserConfigurationException | SAXException | IOException e) {
            ErrorLog.add(this, "Problem setting up Scheduler.", e);
        } catch (Exception e) {
            ErrorLog.add(this, "Problem setting up Scheduler.", e);
        }
        finally {
            if(con != null) {
                con.close();
            }
        }

        // Update status in EASTWebManager
        SchedulerStatus status = null;
        synchronized (statusContainer)
        {
            try {
                status = statusContainer.GetStatus();
            } catch (SQLException e) {
                ErrorLog.add(this, "Problem setting up Scheduler.", e);
            }
        }
        if(status != null) {
            manager.NotifyUI(status);
        }
        System.out.println("Done setting up new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "'.");
    }

    /**
     * Returns this Scheduler's ID number that acts similar to a system process ID.
     *
     * @author michael.devos
     * @return unique ID number
     */
    public int GetID() { return ID; }

    /**
     * Retrieves the ClearIntermediateFiles flag. This flag is TRUE if directories/files in Process temp directories and in output directories for Download, Processor,
     * & Indices should be deleted once no longer needed.
     *
     * @return  ClearIntermediateFiles flag value
     */
    public boolean GetClearIntermediateFilesFlag() { return new Boolean(clearIntermediateFiles); }

    /**
     * Returns the current status of this Scheduler.
     *
     * @author michael.devos
     * @return SchedulerStatus - represents status of this Scheduler instance at that point in time
     */
    public SchedulerStatus GetSchedulerStatus()
    {
        SchedulerStatus status = null;
        System.out.println("Retrieving Scheduler Status.");
        synchronized (statusContainer)
        {
            try {
                status = statusContainer.GetStatus();
            } catch (SQLException e) {
                ErrorLog.add(this, "Problem while getting SchedulerStatus instance.", e);
            }
        }
        System.out.println("Scheduler Status retrieved.");
        return status;
    }

    /**
     * Updates the Scheduler's {@link TaskState TaskState} to RUNNING notifying all observers of said state of the change.
     *
     * @author michael.devos
     */
    public void Start()
    {
        TaskState myState;
        String projectSchema;

        DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        if(con == null) {
            return;
        }

        synchronized (statusContainer) {
            myState = statusContainer.GetState();
        }

        if(myState != TaskState.STOPPED) {
            con.close();
            return;
        }

        synchronized (statusContainer) {
            statusContainer.UpdateSchedulerTaskState(TaskState.STARTING);
        }

        Statement stmt = null;
        String updateQueryFormat;
        try {
            System.out.println("Starting Scheduler for project '" + projectInfoFile.GetProjectName() + "'.");
            stmt = con.createStatement();
            updateQueryFormat = "UPDATE \"%1$s\".\"%2$s\" SET \"Retrieved\" = FALSE WHERE \"Processed\" = FALSE;";
            for(ProjectInfoPlugin item: projectInfoFile.GetPlugins())
            {
                projectSchema = Schemas.getSchemaName(projectInfoFile.GetParentProjectName(), item.GetName());
                stmt.addBatch(String.format(updateQueryFormat, projectSchema, "DownloadCache"));
                stmt.addBatch(String.format(updateQueryFormat, projectSchema, "DownloadCacheExtra"));
                stmt.addBatch(String.format(updateQueryFormat, projectSchema, "ProcessorCache"));
                stmt.addBatch(String.format(updateQueryFormat, projectSchema, "IndicesCache"));
            }
            stmt.executeBatch();
            stmt.close();
        } catch(SQLException e) {
            ErrorLog.add(this, "Problem while updating caches to prepare for start or restart.", e);
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    ErrorLog.add(this, "Problem while updating caches to prepare for start or restart.", e);
                }
            }
            con.close();
        }

        synchronized (statusContainer) {
            statusContainer.UpdateSchedulerTaskState(TaskState.RUNNING);
        }

        try {
            AttemptUpdate();
        } catch (SQLException | ClassNotFoundException | ParserConfigurationException | SAXException | IOException e) {
            ErrorLog.add(this, "Problem while attempting update of Scheduler for project '" + projectInfoFile.GetProjectName() + "'.", e);
        }
        System.out.println("Finished starting Scheduler for project '" + projectInfoFile.GetProjectName() + "'.");
    }

    /**
     * Updates the Scheduler's {@link TaskState TaskState} to STOPPED notifying all observers of said state of the change.
     * @author michael.devos
     *
     * @return  TRUE if successful at stopping all workers.
     */
    public boolean Stop()
    {
        TaskState myState;
        SchedulerStatus status = null;

        System.out.println("Stopping Scheduler for project '" + projectInfoFile.GetProjectName() + "'.");

        synchronized (statusContainer) {
            myState = statusContainer.GetState();
        }

        if(myState == TaskState.STARTING || myState == TaskState.STOPPING) {
            return false;
        } else if(myState == TaskState.STOPPED){
            return true;
        }

        synchronized (statusContainer)
        {
            statusContainer.UpdateSchedulerTaskState(TaskState.STOPPING);
            try {
                status = statusContainer.GetStatus();
            } catch (SQLException e) {
                ErrorLog.add(this, "Problem while getting SchedulerStatus.", e);
            }
        }

        boolean allStopped = true;
        final int maxWaitTimeMS = 1200000;  // 20 minutes
        int currentWaitedTimeMS = 0;
        do {
            try {
                if(!allStopped)
                {
                    Thread.sleep(5000);
                    currentWaitedTimeMS += 5000;
                    allStopped = true;
                }

                synchronized (statusContainer) {
                    status = statusContainer.GetStatus();
                }
            } catch (SQLException e) {
                ErrorLog.add(this, "Problem while getting SchedulerStatus.", e);
            } catch (InterruptedException e) {
                ErrorLog.add(this, "Problem while making thread sleep.", e);
            }

            if(status != null) {
                Iterator<ProcessName> it = status.GetWorkersInQueuePerProcess().keySet().iterator();
                while(it.hasNext()) {
                    if(status.GetWorkersInQueuePerProcess().get(it.next()) > 0) {
                        allStopped = false;
                        break;
                    }
                }

                if(allStopped)
                {
                    it = status.GetActiveWorkersPerProcess().keySet().iterator();
                    while(it.hasNext()) {
                        if(status.GetActiveWorkersPerProcess().get(it.next()) > 0) {
                            allStopped = false;
                            break;
                        }
                    }
                }
            }
        } while(!allStopped && (currentWaitedTimeMS < maxWaitTimeMS));

        if(!allStopped) {
            ErrorLog.add(this, "Timed out waiting for Process Workers to complete.", new Exception("Timed out waiting for Process Workers to complete."));
            return false;
        }

        synchronized (statusContainer) {
            statusContainer.UpdateSchedulerTaskState(TaskState.STOPPED);
            try {
                status = statusContainer.GetStatus();
            } catch (SQLException e) {
                ErrorLog.add(this, "Problem while getting SchedulerStatus.", e);
                return true;
            }
        }

        if(status != null) {
            manager.NotifyUI(status);
        }

        System.out.println("Finished stopping Scheduler for project '" + projectInfoFile.GetProjectName() + "'.");
        return true;
    }

    /**
     * Deletes the project schemas and working directory.
     *
     * @return  TRUE if successfully deleted project.
     */
    public boolean Delete()
    {
        DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        Connection con1 = RDS.getCentralDBconnection();
        if(con == null) {
            return false;
        }
        if(con1 == null) {
            return false;
        }

        // First stop Scheduler
        if(!Stop()) {
            con.close();
            try {
                con1.close();
            } catch (SQLException e) { e.printStackTrace();}
            return false;
        }

        // Set to DELETING state
        synchronized (statusContainer) {
            statusContainer.UpdateSchedulerTaskState(TaskState.DELETING);
        }

        // Go about deleting project
        String projectSchema;
        int projectID;
        File projectDir;
        Statement stmt, stmt1 = null;
        String dropSchemaQueryFormat = "DROP SCHEMA \"%1$s\" CASCADE;";
        String deleteFromExpectedTotalOutput = "delete from \"" + configInstance.getGlobalSchema() + "\".\"%1$sExpectedTotalOutput\" where \"ProjectID\"=%2$d;";
        try {
            stmt = con.createStatement();
            stmt1 = con1.createStatement();

            for(ProjectInfoPlugin item: projectInfoFile.GetPlugins())
            {
                projectSchema = Schemas.getSchemaName(projectInfoFile.GetParentProjectName(), item.GetName());
                projectID = Schemas.getProjectID(configInstance.getGlobalSchema(), projectInfoFile.GetProjectName(), stmt1);
                stmt.addBatch(String.format(dropSchemaQueryFormat, projectSchema));
                stmt1.addBatch(String.format(deleteFromExpectedTotalOutput, "Download", projectID));
                stmt1.addBatch(String.format(deleteFromExpectedTotalOutput, "Processor", projectID));
                stmt1.addBatch(String.format(deleteFromExpectedTotalOutput, "Indices", projectID));
                stmt1.addBatch(String.format("delete from \"" + configInstance.getGlobalSchema() + "\".\"SummaryExpectedTotalOutput\" where \"ProjectSummaryID\"="
                        + " any (select \"ProjectSummaryID\" from \"" + configInstance.getGlobalSchema() + "\".\"ProjectSummary\" where \"ProjectID\"=%1$d);", projectID));
                stmt1.addBatch(String.format("delete from \"" + configInstance.getGlobalSchema() + "\".\"ProjectSummary\" where \"ProjectID\"=%1$d", projectID));
                stmt1.addBatch(String.format("delete from \"" + configInstance.getGlobalSchema() + "\".\"Project\" where \"ProjectID\"=%1$d", projectID));
                projectDir = new File(FileSystem.GetProjectDirectoryPath(projectInfoFile.GetFullPath()));
                if(projectDir.exists()) {
                    FileUtils.deleteDirectory(projectDir);
                }
            }
            stmt.executeBatch();
            stmt1.executeBatch();
            stmt1.close();
            stmt.close();
        } catch(SQLException e) {
            ErrorLog.add(this, "Problem while deleting project, \"" + projectInfoFile.GetProjectName() + "\".", e);
        } catch (IOException e) {
            ErrorLog.add(this, "Problem while deleting project directory for project '" + projectInfoFile.GetProjectName() + "'.",
                    new Exception("Problem while deleting project directory for project '" + projectInfoFile.GetProjectName() + "'."));
        } finally {
            stmt = null;
            stmt1 = null;
            try {
                con1.close();
            } catch (SQLException e) {e.printStackTrace();}
            con.close();
        }

        return true;
    }

    /**
     * Gets this scheduler's thread state.
     *
     * @author michael.devos
     * @return scheduler's current thread state
     */
    public TaskState GetState()
    {
        TaskState myState;
        //            System.out.println("Retrieving current Scheduler TaskState.");
        synchronized (statusContainer)
        {
            myState = statusContainer.GetState();
        }
        //            System.out.println("Current Scheduler TaskState retrieved.");
        return myState;
    }

    /**
     * Used by the executed frameworks ({@link Process Process} objects) to send information up to the GUI.
     *
     * @param event  - GUI update information
     */
    public void NotifyUI(GeneralUIEventObject event)
    {
        SchedulerStatus status = null;
        System.out.println("Handling Scheduler event.");
        synchronized (statusContainer)
        {
            if(event.getStatus() != null) {
                statusContainer.AddToLog(event.getStatus());
            }

            try {
                status = statusContainer.GetStatus();
            } catch (SQLException e) {
                ErrorLog.add(this, "Problem while getting SchedulerStatus instance.", e);
            }
        }
        System.out.println("Scheduler event handled.");
        if(status != null) {
            manager.NotifyUI(status);
        }
    }

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
        System.out.println("Updating Download progress of '" + dataName + "' for plugin '" + pluginName + "'.");
        synchronized (statusContainer)
        {
            statusContainer.UpdateDownloadProgressByData(dataName, pluginName, listDatesFiles, modisTileNames, stmt);
        }
        UpdateStatus();
        System.out.println("Done updating Download progress of '" + dataName + "' for plugin '" + pluginName + "'.");
    }

    /**
     * Updates the processor progress for the given plugin.
     * @param pluginName  - name of plugin whose processor progress to change
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateProcessorProgress(String pluginName, Statement stmt) throws SQLException
    {
        System.out.println("Updating Processor progress for plugin '" + pluginName + "'.");
        synchronized (statusContainer)
        {
            statusContainer.UpdateProcessorProgress(pluginName, stmt);
        }
        UpdateStatus();
        System.out.println("Done updating Processor progress for plugin '" + pluginName + "'.");
    }

    /**
     * Updates the indices progress for the given plugin.
     * @param pluginName  - name of plugin whose processor progress to change
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateIndicesProgress(String pluginName, Statement stmt) throws SQLException
    {
        System.out.println("Updating Indices progress for plugin '" + pluginName + "'.");
        synchronized (statusContainer)
        {
            statusContainer.UpdateIndicesProgress(pluginName, stmt);
        }
        UpdateStatus();
        System.out.println("Done updating Indices progress for plugin '" + pluginName + "'.");
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
        System.out.println("Updating Summary progress of summary " + summaryIDNum + " for plugin '" + pluginInfo.GetName() + "'.");
        synchronized (statusContainer)
        {
            statusContainer.UpdateSummaryProgress(summaryIDNum, compStrategy, daysPerInputData, pluginInfo, stmt);
        }
        UpdateStatus();
        System.out.println("Done updating Summary progress of summary " + summaryIDNum + " for plugin '" + pluginInfo.GetName() + "'.");
    }

    /**
     * Checks for new work from associated GlobalDownloaders using the stored LocalDownloaders which gets the number of available files to process from each of them and updates their local caches to
     * start processing the new files.
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void AttemptUpdate() throws ClassNotFoundException, SQLException, ParserConfigurationException, SAXException, IOException
    {
        System.out.println("Attempting update of Scheduler for project '" + projectInfoFile.GetProjectName() + "'.");
        for(DatabaseCache cache : downloadCaches)
        {
            cache.NotifyObserversToCheckForPastUpdates();
        }
        for(DatabaseCache cache : processorCaches)
        {
            cache.NotifyObserversToCheckForPastUpdates();
        }
        for(DatabaseCache cache : indicesCaches)
        {
            cache.NotifyObserversToCheckForPastUpdates();
        }

        for(LocalDownloader dl : localDownloaders)
        {
            dl.AttemptUpdate();
        }

        UpdateStatus();
        System.out.println("Finished attempting update of Scheduler for project '" + projectInfoFile.GetProjectName() + "'.");
    }

    /**
     * Handles starting new process workers within the Scheduler's project.
     *
     * @param worker  - ProcessWorker to submit to a thread pool
     * @return future handle object
     */
    public Future<ProcessWorkerReturn> StartNewProcessWorker(ProcessWorker worker)
    {
        SchedulerWorker sWorker = new SchedulerWorker(worker, statusContainer, this);
        try {
            UpdateStatus();
        } catch (SQLException e) {
            ErrorLog.add(this, "Problem while updating status after starting new ProcessWorker.", e);
        }
        return manager.StartNewProcessWorker(sWorker);
    }

    protected void UpdateStatus() throws SQLException
    {
        SchedulerStatus status = null;
        synchronized (statusContainer) {
            status = statusContainer.GetStatus();
        }

        if(status != null) {
            manager.NotifyUI(status);
        }
    }

    protected Scheduler(ProjectInfoFile projectInfoFile, PluginMetaDataCollection pluginMetaDataCollection, boolean clearIntermediateFiles, int myID, Config configInstance,
            EASTWebManagerI manager)
    {
        data = new SchedulerData(projectInfoFile, pluginMetaDataCollection, clearIntermediateFiles);
        this.projectInfoFile = projectInfoFile;
        this.pluginMetaDataCollection = pluginMetaDataCollection;
        ID = myID;
        this.configInstance = configInstance;
        this.manager = manager;
    }

    protected Scheduler(ProjectInfoFile projectInfoFile, PluginMetaDataCollection pluginMetaDataCollection, boolean clearIntermediateFiles, int myID, Config configInstance,
            EASTWebManagerI manager, SchedulerStatusContainer statusContainer)
    {
        data = new SchedulerData(projectInfoFile, pluginMetaDataCollection, clearIntermediateFiles);
        this.projectInfoFile = projectInfoFile;
        this.pluginMetaDataCollection = pluginMetaDataCollection;
        ID = myID;
        this.configInstance = configInstance;
        this.manager = manager;
        this.statusContainer = statusContainer;
    }

    /**
     * Sets up a set of Process extending classes to act as ProcesWorker Managers for each of the four frameworks for each ProjectInfoPlugin given.
     *
     * @author michael.devos
     *
     * @param pluginInfo  - plugin information
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws ParseException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    protected void SetupProcesses(ProjectInfoPlugin pluginInfo, PluginMetaData pluginMetaData) throws NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException,
    IllegalAccessException, IllegalArgumentException, InvocationTargetException, ParseException, IOException, ParserConfigurationException, SAXException
    {
        //Scheduler scheduler, String globalSchema, String projectName, String pluginName, String workingDir, ProcessName processCachingFor, ArrayList<String> extraDownloadFiles
        String projectName = null;
        String fullPath = data.projectInfoFile.GetFullPath();
        if(fullPath.endsWith("\\") || fullPath.endsWith("/")){
            projectName = fullPath.substring(0, -1).replace('\\', '/');
        }
        else{
            projectName = fullPath.replace('\\', '/');
        }
        projectName = projectName.substring(projectName.lastIndexOf('/')+1);
        DatabaseCache downloadCache = new DatabaseCache(this, configInstance.getGlobalSchema(), projectName, pluginInfo, pluginMetaData, data.projectInfoFile.GetWorkingDir(),
                ProcessName.DOWNLOAD);
        DatabaseCache processorCache = new DatabaseCache(this, configInstance.getGlobalSchema(), projectName, pluginInfo, pluginMetaData, data.projectInfoFile.GetWorkingDir(),
                ProcessName.PROCESSOR);
        DatabaseCache indicesCache = new DatabaseCache(this, configInstance.getGlobalSchema(), projectName, pluginInfo, pluginMetaData, data.projectInfoFile.GetWorkingDir(),
                ProcessName.INDICES);
        DatabaseCache outputCache = new DatabaseCache(this, configInstance.getGlobalSchema(), projectName, pluginInfo, pluginMetaData, data.projectInfoFile.GetWorkingDir(),
                ProcessName.SUMMARY);
        localDownloaders.addAll(SetupDownloadProcess(pluginInfo, pluginMetaData, downloadCache));
        processorProcesses.add(SetupProcessorProcess(pluginInfo, pluginMetaData, downloadCache, processorCache));
        indicesProcesses.add(SetupIndicesProcess(pluginInfo, pluginMetaData, processorCache, indicesCache));
        summaryProcesses.add(SetupSummaryProcess(pluginInfo, pluginMetaData, indicesCache, outputCache));

        downloadCaches.add(downloadCache);
        processorCaches.add(processorCache);
        indicesCaches.add(indicesCache);
    }

    /**
     * Sets up a {@link GenericFrameworkProcess GenericFrameworkProcess} object to manage DownloadWorkers.
     *
     * @author michael.devos
     *
     * @param pluginInfo  - plugin information
     * @param pluginMetaData  - plugin information gotten from a PluginMetaData.*.xml
     * @param outputCache  - DatabaseCache object used to cache output files from a LocaldDownloader
     * @return general concrete Process object for managing ProcessWorkers
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IOException
     */
    protected ArrayList<LocalDownloader> SetupDownloadProcess(ProjectInfoPlugin pluginInfo, PluginMetaData pluginMetaData, DatabaseCache outputCache) throws ClassNotFoundException, NoSuchMethodException,
    SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, IOException
    {
        ArrayList<LocalDownloader> lDownloders = new ArrayList<LocalDownloader>();

        // Create "data" DownloadFactory
        Class<?> downloadFactoryClass = Class.forName("version2.prototype.download." + pluginInfo.GetName() + "." + pluginMetaData.Download.downloadFactoryClassName);
        Constructor<?> downloadFactoryCtor = downloadFactoryClass.getConstructor(Config.class, ProjectInfoFile.class, ProjectInfoPlugin.class, DownloadMetaData.class, PluginMetaData.class,
                Scheduler.class, DatabaseCache.class, LocalDate.class, LocalDate.class);
        DownloadFactory downloadFactory = (DownloadFactory) downloadFactoryCtor.newInstance(configInstance, projectInfoFile, pluginInfo, pluginMetaData.Download, pluginMetaData, this, outputCache,
                projectInfoFile.GetStartDate(), projectInfoFile.GetEndDate());

        // Create "data" GenericGlobalDownloader
        lDownloders.add(manager.StartGlobalDownloader(downloadFactory));

        for(DownloadMetaData dlMetaData : pluginMetaData.Download.extraDownloads)
        {
            // Create extra ListDatesFiles instance
            downloadFactoryClass = Class.forName("version2.prototype.download." + pluginInfo.GetName() + "." + dlMetaData.downloadFactoryClassName);
            downloadFactoryCtor = downloadFactoryClass.getConstructor(Config.class, ProjectInfoFile.class, ProjectInfoPlugin.class, DownloadMetaData.class, PluginMetaData.class,
                    Scheduler.class, DatabaseCache.class, LocalDate.class, LocalDate.class);
            downloadFactory = (DownloadFactory) downloadFactoryCtor.newInstance(configInstance, projectInfoFile, pluginInfo, dlMetaData, pluginMetaData, this, outputCache, projectInfoFile.GetStartDate(),projectInfoFile.GetEndDate());

            // Create extra GenericGlobalDownloader
            lDownloders.add(manager.StartGlobalDownloader(downloadFactory));
        }

        return lDownloders;
    }

    /**
     * Sets up a {@link GenericFrameworkProcess GenericFrameworkProcess} object to manage ProcessorWorkers.
     *
     * @author michael.devos
     *
     * @param pluginInfo  - plugin information
     * @param pluginMetaData  - plugin information gotten from a PluginMetaData.*.xml
     * @param inputCache  - DatabaseCache object used to acquire files available for processor processing
     * @param outputCache  - DatabaseCache object used to cache output files from processor processing
     * @return general concrete Process object for managing ProcessWorkers
     * @throws ClassNotFoundException
     */
    protected Process SetupProcessorProcess(ProjectInfoPlugin pluginInfo, PluginMetaData pluginMetaData, DatabaseCache inputCache, DatabaseCache outputCache) throws ClassNotFoundException {

        // If desired, GenericFrameworkProcess can be replaced with a custom Process extending class.
        Process process = new GenericProcess<ProcessorWorker>(configInstance, ProcessName.PROCESSOR, projectInfoFile, pluginInfo, pluginMetaData, this, inputCache, outputCache,
                "version2.prototype.processor.ProcessorWorker");
        //        inputCache.addObserver(process);
        return process;
    }

    /**
     * Sets up a {@link GenericFrameworkProcess GenericFrameworkProcess} object to manage IndicesWorkers.
     *
     * @author michael.devos
     *
     * @param pluginInfo  - plugin information
     * @param pluginMetaData  - plugin information gotten from a PluginMetaData.*.xml
     * @param inputCache  - DatabacheCache object used to acquire files available for indices processing
     * @param outputCache  - DatabaseCache object used to cache output files from indices processing
     * @return general concrete Process object for managing ProcessWorkers
     * @throws ClassNotFoundException
     */
    protected Process SetupIndicesProcess(ProjectInfoPlugin pluginInfo, PluginMetaData pluginMetaData, DatabaseCache inputCache, DatabaseCache outputCache) throws ClassNotFoundException {
        // If desired, GenericFrameworkProcess can be replaced with a custom Process extending class.
        Process process = new GenericProcess<IndicesWorker>(configInstance, ProcessName.INDICES, projectInfoFile, pluginInfo, pluginMetaData, this, inputCache, outputCache,
                "version2.prototype.indices.IndicesWorker");
        //        inputCache.addObserver(process);
        return process;
    }

    /**
     * Sets up a {@link version2.prototype.summary.Summary Summary} object to manage SummaryWorkers.
     *
     * @author michael.devos
     *
     * @param pluginInfo  - plugin information
     * @param pluginMetaData  - plugin information gotten from a PluginMetaData.*.xml
     * @return {@link Summary Summary}  - the manager object of SummaryWorkers for the current project and given plugin
     * @param inputCache  - the DatabaseCache object used to acquire files available for summary processing
     */
    protected Process SetupSummaryProcess(ProjectInfoPlugin pluginInfo, PluginMetaData pluginMetaData, DatabaseCache inputCache, DatabaseCache outputCache) throws ClassNotFoundException
    {
        Summary process = new Summary(configInstance, projectInfoFile, pluginInfo, pluginMetaData, this, inputCache, outputCache);
        //        Process process = new GenericProcess<IndicesWorker>(manager, configInstance, ProcessName.SUMMARY, projectInfoFile, pluginInfo, pluginMetaData, this, inputCache, outputCache,
        //                "version2.prototype.summary.SummaryWorker");
        //        inputCache.addObserver(process);
        return process;
    }
}

