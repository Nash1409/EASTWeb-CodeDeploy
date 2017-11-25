package version2.prototype;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.Scheduler;
import version2.prototype.Scheduler.SchedulerData;
import version2.prototype.Scheduler.SchedulerStatus;
import version2.prototype.download.DownloadFactory;
import version2.prototype.download.DownloaderFactory;
import version2.prototype.download.GlobalDownloader;
import version2.prototype.download.LocalDownloader;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnectionPoolA;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.ParallelUtils.NamedThreadFactory;

/**
 * Threading management class for EASTWeb. All spawning, executing, and stopping of threads is handled through this class in order for it to manage
 * the number of currently processing threads for each resource group. This class throttles execution where necessary to allow fair processing time
 * to all threads and to manage memory usage across the system. All communication between the presentation layer (GUI) and EASTWeb processes and other
 * are handled by this class as well.
 *
 * @author michael.devos
 *
 */
public class EASTWebManager implements Runnable, EASTWebManagerI{
    protected static EASTWebManager instance = null;
    protected static ScheduledExecutorService backgroundThreadExecutor = null;
    protected static int defaultNumOfSimultaneousGlobalDLs = 1;
    //    protected static int defaultMSBeetweenUpdates = 300000;       // 5 minutes
    protected static int defaultMSBeetweenUpdates = 5000;       // 5 seconds
    protected static final int NUM_CORES = Runtime.getRuntime().availableProcessors();

    // Logged requests from other threads
    protected static Boolean acceptingRequests = true;
    protected static List<SchedulerData> startNewSchedulerRequests;
    protected static List<SchedulerData> loadNewSchedulerRequests;
    protected static List<Integer> stopExistingSchedulerRequests;
    protected static List<String> stopExistingSchedulerRequestsNames;
    protected static List<Integer> deleteExistingSchedulerRequests;
    protected static List<String> deleteExistingSchedulerRequestsNames;
    protected static List<Integer> startExistingSchedulerRequests;
    protected static List<String> startExistingSchedulerRequestsNames;
    protected static HashMap<GUIUpdateHandler, Boolean> guiHandlers = new HashMap<GUIUpdateHandler, Boolean>(0);     // Boolean - TRUE if flagged for removal

    // EASTWebManager state
    public final Config configInstance;
    protected static Integer numOfCreatedGDLs = 0;
    protected static Integer numOfCreatedSchedulers = 0;
    protected static List<SchedulerStatus> schedulerStatuses = new ArrayList<SchedulerStatus>(1);
    protected static BitSet schedulerIDs = new BitSet(100000);
    protected static BitSet globalDLIDs = new BitSet(1000);
    protected static Boolean schedulerStatesChanged = false;
    protected Boolean justCreateNewSchedulers;
    protected final int msBetweenUpdates;
    protected final DatabaseConnectionPoolA connectionPool;

    // Object references of EASTWeb components
    protected HashMap<Integer, GlobalDownloader> globalDLs;
    protected HashMap<Integer, Scheduler> schedulers;
    protected final ScheduledThreadPoolExecutor globalDLExecutor;
    protected HashMap<Integer, ScheduledFuture<?>> globalDLFutures;
    protected final ExecutorService processWorkerExecutor;

    /**
     *  If first time calling, starts up the EASTWebManager background processing thread to handle passively processing logged requests and updating
     *  its state using default values. [numOfSimultaneousGlobalDLs = 1, msBetweenUpdates = 5000]
     */
    public static void Start()
    {
        EASTWebManager.Start(defaultNumOfSimultaneousGlobalDLs, defaultMSBeetweenUpdates);
    }

