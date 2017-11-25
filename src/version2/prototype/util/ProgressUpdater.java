/**
 *
 */
package version2.prototype.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.PluginMetaDataCollection;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.download.ListDatesFiles;
import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;

/**
 * @author michael.devos
 *
 */
public class ProgressUpdater {
    private final Config configInstance;
    private final ProjectInfoFile projectMetaData;
    private final PluginMetaDataCollection pluginMetaDataCollection;

    private TreeMap<String, TreeMap<String, Integer>> downloadExpectedFiles;
    private TreeMap<String, Integer> processorExpectedNumOfOutputs;
    private TreeMap<String, Integer> indicesExpectedNumOfOutputs;
    private TreeMap<String, TreeMap<Integer, Integer>> summaryExpectedNumOfOutputs;

    /**
     * Creates a ProgressUpdater object bound to the given Scheduler/project. This object will then handle calculating the progress values for each Process.
     *
     * @param configInstance  - the Config instance to use
     * @param projectMetaData  - project metadata
     * @param pluginMetaDataCollection  - reference to PluginMetaDataCollection instance being used by the referenced Scheduler
     */
    public ProgressUpdater(Config configInstance, ProjectInfoFile projectMetaData, PluginMetaDataCollection pluginMetaDataCollection)
    {
        this.configInstance = configInstance;
        this.projectMetaData = projectMetaData;
        this.pluginMetaDataCollection = pluginMetaDataCollection;

        downloadExpectedFiles = new TreeMap<String, TreeMap<String, Integer>>();
        TreeMap<String, Integer> downloadExpectedFilesTemp;
        processorExpectedNumOfOutputs = new TreeMap<String, Integer>();
        indicesExpectedNumOfOutputs = new TreeMap<String, Integer>();
        summaryExpectedNumOfOutputs = new TreeMap<String, TreeMap<Integer, Integer>>();
        TreeMap<Integer, Integer> summaryExpectedNumOfPluginOutputs;
        String pluginName;
        for(ProjectInfoPlugin plugin : projectMetaData.GetPlugins())
        {
            pluginName = plugin.GetName();

            // Setup Download progresses
            downloadExpectedFilesTemp = new TreeMap<String, Integer>();
            downloadExpectedFilesTemp.put("data", null);
            for(String dataName : pluginMetaDataCollection.pluginMetaDataMap.get(pluginName).ExtraDownloadFiles)
            {
                downloadExpectedFilesTemp.put(dataName.toLowerCase(), null);
            }
            downloadExpectedFiles.put(pluginName, downloadExpectedFilesTemp);

            // Setup Processor progresses
            processorExpectedNumOfOutputs.put(pluginName, null);

            // Setup Indices progresses
            indicesExpectedNumOfOutputs.put(pluginName, null);

            // Setup Summary progresses
            summaryExpectedNumOfPluginOutputs = new TreeMap<Integer, Integer>();
            for(ProjectInfoSummary summary : projectMetaData.GetSummaries())
            {
                summaryExpectedNumOfPluginOutputs.put(summary.GetID(), null);
            }
            summaryExpectedNumOfOutputs.put(pluginName, summaryExpectedNumOfPluginOutputs);
        }
    }

