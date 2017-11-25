package version2.prototype.summary;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.Process;
import version2.prototype.ProcessWorker;
import version2.prototype.ProcessWorkerReturn;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.summary.temporal.MergeStrategy;
import version2.prototype.summary.temporal.TemporalSummaryCalculator;
import version2.prototype.summary.temporal.TemporalSummaryRasterFileStore;
import version2.prototype.summary.zonal.SummariesCollection;
import version2.prototype.summary.zonal.ZonalSummaryCalculator;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseCache;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.FileSystem;
import version2.prototype.util.IndicesFileMetaData;
import version2.prototype.util.Schemas;

/**
 * @author michael.devos
 *
 */
public class SummaryWorker extends ProcessWorker {
    private final Config configInstance;
    private final Map<Integer, TemporalSummaryRasterFileStore> fileStores;

    /**
     * @param configInstance
     * @param process
     * @param projectInfoFile
     * @param pluginInfo
     * @param pluginMetaData
     * @param cachedFiles
     * @param outputCache
     * @param fileStores
     */
    public SummaryWorker(Config configInstance, Process process, ProjectInfoFile projectInfoFile, ProjectInfoPlugin pluginInfo, PluginMetaData pluginMetaData, ArrayList<DataFileMetaData> cachedFiles,
            DatabaseCache outputCache, Map<Integer, TemporalSummaryRasterFileStore> fileStores)
    {
        super(configInstance, "SummaryWorker", process, projectInfoFile, pluginInfo, pluginMetaData, cachedFiles, outputCache);
        this.configInstance = configInstance;
        this.fileStores = fileStores;
    }