    /**
     *  If first time calling, starts up the EASTWebManager background processing thread to handle passively processing logged requests and updating
     *  its state.
     *
     * @param numOfSimultaneousGlobalDLs  - number of threads to create for GlobalDownloader objects to use (recommended value: 1)
     * @param msBetweenUpdates  - milliseconds to sleep between state updates and requests processing. If 0 then EASTWebManager won't passively update
     *  its state variables and will require UpdateState() to be manually called in whenever it is desired for requests to be processed and state
     *  variables to be updated.
     */
    public static void Start(int numOfSimultaneousGlobalDLs, int msBetweenUpdates)
    {
        try{
            if(instance == null)
            {
                int numOfWorkerThreads = NUM_CORES < 4 ?
                        1 : ((NUM_CORES - 1 - numOfSimultaneousGlobalDLs) <= 1 ?
                                1 : NUM_CORES - 1 - numOfSimultaneousGlobalDLs);
                instance = new EASTWebManager(
                        numOfSimultaneousGlobalDLs,  // Number of Global Downloaders allowed to be simultaneously active
                        numOfWorkerThreads, // Number of ProcessWorkers allowed to be simultaneously active
                        msBetweenUpdates
                        );

                // If passive updating desired
                if(msBetweenUpdates > 0)
                {
                    backgroundThreadExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable target) {
                            final Thread thread = new Thread(target);
                            //                log.debug("Creating new worker thread");
                            thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                                @Override
                                public void uncaughtException(Thread t, Throwable e) {
                                    //                        log.error("Uncaught Exception", e);
                                }
                            });
                            return thread;
                        }
                    });
                    backgroundThreadExecutor.scheduleWithFixedDelay(instance, 0, msBetweenUpdates, TimeUnit.MILLISECONDS);
                }
                if(instance != null) {
                    System.out.println("EASTWeb started. Threads being used: " + (2 + numOfSimultaneousGlobalDLs + numOfWorkerThreads)
                            + " {GUI threads: 1 reserved, EASTWebManagement threads: 1, "
                            + "GlobalDownload threads: " + numOfSimultaneousGlobalDLs
                            + ", ProcessWorker threads: " + numOfWorkerThreads
                            + ", " + (msBetweenUpdates > 0 ? "ms between updates: " + msBetweenUpdates : "no automatic GUI updating") + "}");
                }
            }
        } catch(Exception e) {
            ErrorLog.add(instance.configInstance, "Problem while starting EASTWebManager.", e);
        }

        Config configInstance = Config.getInstance();
        if(!testDatabaseConnection(configInstance)) {
            instance = null;
        }
    }

    /**
     * Safely handles the stopping and shutdown of EASTWeb and all managed resources before returning.
     */
    public static void StopAndShutdown()
    {
        System.out.println("Shutting down EASTWeb.");

        if(instance == null) {
            System.out.println("Instance was null");
            return;
        }

        /**
         * 1) Stop accepting new requests.
         * 2) Stop background thread.
         * 3) Stop all Schedulers.
         * 4) Stop all GlobalDownloaders and their tasks.
         * 5) Shutdown thread pool executers.
         * 6) Wait for currently executing ProcessWorkers to finish.
         * 7) Shutdown database connection pool.
         */

        // Step 1
        synchronized(acceptingRequests) {
            acceptingRequests = false;
        }

        // Step 2
        backgroundThreadExecutor.shutdownNow();

        // Steps 3-6
        instance.stopAndShutdown();

        // Step 7
        DatabaseConnector.Close();

        System.out.println("EASTWeb successfully shut down.");
    }

    /**
     * Retrieves the names of the registered TemporalSummaryCompositionStrategy implementing classes available for use in temporal summaries.
     * @return ArrayList of class name strings of the TemporalSummaryCompositionStrategy implementing classes usable for temporal summary calculation
     */
    public static ArrayList<String> GetRegisteredTemporalSummaryCompositionStrategies()
    {
        ArrayList<String> strategyNames = new ArrayList<String>();
        if(instance != null)
        {
            strategyNames = instance.configInstance.getSummaryTempCompStrategies();
        }
        else
        {
            strategyNames = Config.getInstance().getSummaryTempCompStrategies();
        }
        strategyNames.add(0,"(No Selection)");

        return strategyNames;
    }

    /**
     * If {@link #Start Start(int, int)} has been previously called then forces EASTWebManager to process any and all currently logged requests and
     * update state information. Note, that if Start(int, int) was called with value of 0 or less for msBetweenUpdates then this method MUST be called
     * in order to process any of the requests made to EASTWebManager via its public methods and in order for it to show updated state information.
     */
    public static void UpdateState()
    {
        if(instance != null) {
            instance.run();
        }
    }

    /**
     * Requests for a new {@link version2.prototype.Scheduler#Scheduler Scheduler} to be started and keeps its reference for later status retrieval and stopping.
     * The created Scheduler can be identified via its {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus} object gotten from calling
     * {@link #GetSchedulerStatus GetSchedulerStatus()}.
     *
     * @param data - {@link version2.prototype.Scheduler#SchedulerData SchedulerData} to create the Scheduler instance from
     * @param forceUpdate  - forces an immediate update to start the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean StartNewScheduler(SchedulerData data, boolean forceUpdate)
    {
        if(!acceptingRequests) {
            return false;
        }

        if(instance == null){
            EASTWebManager.Start(defaultNumOfSimultaneousGlobalDLs, defaultMSBeetweenUpdates);
        }

        synchronized (startNewSchedulerRequests) {
            startNewSchedulerRequests.add(data);
        }

        if(forceUpdate)
        {
            if(instance != null) {
                synchronized(instance.justCreateNewSchedulers) {
                    instance.justCreateNewSchedulers = true;
                }
            }
            UpdateState();
        }
        return true;
    }

    /**
     * Requests for a new {@link version2.prototype.Scheduler#Scheduler Scheduler} to be created but the project not set to STARTED status and keeps its reference
     * for later status retrieval and stopping. The created Scheduler can be identified via its {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus}
     * object gotten from calling {@link #GetSchedulerStatus GetSchedulerStatus()}.
     *
     * @param data - {@link version2.prototype.Scheduler#SchedulerData SchedulerData} to create the Scheduler instance from
     * @param forceUpdate  - forces an immediate update to load the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean LoadNewScheduler(SchedulerData data, boolean forceUpdate)
    {
        if(!acceptingRequests) {
            return false;
        }

        if(instance == null) {
            EASTWebManager.Start(defaultNumOfSimultaneousGlobalDLs, defaultMSBeetweenUpdates);
        }

        synchronized (loadNewSchedulerRequests) {
            loadNewSchedulerRequests.add(data);
        }

        if(forceUpdate)
        {
            if(instance != null) {
                synchronized(instance.justCreateNewSchedulers) {
                    instance.justCreateNewSchedulers = true;
                }
            }
            UpdateState();
        }
        return true;
    }

    /**
     * Requests for the {@link version2.prototype.Scheduler#Scheduler Scheduler} with the specified unique schedulerID to be stopped. Sets the
     * {@link version2#TaskState TaskState} value for that Scheduler to STOPPED effectively stopping all associated Process objects. Causes a graceful
     * shutdown of a project since it only keeps Processes from spawning more ProcessWorkers.
     *
     * @param schedulerID  - targeted Scheduler's ID
     * @param forceUpdate  - forces an immediate update to start the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean StopExistingScheduler(int schedulerID, boolean forceUpdate)
    {
        if(!acceptingRequests || instance == null) {
            return false;
        }

        if(schedulerIDs.get(schedulerID))
        {
            synchronized (stopExistingSchedulerRequests) {
                stopExistingSchedulerRequests.add(schedulerID);
            }

            if(forceUpdate)
            {
                UpdateState();
            }
        }
        return true;
    }

    /**
     * Requests for the {@link version2.prototype.Scheduler#Scheduler Scheduler} with the specified unique schedulerID to be stopped. Sets the
     * {@link version2.prototype.TaskState#TaskState TaskState} value for that Scheduler to STOPPED effectively stopping all associated Process objects. Causes a graceful
     * shutdown of a project since it only keeps Processes from spawning more ProcessWorkers.
     *
     * @param projectName  - targeted Scheduler's project name
     * @param forceUpdate  - forces an immediate update to start the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean StopExistingScheduler(String projectName, boolean forceUpdate)
    {
        if(!acceptingRequests || instance == null) {
            return false;
        }

        synchronized (stopExistingSchedulerRequestsNames) {
            stopExistingSchedulerRequestsNames.add(projectName);

            if(forceUpdate)
            {
                UpdateState();
            }
        }
        return true;
    }

    /**
     * Requests for the {@link version2.prototype.Scheduler#Scheduler Scheduler} with the specified unique schedulerID to be deleted from the EASTWebManager's list
     * and stopped. This does what {@link #StopExistingScheduler StopScheStopExistingSchedulerduler(int)} does with the added effect of removing it all references to the Scheduler
     * from this manager. This may or may not remove any GlobalDownloaders currently existing. A
     * {@link version2.prototype.download#GlobalDownloader GlobalDownloader} is only removed when it no longer has any projects currently using it
     * (GlobalDownloader objects are shared amongst Schedulers).
     *
     * @param schedulerID  - targeted Scheduler's ID
     * @param forceUpdate  - forces an immediate update to start the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean DeleteScheduler(int schedulerID, boolean forceUpdate)
    {
        if(!acceptingRequests || instance == null) {
            return false;
        }

        if(schedulerIDs.get(schedulerID))
        {
            synchronized (deleteExistingSchedulerRequests) {
                deleteExistingSchedulerRequests.add(schedulerID);

                if(forceUpdate)
                {
                    UpdateState();
                }
            }
        }
        return true;
    }

    /**
     * Requests for the {@link version2.prototype.Scheduler#Scheduler Scheduler} with the specified project name to be deleted from the EASTWebManager's list
     * and stopped. This does what {@link #StopExistingScheduler StopExistingScheduler(int)} does with the added effect of removing it all references to the Scheduler
     * from this manager. This may or may not remove any GlobalDownloaders currently existing. A
     * {@link version2.prototype.download#GlobalDownloader GlobalDownloader} is only removed when it no longer has any projects currently using it
     * (GlobalDownloader objects are shared amongst Schedulers).
     *
     * @param projectName  - targeted Scheduler's project name
     * @param forceUpdate  - forces an immediate update to start the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean DeleteScheduler(String projectName, boolean forceUpdate)
    {
        if(!acceptingRequests || instance == null) {
            return false;
        }

        synchronized (deleteExistingSchedulerRequestsNames) {
            deleteExistingSchedulerRequestsNames.add(projectName);

            if(forceUpdate)
            {
                UpdateState();
            }
        }
        return true;
    }

    /**
     * Requests for the {@link version2.prototype.Scheduler#Scheduler Scheduler} with the specified unique schedulerID to have its
     * {@link version2#TaskState TaskState} value set to STARTED. Starts a currently stopped Scheduler picking up where it stopped at according to the
     * cache information in the database.
     *
     * @param schedulerID  - targeted Scheduler's ID
     * @param forceUpdate  - forces an immediate update to start the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean StartExistingScheduler(int schedulerID, boolean forceUpdate)
    {
        if(!acceptingRequests || instance == null) {
            return false;
        }

        if(schedulerIDs.get(schedulerID))
        {
            synchronized (startExistingSchedulerRequests) {
                startExistingSchedulerRequests.add(schedulerID);

                if(forceUpdate)
                {
                    UpdateState();
                }
            }
        }
        return true;
    }

    /**
     * Requests for the {@link version2.prototype.Scheduler#Scheduler Scheduler} with the specified specified project name to have its
     * {@link version2#TaskState TaskState} value set to STARTED. Starts a currently stopped Scheduler picking up where it stopped at according to the
     * cache information in the database.
     *
     * @param projectName  - targeted Scheduler's project name
     * @param forceUpdate  - forces an immediate update to start the new scheduler before returning.
     * @return true if successful, false if shutting down
     */
    public static boolean StartExistingScheduler(String projectName, boolean forceUpdate)
    {
        if(!acceptingRequests || instance == null) {
            return false;
        }

        synchronized (startExistingSchedulerRequestsNames) {
            startExistingSchedulerRequestsNames.add(projectName);

            if(forceUpdate)
            {
                UpdateState();
            }
        }
        return true;
    }

    /**
     * Gets the number of {@link version2.prototype.download#GlobalDownloader GlobalDownloader} objects currently created.
     *
     * @return number of {@link version2.prototype.download#GlobalDownloader GlobalDownloader} instances stored
     */
    public static int GetNumberOfGlobalDownloaders()
    {
        if(instance == null)
        {
            return 0;
        }
        int num;
        synchronized (numOfCreatedGDLs) {
            num = numOfCreatedGDLs;
        }
        return num;
    }

    /**
     * Gets the number of {@link version2.prototype.Scheduler#Scheduler Scheduler} objects currently created.
     *
     * @return number of {@link version2.prototype.Scheduler#Scheduler Scheduler} instances stored
     */
    public static int GetNumberOfSchedulerResources()
    {
        if(instance == null)
        {
            return 0;
        }
        int num;
        synchronized (numOfCreatedSchedulers) {
            num = numOfCreatedSchedulers;
        }
        return num;
    }

    /**
     * Returns the list of {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus} objects relevant to all currently known active
     * {@link version2.prototype.Scheduler#Scheduler Scheduler} instances. This information is updated passively by EASTWebManager's background thread if
     * {@link #Start Start(int, int)} was called with a value greater than 0 for msBetweenUpdates, otherwise it is updated actively by calling
     * {@link #UpdateState UpdateState()}.
     *
     * @return list of SchedulerStatus objects for the currently created Scheduler instances
     */
    public static ArrayList<SchedulerStatus> GetSchedulerStatuses()
    {
        if(instance == null)
        {
            return new ArrayList<SchedulerStatus>();
        }
        ArrayList<SchedulerStatus> output = new ArrayList<SchedulerStatus>(0);
        synchronized (schedulerStatuses)
        {
            for(SchedulerStatus status : schedulerStatuses) {
                output.add(status);
            }
        }
        return output;
    }

    /**
     * Gets the {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus} currently known for a {@link version2.prototype.Scheduler#Scheduler Scheduler} with the
     * given unique ID, if it exists. If not, then returns null.
     *
     * @param schedulerID  - unique ID of the target {@link version2.prototype.Scheduler#Scheduler Scheduler} instance
     * @return the currently known {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus} for the target Scheduler if found, otherwise null returned.
     */
    public static SchedulerStatus GetSchedulerStatus(int schedulerID)
    {
        SchedulerStatus status = null;

        if(instance == null)
        {
            return null;
        }
        synchronized (schedulerStatuses)
        {
            for(SchedulerStatus aStatus : schedulerStatuses)
            {
                if(aStatus.SchedulerID == schedulerID) {
                    status = aStatus;
                    break;
                }
            }
        }

        return status;
    }

    /**
     * Gets the {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus} currently known for a {@link version2.prototype.Scheduler#Scheduler Scheduler} with the
     * given unique ID, if it exists. If not, then returns null.
     *
     * @param projectName  - targeted Scheduler's project name
     * @return the currently known {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus} for the target Scheduler if found, otherwise null returned.
     */
    public static SchedulerStatus GetSchedulerStatus(String projectName)
    {
        SchedulerStatus status = null;

        if(instance == null)
        {
            return null;
        }
        synchronized (schedulerStatuses)
        {
            for(SchedulerStatus aStatus : schedulerStatuses)
            {
                if(aStatus.ProjectName.equals(projectName)) {
                    status = aStatus;
                    break;
                }
            }
        }

        if(status != null) {
            return new SchedulerStatus(status);
        } else {
            return null;
        }
    }

    /**
     * Adds a {@link version2#GUIUpdateHandler GUIUpdateHandler} object to the list of registered GUIUpdateHandlers that the EASTWebManager's background
     * thread will run once an update is detected to one of the {@link version2.prototype.Scheduler#SchedulerStatus SchedulerStatus} objects. The thread runs as
     * often as specified when calling the {@link #Start Start(int, int)} method.
     *
     * @param handler  - GUIUpdateHandler object execute when updates to the UI are triggered.
     */
    public static void RegisterGUIUpdateHandler(GUIUpdateHandler handler)
    {
        synchronized (guiHandlers)
        {
            guiHandlers.put(handler, new Boolean(false));
        }
    }

    /**
     * Removes a specified {@link version2#GUIUpdateHandler GUIUpdateHandler} instance from the registered handler list.
     *
     * @param handler  - {@link version2#GUIUpdateHandler GUIUpdateHandler} to unregister
     */
    public static void RemoveGUIUpdateHandler(GUIUpdateHandler handler)
    {
        synchronized (guiHandlers)
        {
            guiHandlers.put(handler, new Boolean(true));
        }
    }

    /**
     * Checks if the specified GUIUpdateHandler instance is registered.
     * @param handler  - instance to search for
     * @return boolean - TRUE if registered, FALSE otherwise
     */
    public static boolean IsRegistered(GUIUpdateHandler handler)
    {
        boolean registered = false;
        synchronized(guiHandlers)
        {
            if(guiHandlers.get(handler) != null) {
                registered = true;
            }
        }
        return registered;
    }

    /**
     * Handles stopping/shutting down the EASTWebManager instance.
     */
    public void stopAndShutdown()
    {
        /*
         * 3) Stop all Schedulers.
         * 4) Stop all GlobalDownloaders.
         * 5) Shutdown thread pool executers.
         * 6) Wait for currently executing ProcessWorkers to finish.
         */

        // Step 3
        synchronized(schedulers) {
            for(Scheduler s : schedulers.values()) {
                s.Stop();
            }
        }

        // Step 4
        synchronized(globalDLs) {
            for(GlobalDownloader gdl : globalDLs.values()) {
                gdl.Stop();
                //            globalDLFutures.get(gdl.ID).cancel(false);
            }
        }

        // Step 5
        synchronized(globalDLExecutor) {
            globalDLExecutor.shutdownNow();
        }
        synchronized(processWorkerExecutor) {
            processWorkerExecutor.shutdownNow();
        }

        // Step 6
        try {
            processWorkerExecutor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e1) {
            ErrorLog.add(configInstance, "Timed out waiting for Process Workers to complete.", new Exception("Timed out waiting for Process Workers to complete."));
        }
    }

    /* (non-Javadoc)
     * @see version2.prototype.EASTWebManagerI#run()
     */
    @Override
    public void run() {
        try {
            // Handle new Scheduler requests
            if(startNewSchedulerRequests.size() > 0)
            {
                List<SchedulerData> tempRequestsList = new ArrayList<SchedulerData>();
                synchronized (startNewSchedulerRequests)
                {
                    while(startNewSchedulerRequests.size() > 0)
                    {
                        tempRequestsList.add(startNewSchedulerRequests.remove(0));
                    }
                }

                while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                {
                    handleStartNewSchedulerRequests(tempRequestsList.remove(0));
                }
            }

            if(loadNewSchedulerRequests.size() > 0)
            {
                List<SchedulerData> tempRequestsList = new ArrayList<SchedulerData>();
                synchronized (loadNewSchedulerRequests)
                {
                    while(loadNewSchedulerRequests.size() > 0)
                    {
                        tempRequestsList.add(loadNewSchedulerRequests.remove(0));
                    }
                }

                while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                {
                    handleLoadNewSchedulerRequests(tempRequestsList.remove(0));
                }
            }

            if(!justCreateNewSchedulers)
            {
                // Handle stop scheduler requests
                if(stopExistingSchedulerRequests.size() > 0)
                {
                    List<Integer> tempRequestsList = new ArrayList<Integer>();
                    synchronized (stopExistingSchedulerRequests)
                    {
                        while(stopExistingSchedulerRequests.size() > 0)
                        {
                            tempRequestsList.add(stopExistingSchedulerRequests.remove(0));
                        }
                    }

                    while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                    {
                        handleStopSchedulerRequests(tempRequestsList.remove(0));
                    }
                }

                if(stopExistingSchedulerRequestsNames.size() > 0)
                {
                    List<String> tempRequestsList = new ArrayList<String>();
                    synchronized (stopExistingSchedulerRequestsNames)
                    {
                        while(stopExistingSchedulerRequestsNames.size() > 0)
                        {
                            tempRequestsList.add(stopExistingSchedulerRequestsNames.remove(0));
                        }
                    }

                    Map<String,Integer> schedulerNamesAndIDs = getClonedSchedulersIDList();

                    int schedulerId;
                    String projectName;
                    while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                    {
                        projectName = tempRequestsList.remove(0);
                        schedulerId = getSchedulerIDFromClonedList(schedulerNamesAndIDs, projectName);
                        if(schedulerId != -1) {
                            handleStopSchedulerRequests(schedulerId);
                        }
                    }
                }

                // Handle delete scheduler requests
                if(deleteExistingSchedulerRequests.size() > 0)
                {
                    List<Integer> tempRequestsList = new ArrayList<Integer>();
                    synchronized (deleteExistingSchedulerRequests)
                    {
                        while(deleteExistingSchedulerRequests.size() > 0)
                        {
                            tempRequestsList.add(deleteExistingSchedulerRequests.remove(0));
                        }
                    }

                    while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                    {
                        handleDeleteSchedulerRequests(tempRequestsList.remove(0));
                    }
                }

                if(deleteExistingSchedulerRequestsNames.size() > 0)
                {
                    List<String> tempRequestsList = new ArrayList<String>();
                    synchronized (deleteExistingSchedulerRequestsNames)
                    {
                        while(deleteExistingSchedulerRequestsNames.size() > 0)
                        {
                            tempRequestsList.add(deleteExistingSchedulerRequestsNames.remove(0));
                        }
                    }

                    Map<String,Integer> schedulerNamesAndIDs = getClonedSchedulersIDList();

                    int schedulerId;
                    String projectName;
                    while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                    {
                        projectName = tempRequestsList.remove(0);
                        schedulerId = getSchedulerIDFromClonedList(schedulerNamesAndIDs, projectName);
                        if(schedulerId != -1) {
                            handleDeleteSchedulerRequests(schedulerId);
                        }
                    }
                }

                // Handle start back up existing Scheduler requests
                if(startExistingSchedulerRequests.size() > 0)
                {
                    List<Integer> tempRequestsList = new ArrayList<Integer>();
                    synchronized (startExistingSchedulerRequests)
                    {
                        while(startExistingSchedulerRequests.size() > 0)
                        {
                            tempRequestsList.add(startExistingSchedulerRequests.remove(0));
                        }
                    }

                    while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                    {
                        handleStartExistingSchedulerRequests(tempRequestsList.remove(0));
                    }
                }

                if(startExistingSchedulerRequestsNames.size() > 0)
                {
                    List<String> tempRequestsList = new ArrayList<String>();
                    synchronized (startExistingSchedulerRequestsNames)
                    {
                        while(startExistingSchedulerRequestsNames.size() > 0)
                        {
                            tempRequestsList.add(startExistingSchedulerRequestsNames.remove(0));
                        }
                    }

                    Map<String,Integer> schedulerNamesAndIDs = getClonedSchedulersIDList();

                    int schedulerId;
                    String projectName;
                    while(tempRequestsList.size() > 0 && !Thread.currentThread().isInterrupted())
                    {
                        projectName = tempRequestsList.remove(0);
                        schedulerId = getSchedulerIDFromClonedList(schedulerNamesAndIDs, projectName);
                        if(schedulerId != -1) {
                            handleStartExistingSchedulerRequests(schedulerId);
                        }
                    }
                }

                // Handle stopping and deleting GlobalDownloaders whose using projects are all stopped.
                // Handle stopping and deleting GlobalDownloaders that don't have any currently existing projects using them.
                // Handle restarting stopped GlobalDownloaders for which a using project has been started.
                if(globalDLs.size() > 0 && !Thread.currentThread().isInterrupted())
                {
                    synchronized (globalDLs)
                    {
                        if(schedulers.size() > 0)
                        {
                            Map<String,TaskState> pluginNamesAndRunningState = getRunningStateForProjectPluginsList();

                            Collection<GlobalDownloader> gdlList = globalDLs.values();
                            for(GlobalDownloader gdl : gdlList)
                            {
                                if(!Thread.currentThread().isInterrupted())
                                {
                                    if(!pluginNamesAndRunningState.containsKey(gdl.pluginName) || pluginNamesAndRunningState.get(gdl.pluginName) == TaskState.STOPPED
                                            || pluginNamesAndRunningState.get(gdl.pluginName) == TaskState.STOPPING)
                                    {
                                        if(gdl.GetRunningState() == TaskState.STARTED || gdl.GetRunningState() == TaskState.RUNNING)
                                        {
                                            System.out.println("Stopping GlobalDownloader '" + gdl.pluginName + "':'" + gdl.metaData.name + "'.");
                                            gdl.Stop();
                                            synchronized(globalDLFutures) {
                                                globalDLFutures.remove(gdl.ID).cancel(false);
                                            }
                                        }
                                    }

                                    else if(gdl.GetRunningState() == TaskState.STOPPED)
                                    {
                                        System.out.println("Starting GlobalDownloader '" + gdl.pluginName + "':'" + gdl.metaData.name + "'.");
                                        gdl.Start();
                                        synchronized(globalDLExecutor) {
                                            if(!globalDLExecutor.isShutdown()) {
                                                ScheduledFuture<?> future = globalDLExecutor.scheduleWithFixedDelay(gdl, 0, 1, TimeUnit.DAYS);

                                                synchronized(globalDLFutures) {
                                                    globalDLFutures.put(gdl.ID, future);
                                                }
                                            }
                                        }
                                    }
                                } else{
                                    break;
                                }
                            }
                        }
                        else
                        {
                            Collection<GlobalDownloader> gdlList = globalDLs.values();
                            for(GlobalDownloader gdl : gdlList)
                            {
                                if(!Thread.currentThread().isInterrupted())
                                {
                                    if(gdl.GetRunningState() == TaskState.STARTED || gdl.GetRunningState() == TaskState.RUNNING)
                                    {
                                        System.out.println("Stopping GlobalDownloader '" + gdl.pluginName + "':'" + gdl.metaData.name + "'.");
                                        gdl.Stop();
                                        synchronized(globalDLFutures) {
                                            globalDLFutures.remove(gdl.ID).cancel(false);
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                        }

                        synchronized (numOfCreatedGDLs) {
                            numOfCreatedGDLs = globalDLs.size();
                        }
                    }
                }

                if(schedulerStatesChanged && !Thread.currentThread().isInterrupted())
                {
                    System.out.println("Running GUI Update Handlers");
                    synchronized (schedulerStatesChanged)
                    {
                        runGUIUpdateHandlers();
                        schedulerStatesChanged = false;
                    }
                    System.out.println("Done with GUI Update Handlers");
                }
            }
        }
        catch (ConcurrentModificationException e) {
            ErrorLog.add(configInstance, "EASTWebManager.run error.", e);
        } catch (Exception e) {
            ErrorLog.add(configInstance, "EASTWebManager.run error.", e);
        }
        synchronized(justCreateNewSchedulers) {
            justCreateNewSchedulers = false;
        }
    }

    /*
     * (non-Javadoc)
     * @see version2.prototype.EASTWebManagerI#NotifyUI(version2.prototype.Scheduler.SchedulerStatus)
     */
    @Override
    public void NotifyUI(SchedulerStatus updatedStatus)
    {
        System.out.println("Updating Scheduler status in EASTWeb Manager.");
        synchronized (schedulerStatesChanged)
        {
            schedulerStatesChanged = true;

            synchronized (schedulerStatuses)
            {
                for(int i=0; i < schedulerStatuses.size(); i++)
                {
                    if(schedulerStatuses.get(i).SchedulerID == updatedStatus.SchedulerID)
                    {
                        schedulerStatuses.set(i, updatedStatus);
                        break;
                    }
                }
            }
        }
        System.out.println("Done updating Scheduler status in EASTWeb Manager.");
    }

    /* (non-Javadoc)
     * @see version2.prototype.EASTWebManagerI#StartGlobalDownloader(version2.prototype.download.DownloadFactory)
     */
    @Override
    public LocalDownloader StartGlobalDownloader(DownloadFactory dlFactory)
    {
        synchronized (globalDLs)
        {
            int id = getLowestAvailableGlobalDLID();
            if(IsIDValid(id, globalDLIDs))
            {
                GlobalDownloader gdl = null;
                LocalDownloader localDl;
                int currentGDLIdx = -1;
                String tempDownloadFactoryClassName;
                for(int i=0; i < globalDLs.size(); i++)
                {
                    tempDownloadFactoryClassName = globalDLs.get(i).metaData.downloadFactoryClassName;
                    if(tempDownloadFactoryClassName.equals(dlFactory.downloadMetaData.downloadFactoryClassName)
                            && globalDLs.get(i).GetStartDate().equals(dlFactory.startDate)
                            && globalDLs.get(i).GetEndDate().equals(dlFactory.endDate))
                    {
                        currentGDLIdx = i;
                        break;
                    }
                }

                DownloaderFactory factory = null;
                try {
                    factory = dlFactory.CreateDownloaderFactory(dlFactory.CreateListDatesFiles());
                } catch (IOException e) {
                    ErrorLog.add(configInstance, "EASTWebManager.StartGlobalDownloader error while creating DownloadFactory or ListDatesFiles.", e);
                } catch (Exception e) {
                    ErrorLog.add(configInstance, "EASTWebManager.StartGlobalDownloader error while creating DownloadFactory or ListDatesFiles.", e);
                }

                if(factory == null) {
                    return null;
                }

                if(currentGDLIdx >= 0)
                {
                    releaseGlobalDLID(id);
                    gdl = globalDLs.get(currentGDLIdx);
                    //                    gdl.SetStartDate(dlFactory.startDate); // These 2 lines are replacing the following 3
                    //                    gdl.SetEndDate(dlFactory.endDate);
                    if(gdl.GetStartDate().isAfter(dlFactory.startDate)) {
                        gdl.SetStartDate(dlFactory.startDate);
                    }
                }
                else {
                    System.out.println("Creating new GlobalDownloader for '" + dlFactory.downloadMetaData.name + "' for plugin '" + dlFactory.downloadMetaData.Title + "'.");
                    gdl = factory.CreateGlobalDownloader(id);
                    if(gdl == null) {
                        return null;
                    }
                    System.out.println("GlobalDownloader for '" + dlFactory.downloadMetaData.name + "' for plugin '" + dlFactory.downloadMetaData.Title + "' created.");

                    globalDLs.put(id, gdl);
                    synchronized(globalDLExecutor) {
                        if(!globalDLExecutor.isShutdown()) {
                            ScheduledFuture<?> future = globalDLExecutor.scheduleWithFixedDelay(gdl, 0, 1, TimeUnit.DAYS);

                            synchronized(globalDLFutures) {
                                globalDLFutures.put(id, future);
                            }
                        }
                    }
                }

                synchronized (numOfCreatedGDLs) {
                    numOfCreatedGDLs = globalDLs.size();
                }

                if(gdl.GetRunningState() == TaskState.STARTED || gdl.GetRunningState() == TaskState.RUNNING) {
                    System.out.println("GlobalDownloader already running for '" + dlFactory.downloadMetaData.name + "' for plugin '" + dlFactory.downloadMetaData.Title + "'.");
                } else if(currentGDLIdx >= 0) {
                    System.out.println("Restarting GlobalDownloader for '" + dlFactory.downloadMetaData.name + "' for plugin '" + dlFactory.downloadMetaData.Title + "'.");
                    gdl.Start();
                    synchronized(globalDLExecutor) {
                        if(!globalDLExecutor.isShutdown()) {
                            ScheduledFuture<?> future = globalDLExecutor.scheduleWithFixedDelay(gdl, 0, 1, TimeUnit.DAYS);

                            synchronized(globalDLFutures) {
                                globalDLFutures.put(gdl.ID, future);
                            }
                        }
                    }
                }
                else {
                    System.out.println("Starting GlobalDownloader for '" + dlFactory.downloadMetaData.name + "' for plugin '" + dlFactory.downloadMetaData.Title + "'.");
                    gdl.Start();
                }

                System.out.println("Creating new LocalDownloader for '" + dlFactory.downloadMetaData.name + "' for plugin '" + dlFactory.downloadMetaData.Title + "'.");
                localDl = factory.CreateLocalDownloader(gdl);
                System.out.println("LocalDownloader for '" + dlFactory.downloadMetaData.name + "' for plugin '" + dlFactory.downloadMetaData.Title + "' created.");
                return localDl;
            }
            else
            {
                return null;
            }
        }
    }

    /* (non-Javadoc)
     * @see version2.prototype.EASTWebManagerI#StartNewProcessWorker(version2.prototype.ProcessWorker)
     */
    @Override
    public Future<ProcessWorkerReturn> StartNewProcessWorker(Callable<ProcessWorkerReturn> worker)
    {
        if(!processWorkerExecutor.isShutdown()) {
            return processWorkerExecutor.submit(worker);
        } else {
            return null;
        }
    }

    @Override
    public DatabaseConnection GetConnection() {
        return connectionPool.getConnection();
    }

    static protected boolean testDatabaseConnection(Config configInstance)
    {
        boolean successful = false;
        DatabaseConnection con = null;
        Statement stmt = null;
        try {
            con = DatabaseConnector.getConnection(configInstance);

            if(con != null) {
                stmt = con.createStatement();
                successful = stmt.execute("SELECT 1;");
            }
        } catch (Exception e) {
            ErrorLog.add(configInstance, "Could not establish connection with database.", e);
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) { /* do nothing */ }
            if(con != null) {
                con.close();
            }
        }
        return successful;
        //            return con.isValid(0);      // org.postgresql.util.PSQLException: Method org.postgresql.jdbc4.Jdbc4Connection.isValid(int) is not yet implemented.
    }

    /**
     * Empty instance just to allow usage of private classes.
     */
    protected EASTWebManager()
    {
        this(1, 1, 1000);
    }

    protected EASTWebManager(int numOfGlobalDLResourses, int numOfProcessWorkerResourses, int msBetweenUpdates)
    {
        justCreateNewSchedulers = false;
        this.msBetweenUpdates = msBetweenUpdates;
        configInstance = Config.getInstance();
        //        connectionPool = new C3P0ConnectionPool(configInstance);
        connectionPool = null;

        // Setup request lists
        startNewSchedulerRequests = Collections.synchronizedList(new ArrayList<SchedulerData>(0));
        loadNewSchedulerRequests = Collections.synchronizedList(new ArrayList<SchedulerData>(0));
        stopExistingSchedulerRequests = Collections.synchronizedList(new ArrayList<Integer>(0));
        stopExistingSchedulerRequestsNames = Collections.synchronizedList(new ArrayList<String>(0));
        deleteExistingSchedulerRequests = Collections.synchronizedList(new ArrayList<Integer>(0));
        deleteExistingSchedulerRequestsNames = Collections.synchronizedList(new ArrayList<String>(0));
        startExistingSchedulerRequests = Collections.synchronizedList(new ArrayList<Integer>(0));
        startExistingSchedulerRequestsNames = Collections.synchronizedList(new ArrayList<String>(0));

        // Setup for handling executing GlobalDownloaders
        globalDLs = new HashMap<Integer, GlobalDownloader>();
        globalDLExecutor = new ScheduledThreadPoolExecutor(numOfGlobalDLResourses, new NamedThreadFactory(configInstance, "GlobalDownloader", false));
        globalDLExecutor.setRemoveOnCancelPolicy(true);
        globalDLFutures = new HashMap<Integer, ScheduledFuture<?>>();

        // Setup for handling executing Schedulers
        schedulers = new HashMap<Integer, Scheduler>();

        // Setup for handling executing ProcessWorkers
        //        processWorkerExecutor = Executors.newFixedThreadPool(numOfProcessWorkerResourses, new NamedThreadFactory(configInstance, "ProcessWorker", false));
        processWorkerExecutor = new ThreadPoolExecutor(numOfProcessWorkerResourses, numOfProcessWorkerResourses, 1l, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory(configInstance, "ProcessWorker", false));
        ((ThreadPoolExecutor)processWorkerExecutor).allowCoreThreadTimeOut(true);
    }

    protected void handleStartNewSchedulerRequests(SchedulerData data)
    {
        System.out.println("Handling start request of a new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "'.");
        if(!handleNewSchedulerRequest(data, TaskState.RUNNING)) {
            synchronized(startNewSchedulerRequests) {
                startNewSchedulerRequests.add(data);
            }
            System.out.println("Start request of new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "' failed. Could not get an ID for new Scheduler.");
        } else {
            System.out.println("Start request of a new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "' handled.");
        }
    }

    protected void handleLoadNewSchedulerRequests(SchedulerData data)
    {
        System.out.println("Handling load request of a new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "'.");
        if(!handleNewSchedulerRequest(data, TaskState.STOPPED)) {
            synchronized(loadNewSchedulerRequests) {
                loadNewSchedulerRequests.add(data);
            }
            System.out.println("Load request of new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "' failed. Could not get an ID for new Scheduler.");
        } else {
            System.out.println("Load request of a new Scheduler for project '" + data.projectInfoFile.GetProjectName() + "' handled.");
        }
    }

    protected boolean handleNewSchedulerRequest(SchedulerData data, TaskState initState)
    {
        boolean success = false;
        int id = getLowestAvailableSchedulerID();
        synchronized(schedulerIDs) {
            success = IsIDValid(id, schedulerIDs);
        }
        if(success)
        {
            Scheduler scheduler = null;
            scheduler = new Scheduler(data, id, initState, this, configInstance);
            synchronized(schedulerStatuses) {
                schedulerStatuses.add(scheduler.GetSchedulerStatus());
            }
            synchronized (schedulers)
            {
                schedulers.put(id, scheduler);

                synchronized (numOfCreatedSchedulers) {
                    numOfCreatedSchedulers = schedulers.size();
                }
            }
        }
        return success;
    }

    protected void handleStopSchedulerRequests(int schedulerID)
    {
        if(schedulers.size() > schedulerID)
        {
            System.out.println("Handling stop request of the Scheduler for project '" + schedulers.get(schedulerID).projectInfoFile.GetProjectName() + "'.");
            synchronized (schedulers)
            {
                if(schedulers.get(schedulerID) != null) {
                    schedulers.get(schedulerID).Stop();
                }
            }
            System.out.println("Stop request of the Scheduler for project '" + schedulers.get(schedulerID).projectInfoFile.GetProjectName() + "' handled.");
        }
    }

    protected void handleDeleteSchedulerRequests(int schedulerID)
    {
        /*
            protected static Integer numOfCreatedSchedulers = 0;
            protected static List<SchedulerStatus> schedulerStatuses = new ArrayList<SchedulerStatus>(1);
            protected static BitSet schedulerIDs = new BitSet(100000);
         */

        Scheduler scheduler = schedulers.get(schedulerID);
        String projectName = schedulers.get(schedulerID).projectInfoFile.GetProjectName();

        if(schedulers.size() > schedulerID)
        {
            System.out.println("Handling delete request of the project '" + projectName + "'.");
            synchronized (schedulers)
            {
                schedulers.remove(schedulerID);
                releaseSchedulerID(schedulerID);
            }
        } else {
            return;
        }

        synchronized (numOfCreatedSchedulers) {
            numOfCreatedSchedulers = schedulers.size();
        }

        scheduler.Delete();

        synchronized (schedulerStatuses)
        {
            for(int i=0; i < schedulerStatuses.size(); i++)
            {
                if(schedulerStatuses.get(i) != null && schedulerStatuses.get(i).SchedulerID == schedulerID) {
                    schedulerStatuses.remove(i);
                    break;
                }
            }
        }

        System.out.println("Delete request of the project '" + projectName + "' handled.");
    }

    protected void handleStartExistingSchedulerRequests(int schedulerID)
    {
        if(schedulers.size() > schedulerID)
        {
            System.out.println("Handling request to start back up the Scheduler for project '" + schedulers.get(schedulerID).projectInfoFile.GetProjectName() + "'.");
            synchronized (schedulers)
            {
                schedulers.get(schedulerID).Start();
            }
            System.out.println("Restart request of the Scheduler for project '" + schedulers.get(schedulerID).projectInfoFile.GetProjectName() + "' handled.");
        }
    }

    protected void runGUIUpdateHandlers()
    {
        synchronized (guiHandlers)
        {
            ArrayList<GUIUpdateHandler> flaggedForRemoval = new ArrayList<GUIUpdateHandler>();
            Set<GUIUpdateHandler> handlers = guiHandlers.keySet();
            for(GUIUpdateHandler handler : handlers)
            {
                if(!Thread.currentThread().isInterrupted())
                {
                    handler.run();
                    if(guiHandlers.get(handler)) {
                        flaggedForRemoval.add(handler);
                    }
                } else {
                    break;
                }
            }

            for(GUIUpdateHandler handler : flaggedForRemoval)
            {
                guiHandlers.remove(handler);
            }
        }
    }

    protected int getLowestAvailableSchedulerID()
    {
        int id = -1;

        synchronized (schedulerIDs)
        {
            id = schedulerIDs.nextClearBit(0);

            if(IsIDValid(id, schedulerIDs))
            {
                schedulerIDs.set(id);
            }
            else
            {
                id = -1;
            }
        }

        return id;
    }

    protected void releaseSchedulerID(int id)
    {
        synchronized (schedulerIDs)
        {
            if(IsIDValid(id, schedulerIDs))
            {
                schedulerIDs.clear(id);
            }
        }
    }

    protected int getLowestAvailableGlobalDLID()
    {
        int id = -1;

        synchronized (globalDLIDs)
        {
            id = globalDLIDs.nextClearBit(0);

            if(IsIDValid(id, globalDLIDs))
            {
                globalDLIDs.set(id);
            }
            else
            {
                id = -1;
            }
        }

        return id;
    }

    protected void releaseGlobalDLID(int id)
    {
        synchronized (globalDLIDs)
        {
            if(IsIDValid(id, globalDLIDs))
            {
                globalDLIDs.clear(id);
            }
        }
    }

    protected boolean IsIDValid(int id, BitSet set)
    {
        if((id >= 0) && (id < set.size())) {
            return true;
        } else {
            return false;
        }
    }

    protected Map<String,Integer> getClonedSchedulersIDList() {
        Map<String,Integer> schedulerNameAndID = new HashMap<String,Integer>(0);
        synchronized(schedulers)
        {
            Collection<Scheduler> schedulerList = schedulers.values();
            for(Scheduler scheduler : schedulerList)
            {
                schedulerNameAndID.put(scheduler.projectInfoFile.GetProjectName(), scheduler.GetID());
            }
        }
        return schedulerNameAndID;
    }

    protected Map<String,TaskState> getRunningStateForProjectPluginsList() {
        Map<String,TaskState> pluginNamesAndRunningState = new HashMap<String,TaskState>(0);
        synchronized(schedulers)
        {
            Collection<Scheduler> schedulerList = schedulers.values();
            for(Scheduler scheduler : schedulerList)
            {
                for(ProjectInfoPlugin pluginInfo : scheduler.projectInfoFile.GetPlugins())
                {
                    if(pluginNamesAndRunningState.containsKey(pluginInfo.GetName()))
                    {
                        if(pluginNamesAndRunningState.get(pluginInfo.GetName()) == TaskState.STOPPED || pluginNamesAndRunningState.get(pluginInfo.GetName()) == TaskState.STOPPING)
                        {
                            pluginNamesAndRunningState.put(pluginInfo.GetName(), scheduler.GetState());
                        }
                    }
                    else
                    {
                        pluginNamesAndRunningState.put(pluginInfo.GetName(), scheduler.GetState());
                    }
                }
            }
        }
        return pluginNamesAndRunningState;
    }

    protected int getSchedulerIDFromClonedList(Map<String, Integer> schedulerNamesAndIDs, String projectName) {
        int schedulerId;
        schedulerId = -1;
        Set<String> projectNames = schedulerNamesAndIDs.keySet();
        for(String name : projectNames)
        {
            if(name.equals(projectName))
            {
                schedulerId = schedulerNamesAndIDs.get(name);
                break;
            }
        }
        return schedulerId;
    }
}