    /**
     * Calculates the current progress percentage for the Download process.
     *
     * @param dataName  - the name of the file type being downloaded (e.g. "Data" or "Qc")
     * @param pluginName  - the plugin title gotten from the plugin metadata to calculate progress in relation to
     * @param startDate
     * @param modisTileNames
     * @param stmt  - Statement object to reuse
     * @return double  - progress percentage of the download process in relation to the local downloader downloading the files named by the given dataName and for the specified pluginName
     * @throws SQLException
     */
    public double GetCurrentDownloadProgress(String dataName, String pluginName, LocalDate startDate, ArrayList<String> modisTileNames, Statement stmt) throws SQLException
    {
        Connection con = RDS.getCentralDBconnection();
        Statement stmt1 = con.createStatement();
        double progress = 0;
        String mSchemaName = Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginName);
        PluginMetaData pluginMetaData = pluginMetaDataCollection.pluginMetaDataMap.get(pluginName);
        int currentCount = calculateDownloadCurrentCount(mSchemaName, dataName, stmt, projectMetaData.GetStartDate(), projectMetaData.GetEndDate());
        int expectedCount = getStoredDownloadExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginName, dataName, stmt1);
        int maxExpectedCount = calculateMaxDownloadExpectedCount(pluginMetaData, projectMetaData.GetStartDate(), projectMetaData.GetEndDate(), modisTileNames);

        if(expectedCount > 0 && currentCount > 0)
        {
            progress = (new Double(currentCount) / new Double(expectedCount)) * 100;
        }

        if((progress > 100 || progress < 0) && (currentCount > maxExpectedCount)) {
            ErrorLog.add(configInstance, "Invalid Download progress (current = " + currentCount + ", expected = " + expectedCount + ") of " + progress
                    + " for plugin '" + pluginName + "' and data '" + dataName + "'.", new Exception("Invalid Download progress."));
        }
        con.close();

        return progress;
    }

    /**
     * Calculates the current progress percentage for the Processor process.
     *
     * @param pluginName  - the plugin title gotten from the plugin metadata to calculate progress in relation to
     * @param stmt  - Statement object to reuse
     * @return double  - progress percentage of the processor process for the specified pluginName
     * @throws SQLException
     */
    public double GetCurrentProcessorProgress(String pluginName, Statement stmt) throws SQLException
    {
        double progress = 0;
        PluginMetaData pluginMetaData = pluginMetaDataCollection.pluginMetaDataMap.get(pluginName);
        ProjectInfoPlugin pluginInfo = null;
        for(ProjectInfoPlugin plugin : projectMetaData.GetPlugins()) {
            if(plugin.GetName().equals(pluginName)) {
                pluginInfo = plugin;
                break;
            }
        }
        if(pluginInfo == null) {
            ErrorLog.add(configInstance, "Mising project plugin info for plugin '" + pluginName + "'.", new Exception("Plugin '" + pluginName + "' info could not be found in project "
                    + "metadata."));
        }
        String mSchemaName = Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginName);
        int currentCount = calculateProcessorCurrentCount(mSchemaName, stmt,projectMetaData.GetStartDate(),projectMetaData.GetEndDate());
        int expectedCount = calculateProcessorExpectedCount(pluginMetaData, pluginInfo, mSchemaName, stmt,projectMetaData.GetStartDate(),projectMetaData.GetEndDate());

        if(expectedCount > 0 && currentCount > 0)
        {
            progress = (new Double(currentCount) / new Double(expectedCount)) * 100;
        }

        if(progress > 100 || progress < 0) {
            ErrorLog.add(configInstance, "Invalid Processor progress (current = " + currentCount + ", expected = " + expectedCount + ") of " + progress
                    + " for plugin " + pluginName + ".", new Exception("Invalid Processor progress."));
        }

        return progress;
    }

    /**
     * Calculates the current progress percentage for the Indices process.
     *
     * @param pluginName  - the plugin title gotten from the plugin metadata to calculate progress in relation to
     * @param stmt  - Statement object to reuse
     * @return double  - progress percentage of the processor process for the specified pluginName
     * @throws SQLException
     */
    public double GetCurrentIndicesProgress(String pluginName, Statement stmt) throws SQLException
    {
        double progress = 0;
        PluginMetaData pluginMetaData = pluginMetaDataCollection.pluginMetaDataMap.get(pluginName);
        String mSchemaName = Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginName);
        int currentCount = calculateIndicesCurrentCount(mSchemaName, stmt,projectMetaData.GetStartDate(),projectMetaData.GetEndDate());
        int expectedCount = calculateIndicesExpectedCount(Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginName), pluginMetaData, pluginName, stmt,projectMetaData.GetStartDate(),projectMetaData.GetEndDate());

        if(expectedCount > 0 && currentCount > 0)
        {
            progress = (new Double(currentCount) / new Double(expectedCount)) * 100;
        }

        if(progress > 100 || progress < 0) {
            ErrorLog.add(configInstance, "Invalid Indices progress (current = " + currentCount + ", expected = " + expectedCount + ") of " + progress
                    + " for plugin " + pluginName + ".", new Exception("Invalid Indices progress."));
        }

        return progress;
    }

    /**
     * Calculates the current progress percentage for the Summary process.
     *
     * @param summaryIDNum  - ID attribute value to calculate progress for gotten from project metadata
     * @param compStrategy
     * @param daysPerInputData
     * @param pluginInfo  - reference to a ProjectInfoPlugin object to use
     * @param stmt  - Statement object to reuse
     * @return double  - progress percentage of the summary process for the specified pluginInfo and project defined summary identified by summaryID
     * @throws SQLException
     */
    public double GetCurrentSummaryProgress(int summaryIDNum, TemporalSummaryCompositionStrategy compStrategy, int daysPerInputData, ProjectInfoPlugin pluginInfo, Statement stmt)
            throws SQLException
    {
        Connection con = RDS.getCentralDBconnection();
        Statement stmt1 = con.createStatement();
        double progress = 0;
        int projectSummaryID = Schemas.getProjectSummaryID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), summaryIDNum, stmt1);
        String mSchemaName = Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginInfo.GetName());
        int currentCount = calculateSummaryCurrentCount(projectSummaryID, mSchemaName, stmt, projectMetaData.GetStartDate(),projectMetaData.GetEndDate());
        int expectedCount = calculateSummaryExpectedCount(mSchemaName, compStrategy, daysPerInputData, pluginInfo, stmt);
        //currentCount++;
        if(expectedCount > 0 && currentCount > 0)
        {
            progress = (new Double(currentCount) / new Double(expectedCount)) * 100;
        }

        if(progress > 100 || progress < 0) {
            ErrorLog.add(configInstance, "Invalid Summary progress (current = " + currentCount + ", expected = " + expectedCount + ") of " + progress
                    + " for plugin " + pluginInfo.GetName() + " and summary with ID " + summaryIDNum + ".", new Exception("Invalid Summary progress."));
        }
        stmt1.close();
        con.close();

        return progress;
    }

    /**
     * Calculates and updates, if necessary, the currently database stored DownloadExpectedCount values.
     * @param dataName  - the name of the file type being downloaded (e.g. "Data" or "Qc")
     * @param pluginName  - the plugin title gotten from the plugin metadata to calculate progress in relation to
     * @param listDatesFiles  - reference to the ListDatesFiles object to use
     * @param modisTileNames  - list of modis tiles included
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateDBDownloadExpectedCount(String pluginName, String dataName, ListDatesFiles listDatesFiles, ArrayList<String> modisTileNames, Statement stmt) throws SQLException
    {
        int storedExpectedCount = getStoredDownloadExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginName, dataName, stmt);
        int calculatedExpectedCount = calculateDownloadExpectedCount(listDatesFiles, modisTileNames);
        if(storedExpectedCount != calculatedExpectedCount)
        {
            String updateQuery = "UPDATE \"" + configInstance.getGlobalSchema() + "\".\"DownloadExpectedTotalOutput\" SET \"ExpectedNumOfOutputs\" = " + calculatedExpectedCount + " WHERE " +
                    "\"ProjectID\" = " + Schemas.getProjectID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), stmt) + " AND " +
                    "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt) + " AND " +
                    "\"DataName\" = '" + dataName + "';";

            stmt.execute(updateQuery);

            downloadExpectedFiles.get(pluginName).put(dataName, calculatedExpectedCount);

        }
    }

    /**
     * Calculates and updates, if necessary, the currently database stored ProcessorExpectedCount value.
     * @param pluginName  - the plugin title gotten from the plugin metadata to calculate progress in relation to
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateDBProcessorExpectedCount(String pluginName, Statement stmt) throws SQLException
    {
        Connection conn = RDS.getCentralDBconnection();
        Statement stmt1 = conn.createStatement();
        ProjectInfoPlugin pluginInfo = null;
        for(ProjectInfoPlugin plugin : projectMetaData.GetPlugins()) {
            if(plugin.GetName().equals(pluginName)) {
                pluginInfo = plugin;
                break;
            }
        }
        if(pluginInfo == null) {
            ErrorLog.add(configInstance, "Mising project plugin info for plugin '" + pluginName + "'.", new Exception("Plugin '" + pluginName + "' info could not be found in project "
                    + "metadata."));
            return;
        }
        String mSchemaName = Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginInfo.GetName());
        int storedExpectedCount = getStoredProcessorExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginName, stmt1);
        //getStoredProcessorExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginName, stmt1);
        int calculatedExpectedCount = calculateProcessorExpectedCount(pluginMetaDataCollection.pluginMetaDataMap.get(pluginName), pluginInfo, mSchemaName, stmt,projectMetaData.GetStartDate(),projectMetaData.GetEndDate());

        if(storedExpectedCount != calculatedExpectedCount)
        {
            /* String updateQuery = "UPDATE \"" + configInstance.getGlobalSchema() + "\".\"ProcessorExpectedTotalOutput\" SET \"ExpectedNumOfOutputs\" = " + calculatedExpectedCount + " WHERE " +
                    "\"ProjectID\" = " + Schemas.getProjectID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), stmt) + " AND " +
                    "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt) + ";";*/
            String updateQuery_ = "UPDATE \"" + configInstance.getGlobalSchema() + "\".\"ProcessorExpectedTotalOutput\" SET \"ExpectedNumOfOutputs\" = " + calculatedExpectedCount + " WHERE " +
                    "\"ProjectID\" = " + Schemas.getProjectID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), stmt1) + " AND " +
                    "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt1) + ";";
            //stmt.execute(updateQuery);
            stmt1.execute(updateQuery_);
            processorExpectedNumOfOutputs.put(pluginName, calculatedExpectedCount);
        }
        stmt1.close();
        conn.close();
    }

    /**
     * Calculates and updates, if necessary, the currently database stored IndicesExpectedCount value.
     * @param pluginName  - the plugin title gotten from the plugin metadata to calculate progress in relation to
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateDBIndicesExpectedCount(String pluginName, Statement stmt) throws SQLException
    {
        Connection conn = RDS.getCentralDBconnection();
        Statement stmt1 = conn.createStatement();
        int storedExpectedCount = getStoredIndicesExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginName, stmt1);
        //getStoredIndicesExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginName, stmt1);
        int calculatedExpectedCount = calculateIndicesExpectedCount(Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginName), pluginMetaDataCollection.pluginMetaDataMap.get(pluginName), pluginName, stmt, projectMetaData.GetStartDate(),projectMetaData.GetEndDate());
        if(storedExpectedCount != calculatedExpectedCount)
        {
            /* String updateQuery = "UPDATE \"" + configInstance.getGlobalSchema() + "\".\"IndicesExpectedTotalOutput\" SET \"ExpectedNumOfOutputs\" = " + calculatedExpectedCount + " WHERE " +
                    "\"ProjectID\" = " + Schemas.getProjectID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), stmt) + " AND " +
                    "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt) + ";";*/
            String updateQuery_ = "UPDATE \"" + configInstance.getGlobalSchema() + "\".\"IndicesExpectedTotalOutput\" SET \"ExpectedNumOfOutputs\" = " + calculatedExpectedCount + " WHERE " +
                    "\"ProjectID\" = " + Schemas.getProjectID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), stmt1) + " AND " +
                    "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt1) + ";";
            //stmt1.execute(updateQuery);
            stmt1.execute(updateQuery_);
            indicesExpectedNumOfOutputs.put(pluginName, calculatedExpectedCount);
        }
        stmt1.close();
        conn.close();
    }

    /**
     * Calculates and updates, if necessary, the currently database stored SummaryExpectedCount value.
     * @param summaryIDNum  - ID attribute value to calculate progress for gotten from project metadata
     * @param compStrategy  - TemporalSummaryCompositionStrategy object to use in calculating total expecting results in temporal summary cases
     * @param daysPerInputData  - number of days each input file represents
     * @param pluginInfo  - reference to a ProjectInfoPlugin object to use
     * @param stmt  - Statement object to reuse
     * @throws SQLException
     */
    public void UpdateDBSummaryExpectedCount(int summaryIDNum, TemporalSummaryCompositionStrategy compStrategy, int daysPerInputData, ProjectInfoPlugin pluginInfo, Statement stmt) throws SQLException
    {
        Connection conn = RDS.getCentralDBconnection();
        Statement stmt1 = conn.createStatement();

        int storedExpectedCount = getStoredSummaryExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginInfo.GetName(), summaryIDNum, stmt1);
        //getStoredSummaryExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginInfo.GetName(), summaryIDNum, stmt1);
        int calculatedExpectedCount = calculateSummaryExpectedCount(Schemas.getSchemaName(projectMetaData.GetParentProjectName(), pluginInfo.GetName()), compStrategy, daysPerInputData, pluginInfo, stmt);
        if(storedExpectedCount != calculatedExpectedCount)
        {
            /* String updateQuery = "UPDATE \"" + configInstance.getGlobalSchema() + "\".\"SummaryExpectedTotalOutput\" SET \"ExpectedNumOfOutputs\" = " + calculatedExpectedCount + " WHERE " +
                    "\"ProjectSummaryID\" = " + Schemas.getProjectSummaryID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), summaryIDNum, stmt) + " AND " +
                    "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginInfo.GetName(), stmt) + ";";*/
            String updateQuery_ = "UPDATE \"" + configInstance.getGlobalSchema() + "\".\"SummaryExpectedTotalOutput\" SET \"ExpectedNumOfOutputs\" = " + calculatedExpectedCount + " WHERE " +
                    "\"ProjectSummaryID\" = " + Schemas.getProjectSummaryID(configInstance.getGlobalSchema(), projectMetaData.GetParentProjectName(), summaryIDNum, stmt1) + " AND " +
                    "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginInfo.GetName(), stmt1) + ";";
            //stmt.execute(updateQuery);
            stmt1.execute(updateQuery_);
            summaryExpectedNumOfOutputs.get(pluginInfo.GetName()).put(summaryIDNum, calculatedExpectedCount);
        }
        stmt1.close();
        conn.close();
    }

    protected int calculateDownloadCurrentCount(String mSchemaName, String dataName, Statement stmt,LocalDate startDate,LocalDate endDate) throws SQLException
    {
        int currentCount = 0;
        String progressQuery;
        String columnName;
        if(dataName.toLowerCase().equals("data"))
        {
            progressQuery = "SELECT Count(\"DownloadCacheID\") AS \"DownloadCacheIDCount\" FROM \"" + mSchemaName + "\".\"DownloadCache\" ";
            progressQuery += generateDatesFilter(startDate, endDate, "DataFilePath");
            progressQuery += ";";
            columnName = "DownloadCacheIDCount";
        }
        else
        {
            progressQuery = "SELECT Count(\"DownloadCacheExtraID\") AS \"DownloadCacheExtraIDCount\" FROM \"" + mSchemaName + "\".\"DownloadCacheExtra\" ";
            progressQuery += generateDatesFilter(startDate, endDate, "DataName");
            progressQuery += ";";
            columnName = "DownloadCacheExtraIDCount";
        }

        ResultSet rs = stmt.executeQuery(progressQuery);
        if(rs != null)
        {
            if(rs.next()) {
                currentCount = rs.getInt(columnName);
            }
            rs.close();
        }
        return currentCount;
    }

    /**
     * Function to filter files inside a dates range
     * @param startDate
     * @param endDate
     * @param table
     * @return Portion of SQL code with the filters
     */
    private String generateDatesFilter(LocalDate startDate, LocalDate endDate, String table){
        String filter = "";
        int lastMonth, month;
        boolean where = true;
        if(startDate.getYear()-endDate.getYear() > 0) {
            return filter;
        }
        for(int i=startDate.getYear(); i <= endDate.getYear();i++){
            if(startDate.getYear()==endDate.getYear()){
                month = startDate.getMonthValue();
                lastMonth = endDate.getMonthValue();
            }
            else if(i==startDate.getYear()){
                month = startDate.getMonthValue();
                lastMonth = 12;
            }
            else if(i==endDate.getYear()){
                month = 1;
                lastMonth = endDate.getMonthValue();
            }
            else{
                month = 1;
                lastMonth = 12;
            }
            for(int j=month; j <= lastMonth; j++){
                if(where){
                    filter += "WHERE";
                    where = false;
                }
                if(j < 10) {
                    filter += " \""+table+"\" LIKE Concat('%','"+i+'0'+j+"','%') OR";
                } else {
                    filter += " \""+table+"\" LIKE Concat('%','"+i+j+"','%') OR";
                }
            }
        }
        if(filter.length() > 0) {
            filter = filter.substring(0,filter.length()-3);
        }
        return filter;
    }

    protected int calculateDownloadExpectedCount(ListDatesFiles listDatesFiles, ArrayList<String> modisTileNames)
    {
        int expectedCount = 0;

        // Clean up and format tiles list if necessary
        if(modisTileNames != null)
        {
            for(int i=0; i < modisTileNames.size(); i++) {
                modisTileNames.set(i, modisTileNames.get(i).trim().toLowerCase());
            }
        }

        // Check for desired files
        for(ArrayList<String> files : listDatesFiles.CloneListDatesFiles().values())
        {
            if(modisTileNames != null && modisTileNames.size() > 0)
            {
                Iterator<String> tileIt;
                for(String file : files)
                {
                    tileIt = modisTileNames.iterator();
                    String temp = file.trim().toLowerCase();
                    while(tileIt.hasNext())
                    {
                        if(temp.contains(tileIt.next())) {
                            expectedCount += 1;
                            break;
                        }
                    }
                }
            } else {
                expectedCount += files.size();
            }
        }

        return expectedCount;
    }

    protected int calculateMaxDownloadExpectedCount(PluginMetaData pluginMetaData, LocalDate startDate, LocalDate endDate, ArrayList<String> modisTileNames)
    {
        int expectedCount = 0;

        long daysSinceStart = ChronoUnit.DAYS.between(startDate, endDate) + 1;//LocalDate.now());
        int adjustedDaysSinceStart = (int)(daysSinceStart / pluginMetaData.Download.DaysPerInputData);

        if(modisTileNames != null && modisTileNames.size() > 1) {
            expectedCount = adjustedDaysSinceStart * modisTileNames.size();
        } else {
            expectedCount = adjustedDaysSinceStart * pluginMetaData.Download.filesPerDay;
        }

        return expectedCount;
    }

    protected int calculateProcessorCurrentCount(String mSchemaName, Statement stmt, LocalDate startDate, LocalDate endDate) throws SQLException
    {
        int currentCount = 0;
        String progressQuery = "SELECT Count(\"ProcessorCacheID\") AS \"ProcessorCacheIDCount\" FROM \"" + mSchemaName + "\".\"ProcessorCache\" ";
        //progressQuery += generateDatesFilter(startDate, endDate, "DataFilePath");
        progressQuery += ";";
        ResultSet rs = stmt.executeQuery(progressQuery);
        if(rs != null)
        {
            if(rs.next()) {
                currentCount = rs.getInt("ProcessorCacheIDCount");
            }
            rs.close();
        }
        return currentCount;
    }

    protected int calculateProcessorExpectedCount(PluginMetaData pluginMetaData, ProjectInfoPlugin pluginInfo, String mSchemaName, Statement stmt, LocalDate startDate, LocalDate endDate) throws SQLException
    {
        //        if(downloadExpectedFiles.get(pluginInfo.GetName()).get("data") == null)
        //        {
        //            downloadExpectedFiles.get(pluginInfo.GetName()).put("data", getStoredDownloadExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginInfo.GetName(), "data", stmt));
        //        }
        //        if(pluginMetaData.ExtraInfo.Tiles) {
        //            return pluginMetaData.Processor.numOfOutput * (downloadExpectedFiles.get(pluginInfo.GetName()).get("data") / pluginInfo.GetModisTiles().size());
        //        } else {
        //            return pluginMetaData.Processor.numOfOutput * downloadExpectedFiles.get(pluginInfo.GetName()).get("data");
        //        }
        int daysDownloadFor = 0;
        // if (stmt.getConnection().getCatalog().toString().compareTo("eastwebDB") != 0 ){
        String progressQuery = "SELECT Count(Distinct \"DateGroupID\") \"DateGroupIDCount\" FROM \"" + mSchemaName + "\".\"DownloadCache\" ";
        //progressQuery += generateDatesFilter(startDate, endDate, "DataFilePath");
        progressQuery += ";";

        ResultSet rs = stmt.executeQuery(progressQuery);
        if(rs != null)
        {
            if(rs.next()) {
                daysDownloadFor = rs.getInt("DateGroupIDCount");
            }
            rs.close();
        }
        // }
        return pluginMetaData.Processor.numOfOutput * daysDownloadFor;
    }

    protected int calculateIndicesCurrentCount(String mSchemaName, Statement stmt, LocalDate startDate, LocalDate endDate) throws SQLException
    {
        String progressQuery = "SELECT Count(\"IndicesCacheID\") AS \"IndicesCacheIDCount\" FROM \"" + mSchemaName + "\".\"IndicesCache\" ";
        //progressQuery += generateDatesFilter(startDate, endDate, "DataFilePath");
        progressQuery += ";";
        int currentCount = 0;
        ResultSet rs = stmt.executeQuery(progressQuery);
        if(rs != null)
        {
            if(rs.next()) {
                currentCount = rs.getInt("IndicesCacheIDCount");
            }
            rs.close();
        }
        return currentCount;
    }

    protected int calculateIndicesExpectedCount(String mSchemaName, PluginMetaData pluginMetaData, String pluginName, Statement stmt, LocalDate startDate, LocalDate endDate) throws SQLException
    {
        Connection con = RDS.getCentralDBconnection();
        Statement stmt1 = con.createStatement();
        if(processorExpectedNumOfOutputs.get(pluginName) == null)
        {
            processorExpectedNumOfOutputs.put(pluginName, getStoredProcessorExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginName, stmt1));
        }
        int indicesCount = 0;
        for(ProjectInfoPlugin pluginInfo : projectMetaData.GetPlugins()) {
            if(pluginInfo.GetName().equalsIgnoreCase(pluginName)) {
                indicesCount = pluginInfo.GetIndices().size();
                break;
            }
        }
        int dateGroups = 0;
        // if(stmt.getConnection().getCatalog().toString().compareTo("eastwebDB") != 0 ){
        //System.out.println(stmt.getConnection().getCatalog().toString());
        // System.out.println(indicesCount);
        String progressQuery = "SELECT Count(Distinct \"DateGroupID\") \"DateGroupIDCount\" FROM \"" + mSchemaName + "\".\"ProcessorCache\" ";
        //progressQuery += generateDatesFilter(startDate, endDate, "DataFilePath");
        progressQuery += ";";
        ResultSet rs = stmt.executeQuery(progressQuery);
        if(rs != null) {
            if(rs.next()) {
                dateGroups = rs.getInt("DateGroupIDCount");
            }
            rs.close();
        }
        // }

        //System.out.println(indicesCount * dateGroups);
        return (indicesCount * dateGroups);
    }

    protected int calculateSummaryCurrentCount(int projectSummaryID, String mSchemaName, Statement stmt, LocalDate startDate, LocalDate endDate) throws SQLException
    {
        int currentCount = 0;
        String temp = "";
        String progressQuery = "SELECT \"IndexID\", Count(DISTINCT \"DateGroupID\") AS \"DateGroupIDCount\" " +
                "FROM \"" + mSchemaName + "\".\"ZonalStat\" " +
                "WHERE \"ProjectSummaryID\"=" + projectSummaryID;
        //temp = generateDatesFilter(startDate, endDate, "FilePath");
        //temp = temp.replaceFirst("WHERE", "AND");
        //progressQuery += temp;
        progressQuery += " GROUP BY \"IndexID\", \"ProjectSummaryID\";";
        //javax.swing.JOptionPane.showMessageDialog(null, temp);
        //javax.swing.JOptionPane.showMessageDialog(null, progressQuery);
        ResultSet rs = stmt.executeQuery(progressQuery);
        if(rs != null)
        {
            while(rs.next()) {
                currentCount += rs.getInt("DateGroupIDCount");
            }
            rs.close();
        }
        //System.out.println("##"+currentCount);
        return currentCount;
    }

    protected int calculateSummaryExpectedCount(String mSchemaName, TemporalSummaryCompositionStrategy compStrategy, int daysPerInputData, ProjectInfoPlugin pluginInfo, Statement stmt) throws SQLException
    {
        int expectedCount = 0;
        boolean flag = false;

        Connection con = RDS.getCentralDBconnection();
        Statement stmt1 = con.createStatement();
        if(compStrategy != null)
        {
            /*ResultSet rs = stmt.executeQuery("SELECT A.\"DateGroupID\", D.\"Year\", D.\"DayOfYear\" " +
                    "FROM \"" + mSchemaName + "\".\"IndicesCache\" A INNER JOIN \"" + configInstance.getGlobalSchema() + "\".\"DateGroup\" D " +
                    "ON D.\"DateGroupID\"=A.\"DateGroupID\" " +
                    "ORDER BY D.\"Year\" ASC, D.\"DayOfYear\" ASC;");*/
            ResultSet rs = stmt.executeQuery("SELECT \"DateGroupID\" " +
                    "FROM \"" + mSchemaName + "\".\"IndicesCache\""+
                    " ORDER BY \"DateGroupID\" ASC;");

            /*ResultSet rs1 = stmt1.executeQuery("SELECT \"DateGroupID\", \"Year\", \"DayOfYear\" " +
                    "FROM \""+ configInstance.getGlobalSchema() + "\".\"DateGroup\" " +
                    "ORDER BY \"Year\" ASC, \"DayOfYear\" ASC;");*/
            ResultSet rs1 = null;
            if (rs != null){
                //if(rs.next()){
                ArrayList<LocalDate> Dates = new ArrayList<LocalDate>();
                while(rs.next()){
                    rs1 = stmt1.executeQuery("SELECT \"DateGroupID\", \"Year\", \"DayOfYear\" " +
                            "FROM \""+ configInstance.getGlobalSchema() + "\".\"DateGroup\" " +
                            "Where \"DateGroupID\"= "+rs.getInt("DateGroupID")+
                            "ORDER BY \"Year\" ASC, \"DayOfYear\" ASC;");
                    if (rs1.next()){
                        Dates.add(LocalDate.ofYearDay(rs1.getInt("Year"), rs1.getInt("DayOfYear")));
                    }
                    rs1.close();
                }
                rs.close();
                //System.out.println("Dates:*******"+Dates.size());
                if(Dates.size() > 0){
                    LocalDate startDate = Dates.get(0);
                    LocalDate endDate = Dates.get(Dates.size()-1);
                    if(daysPerInputData != 1) {
                        endDate = endDate.plusDays(compStrategy.getDaysInThisComposite(endDate));
                    }
                    endDate = endDate.plusDays(1);  // Adjust for algorithms exclusive endDate condition.
                    long completeCompositesInRange = compStrategy.getNumberOfCompleteCompositesInRange(startDate, endDate);
                    expectedCount = (int) (completeCompositesInRange * pluginInfo.GetIndices().size());
                    System.out.println("expectedCount "+ expectedCount);
                }
            }
            //}



            /*if(rs != null && rs1 != null) {
                if (rs.next() && rs1.next()){
                    ArrayList<LocalDate> Dates = new ArrayList<LocalDate>();
                    while (rs.next() && rs1.next()){
                        if (rs.getInt("DateGroupID") == rs1.getInt("DateGroupID")){
                            Dates.add(LocalDate.ofYearDay(rs1.getInt("Year"), rs1.getInt("DayOfYear")));
                            //flag = true;
                        }
                        System.out.println("****************"+Dates.size());
                    }
                    // if(rs.next())
                    //{
                        //LocalDate startDate = LocalDate.ofYearDay(rs.getInt("Year"), rs.getInt("DayOfYear"));
                        LocalDate startDate = Dates.get(0);
                        //while(!rs.isLast()) { rs.next(); }
                        //LocalDate endDate = LocalDate.ofYearDay(rs.getInt("Year"), rs.getInt("DayOfYear"));
                        LocalDate endDate = Dates.get(Dates.size());
                        if(daysPerInputData != 1) {
                            endDate = endDate.plusDays(compStrategy.getDaysInThisComposite(endDate));
                        }
                        endDate = endDate.plusDays(1);  // Adjust for algorithms exclusive endDate condition.
                        long completeCompositesInRange = compStrategy.getNumberOfCompleteCompositesInRange(startDate, endDate);
                        expectedCount = (int) (completeCompositesInRange * pluginInfo.GetIndices().size());
                    //}
                    rs.close();
                    rs1.close();
                }
            }*/
        }
        //}
        else {
            if(indicesExpectedNumOfOutputs.get(pluginInfo.GetName()) == null)
            {
                indicesExpectedNumOfOutputs.put(pluginInfo.GetName(), getStoredIndicesExpectedTotalOutput(projectMetaData.GetParentProjectName(), pluginInfo.GetName(), stmt1));
            }

            expectedCount = indicesExpectedNumOfOutputs.get(pluginInfo.GetName());
        }
        //System.out.println("##EX"+expectedCount);


        return expectedCount;
    }

    protected int getStoredDownloadExpectedTotalOutput(String projectName, String pluginName, String dataName, Statement stmt) throws SQLException
    {
        int expectedCount = 0;
        int projectID = Schemas.getProjectID(configInstance.getGlobalSchema(), projectName, stmt);
        int pluginID = Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt);

        String selectQuery = "SELECT \"ExpectedNumOfOutputs\" FROM \"" + configInstance.getGlobalSchema() + "\".\"DownloadExpectedTotalOutput\" WHERE " +
                "\"ProjectID\" = " + projectID + " AND " +
                "\"PluginID\" = " + pluginID + " AND " +
                "\"DataName\" = '" + dataName + "';";
        String insertQuery = "INSERT INTO \"" + configInstance.getGlobalSchema() + "\".\"DownloadExpectedTotalOutput\" (\"ExpectedNumOfOutputs\", \"ProjectID\", \"PluginID\", \"DataName\") VALUES " +
                "(0, " + projectID + ", " + pluginID + ", '" + dataName + "');";
        expectedCount = getOrInsertIfMissingValue(selectQuery, "ExpectedNumOfOutputs", insertQuery, stmt);

        return expectedCount;

    }

    protected int getStoredProcessorExpectedTotalOutput(String projectName, String pluginName, Statement stmt) throws SQLException
    {
        int expectedCount = 0;
        int projectID = Schemas.getProjectID(configInstance.getGlobalSchema(), projectName, stmt);
        int pluginID = Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt);

        String selectQuery = "SELECT \"ExpectedNumOfOutputs\" FROM \"" + configInstance.getGlobalSchema() + "\".\"ProcessorExpectedTotalOutput\" WHERE " +
                "\"ProjectID\" = " + projectID + " AND " +
                "\"PluginID\" = " + pluginID + ";";
        String insertQuery = "INSERT INTO \"" + configInstance.getGlobalSchema() + "\".\"ProcessorExpectedTotalOutput\" (\"ExpectedNumOfOutputs\", \"ProjectID\", \"PluginID\") VALUES " +
                "(0, " + projectID + ", " + pluginID + ");";
        expectedCount = getOrInsertIfMissingValue(selectQuery, "ExpectedNumOfOutputs", insertQuery, stmt);

        return expectedCount;
    }

    protected int getStoredIndicesExpectedTotalOutput(String projectName, String pluginName, Statement stmt) throws SQLException
    {
        int expectedCount = 0;
        int projectID = Schemas.getProjectID(configInstance.getGlobalSchema(), projectName, stmt);
        int pluginID = Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt);

        String selectQuery = "SELECT \"ExpectedNumOfOutputs\" FROM \"" + configInstance.getGlobalSchema() + "\".\"IndicesExpectedTotalOutput\" WHERE " +
                "\"ProjectID\" = " + projectID + " AND " +
                "\"PluginID\" = " + pluginID + ";";
        String insertQuery = "INSERT INTO \"" + configInstance.getGlobalSchema() + "\".\"IndicesExpectedTotalOutput\" (\"ExpectedNumOfOutputs\", \"ProjectID\", \"PluginID\") VALUES " +
                "(0, " + projectID + ", " + pluginID + ");";

        expectedCount = getOrInsertIfMissingValue(selectQuery, "ExpectedNumOfOutputs", insertQuery, stmt);

        return expectedCount;

    }

    protected int getStoredSummaryExpectedTotalOutput(String projectName, String pluginName, int summaryIDNum, Statement stmt) throws SQLException
    {
        int expectedCount = 0;
        int projectSummaryID = Schemas.getProjectSummaryID(configInstance.getGlobalSchema(), projectName, summaryIDNum, stmt);
        int pluginID = Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt);

        String selectQuery = "SELECT \"ExpectedNumOfOutputs\" FROM \"" + configInstance.getGlobalSchema() + "\".\"SummaryExpectedTotalOutput\" WHERE " +
                "\"ProjectSummaryID\" = " + projectSummaryID + " AND " +
                "\"PluginID\" = " + Schemas.getPluginID(configInstance.getGlobalSchema(), pluginName, stmt) + ";";
        String insertQuery = "INSERT INTO \"" + configInstance.getGlobalSchema() + "\".\"SummaryExpectedTotalOutput\" (\"ExpectedNumOfOutputs\", \"ProjectSummaryID\", \"PluginID\") VALUES " +
                "(0, " + projectSummaryID + ", " + pluginID + ");";
        expectedCount = getOrInsertIfMissingValue(selectQuery, "ExpectedNumOfOutputs", insertQuery, stmt);

        return expectedCount;
    }

    protected int getOrInsertIfMissingValue(final String selectQuery, final String valueField, final String insertQuery, final Statement stmt) throws SQLException
    {
        int value = 0;

        ResultSet rs = stmt.executeQuery(selectQuery);
        if(rs != null && rs.next()) {
            value = rs.getInt(valueField);
            rs.close();
        }
        else {
            if(rs != null) {
                rs.close();
            }
            stmt.execute(insertQuery);
            rs = stmt.executeQuery(selectQuery);
            if(rs != null)
            {
                if(rs.next()) {
                    value = rs.getInt(valueField);
                }
                rs.close();
            }
        }

        return value;
    }
}
