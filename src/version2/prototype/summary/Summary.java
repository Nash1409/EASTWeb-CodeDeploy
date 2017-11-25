package version2.prototype.summary;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.Process;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.Scheduler.Scheduler;
import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;
import version2.prototype.summary.temporal.TemporalSummaryRasterFileStore;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseCache;

/**
 * The custom Summary framework, Process extending class. Manages SummaryWorker objects.
 *
 * @author michael.devos
 *
 */
public class Summary extends Process {
    private Map<Integer, TemporalSummaryRasterFileStore> fileStores;

    /**
     * Creates a Summary object with the defined initial TaskState, owned by the given Scheduler, and acquiring its input from the specified
     * process, inputProcessName.
     * @param configInstance  - Config reference to use and pass on
     * @param projectMetaData  - the current project's information
     * @param pluginInfo  - the current plugin's general information
     * @param pluginMetaData  - the current plugin's xml data mapped
     * @param scheduler  - reference to the controlling Scheduler object
     * @param inputCache
     * @param outputCache
     */
    public Summary(Config configInstance, ProjectInfoFile projectMetaData, ProjectInfoPlugin pluginInfo, PluginMetaData pluginMetaData, Scheduler scheduler,
            DatabaseCache inputCache, DatabaseCache outputCache)
    {
        super(configInstance, ProcessName.SUMMARY, projectMetaData, pluginInfo, pluginMetaData, scheduler, outputCache);
        inputCache.addObserver(this);

        fileStores = new HashMap<Integer, TemporalSummaryRasterFileStore>();
        String temporalSummaryCompositionStrategyClassName;
        TemporalSummaryRasterFileStore fileStore;
        Class<?> strategyClass;
        Constructor<?> ctorStrategy = null;
        for(ProjectInfoSummary summary : projectMetaData.GetSummaries())
        {
            fileStore = null;
            temporalSummaryCompositionStrategyClassName = summary.GetTemporalSummaryCompositionStrategyClassName();
            if(temporalSummaryCompositionStrategyClassName != null && !temporalSummaryCompositionStrategyClassName.isEmpty())
            {
                try {
                    strategyClass = Class.forName("version2.prototype.summary.temporal.CompositionStrategies." + temporalSummaryCompositionStrategyClassName);
                    ctorStrategy = strategyClass.getConstructor();
                    fileStore = new TemporalSummaryRasterFileStore((TemporalSummaryCompositionStrategy)ctorStrategy.newInstance());
                } catch(ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    ErrorLog.add(processName, scheduler, "Problem instantiating temporal filestore.", e);
                }
            }
            fileStores.put(summary.GetID(), fileStore);
        }
    }

    @Override
    public void process(ArrayList<DataFileMetaData> cachedFiles) {
        scheduler.StartNewProcessWorker(new SummaryWorker(configInstance, this, projectInfoFile, pluginInfo, pluginMetaData, cachedFiles, outputCache, fileStores));
    }
}