    @Override
    public ProcessWorkerReturn process() {
        DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        Connection con1 = RDS.getCentralDBconnection();
        if(con == null) {
            return null;
        }
        if(con1 == null) {
            return null;
        }
        Map<Integer, ArrayList<DataFileMetaData>> summaryInputMap = new HashMap<Integer, ArrayList<DataFileMetaData>>();
        Map<Integer, ArrayList<DataFileMetaData>> tempFilesMap = new HashMap<Integer, ArrayList<DataFileMetaData>>();
        ArrayList<DataFileMetaData> outputFiles = new ArrayList<DataFileMetaData>(1);
        ArrayList<DataFileMetaData> tempFiles;
        DataFileMetaData tempFile;
        IndicesFileMetaData cachedFileData = null;

        Class<?> strategyClass;
        Constructor<?> ctorStrategy;
        MergeStrategy mergeStrategy = null;
        try {
            strategyClass = Class.forName("version2.prototype.summary.temporal.MergeStrategies." + pluginMetaData.Summary.mergeStrategyClass);
            ctorStrategy = strategyClass.getConstructor();
            mergeStrategy = (MergeStrategy)ctorStrategy.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            ErrorLog.add(process,  "Problem creating MergeStrategy.", e);
        }

        // Setup initial input mappings
        for(ProjectInfoSummary summary : projectInfoFile.GetSummaries())
        {
            summaryInputMap.put(summary.GetID(), cachedFiles);
        }

        for(ProjectInfoSummary summary: projectInfoFile.GetSummaries())
        {
            try{
                // Check if doing temporal summarization
                if(fileStores.get(summary.GetID()) != null && fileStores.get(summary.GetID()).compStrategy.maxNumberOfDaysInComposite() != pluginMetaData.DaysPerInputData)
                {
                    tempFiles = new ArrayList<DataFileMetaData>();
                    for(DataFileMetaData cachedFile : summaryInputMap.get(summary.GetID()))
                    {
                        if(Thread.currentThread().isInterrupted()) {
                            return null;
                        }

                        cachedFileData = cachedFile.ReadMetaDataForSummary();
                        TemporalSummaryCalculator temporalSummaryCal = new TemporalSummaryCalculator(
                                configInstance,                         // configInstance
                                con1,                                    //**** change to RDS connection DatabaseConnection con
                                process,                                // process
                                projectInfoFile,                        // projectInfoFile
                                pluginInfo.GetName(),                   // pluginName
                                cachedFileData,                         // inputFile
                                pluginMetaData.DaysPerInputData,        // daysPerInputData
                                fileStores.get(summary.GetID()),        // TemporalSummaryRasterFileStore
                                null,                                   // InterpolateStrategy
                                mergeStrategy,
                                summary
                                );
                        tempFile = temporalSummaryCal.calculate();
                        if(tempFile != null) {
                            tempFiles.add(tempFile);
                        }
                    }
                    summaryInputMap.put(summary.GetID(), tempFiles);
                    tempFilesMap.put(summary.GetID(), tempFiles);
                }
            }catch(Exception e) {
                if(cachedFileData != null) {
                    ErrorLog.add(process, "Problem during temporal summary calculation for summary " + summary.toString() + ", date {day of year=" + cachedFileData.day + ", year=" + cachedFileData.year + "}.", e);
                } else {
                    ErrorLog.add(process, "Problem during temporal summary calculation for summary " + summary.toString() + ".", e);
                }
            }
        }

        File outputFile;
        for(ProjectInfoSummary summary: projectInfoFile.GetSummaries())
        {
            for(DataFileMetaData cachedFile : summaryInputMap.get(summary.GetID()))
            {
                if(Thread.currentThread().isInterrupted()) {
                    con.close();
                    try {
                        con1.close();
                    } catch (SQLException e) {e.printStackTrace();}
                    return null;
                }

                cachedFileData = cachedFile.ReadMetaDataForSummary();
                try{
                    outputFile = new File(FileSystem.GetProcessOutputDirectoryPath(projectInfoFile.GetFullPath(),
                            pluginInfo.GetName(), ProcessName.SUMMARY) + String.format("%s/%s/%04d/%03d.csv", cachedFileData.indexNm, "Summary " + summary.GetID(), cachedFileData.year, cachedFileData.day));
                    ZonalSummaryCalculator zonalSummaryCal = new ZonalSummaryCalculator(
                            con,
                            process,
                            configInstance.getGlobalSchema(),
                            projectInfoFile.GetFullPath(),
                            pluginInfo.GetName(),                   // pluginName
                            pluginMetaData.DaysPerInputData,        // daysPerInputData
                            cachedFileData,                         // inputFile
                            outputFile,                             // outTableFile
                            new SummariesCollection(Config.getInstance().getSummaryCalculations()),     // summariesCollection
                            summary,
                            pluginMetaData.NoDataValue,
                            fileStores.get(summary.GetID()),        // fileStore
                            outputCache);
                    zonalSummaryCal.calculate();

                    if(process.GetClearIntermediateFilesFlag())
                    {
                        String newFilePath = FileSystem.GetProcessWorkerTempDirectoryPath(projectInfoFile.GetFullPath(), pluginInfo.GetName(), ProcessName.SUMMARY) +
                                String.format("%04d%03d.tif",
                                        cachedFileData.year,
                                        cachedFileData.day
                                        );
                        File intermediateFile = new File(newFilePath);
                        if(intermediateFile.exists()) {
                            intermediateFile.delete();
                        }
                    }

                    outputFiles.add(new DataFileMetaData(outputFile.getCanonicalPath(), cachedFileData.dateGroupID, cachedFileData.year, cachedFileData.day, cachedFileData.indexNm));
                }catch(Exception e) {
                    if(cachedFileData != null) {
                        ErrorLog.add(process, "Problem during zonal summary calculation for summary " + summary.toString() + ", date {day of year=" + cachedFileData.day + ", year=" + cachedFileData.year + "}.", e);
                    } else {
                        ErrorLog.add(process, "Problem during zonal summary calculation for summary " + summary.toString() + ".", e);
                    }
                }
            }
        }

        if(process.GetClearIntermediateFilesFlag() && tempFilesMap.size() > 0)
        {
            Iterator<Integer> tempIt = tempFilesMap.keySet().iterator();
            Integer key;
            ArrayList<DataFileMetaData> intermediateFiles;
            File dFile;
            while(tempIt.hasNext())
            {
                key = tempIt.next();
                intermediateFiles = tempFilesMap.get(key);
                for(DataFileMetaData dfmd : intermediateFiles)
                {
                    dFile = new File(dfmd.ReadMetaDataForSummary().dataFilePath);
                    if(dFile.exists()) {
                        dFile.delete();
                    }
                }
            }
        }

        con.close();
        try {
            if (con1 != null){
                con1.close();
            }
        } catch (SQLException e) {e.printStackTrace();}
        return new ProcessWorkerReturn(outputFiles);
    }

    @Override
    public boolean verifyResults() {
        String url = "jdbc:postgresql://localhost:" + configInstance.getPort() + "/" + configInstance.getDatabaseName();
        Connection con = null;
        boolean allGood = true;
        try {
            con = DriverManager.getConnection(url, configInstance.getDatabaseUsername(), configInstance.getDatabasePassword());
            for(ProjectInfoSummary summary: projectInfoFile.GetSummaries())
            {
                if(!UpdateForMissingSummaries.findMissingSummaries(con, projectInfoFile.GetWorkingDir() + "\\projects", configInstance.getGlobalSchema(),
                        configInstance.getPort(), Schemas.getSchemaName(projectInfoFile.GetParentProjectName(), pluginInfo.GetName()),
                        projectInfoFile.GetProjectName(), projectInfoFile.GetStartDate(), "Summary " + summary.GetID(), pluginMetaData.DaysPerInputData, fileStores.get(summary.GetID()).compStrategy.maxNumberOfDaysInComposite(),
                        fileStores.get(summary.GetID()).compStrategy.getClass().getSimpleName(), pluginMetaData.CompositesContinueIntoNextYear)) {
                    allGood = false;
                }
            }
            con.close();
        } catch (SQLException|NumberFormatException|ClassNotFoundException|FileNotFoundException e) {
            ErrorLog.add(process, "Problem identifying missing summaries", e);

            if(con != null) {
                try {
                    con.close();
                } catch (SQLException e1) {
                    ErrorLog.add(process, "Problem closing connection", e);
                }
            }
        }
        return allGood;
    }
}
