package version2.prototype;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseCache;

/**
 * Abstract framework worker class. Frameworks are to use a concrete class that extends this class to handle doing their required processing work.
 *
 * @author michael.devos
 *
 */
public abstract class ProcessWorker implements Callable<ProcessWorkerReturn> {
    /**
     * The name of the ProcessWorker.
     */
    public final String processWorkerName;
    public final Config configInstance;
    public final Process process;
    public final ProjectInfoFile projectInfoFile;
    public final ProjectInfoPlugin pluginInfo;
    public final PluginMetaData pluginMetaData;
    protected final ArrayList<DataFileMetaData> cachedFiles;
    protected final DatabaseCache outputCache;
    protected TaskState tState;

    /**
     * Creates a ProcessWorker object labeled by the given processWorkerName, owned by the given process, and set to work on the given cachedFiles.
     *
     * @param configInstance  - Config reference to use
     * @param processWorkerName  - name of this worker
     * @param process  - reference to the owning process object
     * @param projectInfoFile  - the current project's information
     * @param pluginInfo  - the current plugin's general information
     * @param pluginMetaData  - the current plugin's xml data mapped
     * @param cachedFiles  - the files to process
     * @param outputCache  - DatbaseCache instance to use as the outputCache
     */
    protected ProcessWorker(Config configInstance, String processWorkerName, Process process, ProjectInfoFile projectInfoFile, ProjectInfoPlugin pluginInfo,
            PluginMetaData pluginMetaData, ArrayList<DataFileMetaData> cachedFiles, DatabaseCache outputCache)
    {
        this.configInstance = configInstance;
        this.processWorkerName = processWorkerName;
        this.process = process;
        this.projectInfoFile = projectInfoFile;
        this.pluginInfo = pluginInfo;
        this.pluginMetaData = pluginMetaData;
        this.cachedFiles = cachedFiles;
        this.outputCache = outputCache;
        tState = TaskState.STOPPED;
    }

    /**
     * Method to override to handle the processing to be done in the implementing class. Called only when Scheduler TaskState is set to STARTED.
     */
    public abstract ProcessWorkerReturn process();

    public abstract boolean verifyResults();

    public void setTaskState(TaskState state)
    {
        synchronized(tState) {
            tState = state;
        }
    }

    public TaskState getTaskState()
    {
        TaskState temp;
        synchronized(tState) {
            temp = tState;
        }
        return temp;
    }

    /* (non-Javadoc)
     * @see java.util.concurrent.Callable#call()
     */
    @Override
    public ProcessWorkerReturn call() throws Exception {
        if((process.getState() == TaskState.STARTED || process.getState() == TaskState.STARTING || process.getState() == TaskState.RUNNING) && !Thread.currentThread().isInterrupted()) {
            // Common prep code
            ProcessWorkerReturn retVal = process();
            // Common cleanup code
            return retVal;
        } else {
            return null;
        }
    }
}
