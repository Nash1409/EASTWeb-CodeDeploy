package version2.prototype.util;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;

/**
 * Represents a schema in the PostgreSQL database. Allows for recreating/creating of required schemas for EASTWeb.
 *
 * @author michael.devos
 *
 */
public class Schemas {
    //public static boolean IsUpdated, IsCreated = false;
    /**
     * Recreates or creates a full schema identified by the given project name and plugin name.
     * Creates all database cache tables required by frameworks and download classes.
     *
     * @param postgreSQLConnection  - a connection object (can be obtained from DatabaseConnector.getConnection())
     * @param globalEASTWebSchema  - the global schema name (can be gotten from Config.getInstance().getGlobalSchema()
     * @param projectMetaData  - the loaded project metadata from xml to use
     * @param pluginName  - name of plugin to create schema for
     * @param summaryNames  - used to define ZonalStat table
     * @param tempSummaryCompStrategyNames
     * @param daysPerInputFile  - relevant to plugin entry creation and calculating the number of expected results to be found in ZonalStat
     * @param filesPerDay  - relevant to plugin entry creation and calculating when downloads are ready to be loaded by the LocalDownloader
     * @param createTablesWithForeignKeyReferences  - TRUE if tables should be created so that their foreign keys are referencing their corresponding primary keys, FALSE if tables shouldn't be created
     * to enforce foreign key rules
     */
    public static void CreateProjectPluginSchema(Connection postgreSQLConnection, String globalEASTWebSchema, ProjectInfoFile projectMetaData, String pluginName, ArrayList<String> summaryNames,
            ArrayList<String> tempSummaryCompStrategyNames, Integer daysPerInputFile, Integer filesPerDay, boolean createTablesWithForeignKeyReferences)
    {
        final Connection conn;
        final Statement stmt;
        final String mSchemaName;
        try{
            conn = postgreSQLConnection;
            stmt = conn.createStatement();
            String projectName = null;
            String fullPath = projectMetaData.GetFullPath();
            if(fullPath.endsWith("\\") || fullPath.endsWith("/")){
                projectName = fullPath.substring(0, -1).replace('\\', '/');
            }
            else{
                projectName = fullPath.replace('\\', '/');
            }
            projectName = projectName.substring(projectName.lastIndexOf('/')+1);
            mSchemaName = getSchemaName(projectName, pluginName);




            // create Global schema in Cloud DB
            CreateSchema(globalEASTWebSchema,projectMetaData,pluginName, summaryNames,
                    tempSummaryCompStrategyNames,daysPerInputFile,filesPerDay,createTablesWithForeignKeyReferences);

            // Drop an existing schema with the same name
            //        dropSchemaIfExists(stmt, mSchemaName);

            // Create the schema for this project in Local DB
            createSchemaIfNotExist(mSchemaName, stmt);


            // Create the global schema for EASTWeb if it doesn't already exist
            /*createSchemaIfNotExist(globalEASTWebSchema, stmt);

            // Create DateGroup table
            createDateGroupTable(globalEASTWebSchema, stmt);

            // Create Project table
            createProjectTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // Create Plugin table
            createPluginTableIfNotExists(globalEASTWebSchema, stmt);

            // Create TemporalSummaryCompositionStrategy table
            createTemporalSummaryCompositionStrategyTableIfNotExists(globalEASTWebSchema, stmt);

            // Create the ProjectSummary table
            createProjectSummaryTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // Create GlobalDownloader table
            createGlobalDownloaderTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // Create Download table
            createDownloadTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // Create DownloadExtra table
            createDownloadExtraTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // Create Environmental Index table
            createIndexTableIfNotExists(globalEASTWebSchema, stmt);

            // Add temporal summary composition strategies
            if(tempSummaryCompStrategyNames != null && !tempSummaryCompStrategyNames.isEmpty()) {
                for(String strategyClassName : tempSummaryCompStrategyNames) {
                    addTemporalSummaryCompositionStrategy(globalEASTWebSchema, strategyClassName, stmt);
                }
            }

            // Get DateGroupID
            //IsCreated = true;
            int dateGroupID = getDateGroupID(globalEASTWebSchema, projectMetaData.GetStartDate(), stmt);

            // Add entry to Project table
            addProject(globalEASTWebSchema, projectName, dateGroupID, stmt);

            // Add entry to Plugin table if not already existing
            addPlugin(globalEASTWebSchema, pluginName, daysPerInputFile, filesPerDay, stmt);

            // Add summaries to ProjectSummary table
            if(projectMetaData.GetSummaries() != null)
            {
                String strategyName;
                for(ProjectInfoSummary summary : projectMetaData.GetSummaries()) {
                    strategyName = summary.GetTemporalSummaryCompositionStrategyClassName();
                    if(strategyName != null) {
                        strategyName = strategyName.substring(strategyName.lastIndexOf(".") + 1);
                    }
                    addProjectSummaryID(globalEASTWebSchema, projectName, summary.GetID(), summary.GetZonalSummary().GetAreaNameField(), summary.GetZonalSummary().GetShapeFile(),
                            summary.GetZonalSummary().GetAreaCodeField(), strategyName, stmt);
                }
            }*/

            //tables related to sub-schema >> project schema

            // Create the ZonalStats table
            createZonalStatTableIfNotExists(globalEASTWebSchema, mSchemaName, summaryNames, stmt, createTablesWithForeignKeyReferences);

            // Create cache tables for each framework
            createDownloadCacheTableIfNotExists(globalEASTWebSchema, stmt, mSchemaName, createTablesWithForeignKeyReferences);
            createDownloadCacheExtraTableIfNotExists(globalEASTWebSchema, stmt, mSchemaName, createTablesWithForeignKeyReferences);
            createProcessorCacheTableIfNotExists(globalEASTWebSchema, stmt, mSchemaName, createTablesWithForeignKeyReferences);
            createIndicesCacheTableIfNotExists(globalEASTWebSchema, stmt, mSchemaName, createTablesWithForeignKeyReferences);

            // Create expected total tables
            /*createDownloadExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);
            createProcessorExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);
            createIndicesExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);
            createSummaryExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);*/
        }
        catch(SQLException e)
        {
            ErrorLog.add(Config.getInstance(), "SQL Exception in Schemas.", e);
        }
    }

    public static void CreateSchema(String globalEASTWebSchema, ProjectInfoFile projectMetaData, String pluginName, ArrayList<String> summaryNames,
            ArrayList<String> tempSummaryCompStrategyNames, Integer daysPerInputFile, Integer filesPerDay, boolean createTablesWithForeignKeyReferences){
        final Connection conn;
        final Statement stmt;
        //RDS centralDB = new RDS();
        String projectName = null;
        String fullPath = projectMetaData.GetFullPath();
        if(fullPath.endsWith("\\") || fullPath.endsWith("/")){
            projectName = fullPath.substring(0, -1).replace('\\', '/');
        }
        else{
            projectName = fullPath.replace('\\', '/');
        }
        projectName = projectName.substring(projectName.lastIndexOf('/')+1);
        //mSchemaName = getSchemaName(projectName, pluginName);

        try{
            conn = RDS.getCentralDBconnection();
            stmt = conn.createStatement();
            // Create the global schema for EASTWeb if it doesn't already exist
            createSchemaIfNotExist(globalEASTWebSchema, stmt);

            // 1-Create DateGroup table
            createDateGroupTable(globalEASTWebSchema, stmt);

            // 2-Create Project table
            createProjectTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // 3-Create Plugin table
            createPluginTableIfNotExists(globalEASTWebSchema, stmt);

            // 4-Create TemporalSummaryCompositionStrategy table
            createTemporalSummaryCompositionStrategyTableIfNotExists(globalEASTWebSchema, stmt);

            // 5-Create the ProjectSummary table
            createProjectSummaryTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // 6-Create GlobalDownloader table
            createGlobalDownloaderTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // 7-Create Download table
            createDownloadTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // 8-Create DownloadExtra table
            createDownloadExtraTableIfNotExists(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            // 9-Create Environmental Index table
            createIndexTableIfNotExists(globalEASTWebSchema, stmt);

            // Add temporal summary composition strategies
            if(tempSummaryCompStrategyNames != null && !tempSummaryCompStrategyNames.isEmpty()) {
                for(String strategyClassName : tempSummaryCompStrategyNames) {
                    addTemporalSummaryCompositionStrategy(globalEASTWebSchema, strategyClassName, stmt);
                }
            }

            // Get DateGroupID
            //IsCreated = true;
            int dateGroupID = getDateGroupID(globalEASTWebSchema, projectMetaData.GetStartDate(), stmt);

            // Add entry to Project table
            //if(!IsExist(globalEASTWebSchema, projectName, dateGroupID, stmt)){
            addProject(globalEASTWebSchema, projectName, dateGroupID, stmt);
            //}

            // Add entry to Plugin table if not already existing
            addPlugin(globalEASTWebSchema, pluginName, daysPerInputFile, filesPerDay, stmt);

            // Add summaries to ProjectSummary table
            if(projectMetaData.GetSummaries() != null)
            {
                String strategyName;
                for(ProjectInfoSummary summary : projectMetaData.GetSummaries()) {
                    strategyName = summary.GetTemporalSummaryCompositionStrategyClassName();
                    if(strategyName != null) {
                        strategyName = strategyName.substring(strategyName.lastIndexOf(".") + 1);
                    }
                    addProjectSummaryID(globalEASTWebSchema, projectName, summary.GetID(), summary.GetZonalSummary().GetAreaNameField(), summary.GetZonalSummary().GetShapeFile(),
                            summary.GetZonalSummary().GetAreaCodeField(), strategyName, stmt);
                }
            }


            // 10-Create expected total tables
            createDownloadExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);
            createProcessorExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);
            createIndicesExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);
            createSummaryExpectedTotalOutput(globalEASTWebSchema, stmt, createTablesWithForeignKeyReferences);

            conn.close();
        }catch(SQLException e)
        {
            ErrorLog.add(Config.getInstance(), "SQL Exception in Schemas.", e);
        }

    }

    public static void restLocalDB(String globalEASTWebSchema, String ProjectName, String plugin,Statement stmt){

        String mSchemaName = getSchemaName(ProjectName, plugin);
        try {
            System.out.println(mSchemaName);
            dropSchemaIfExists(mSchemaName,stmt);
            dropSchemaIfExists(globalEASTWebSchema,stmt);
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /* private static boolean IsExist(String globalEASTWebSchema, String projectName,
            int dateGroupID, Statement stmt) throws SQLException {
        String selectQuery = String.format("SELECT \"ProjectID\" FROM \"%1$s\".\"Project\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(projectName) + "';",
                globalEASTWebSchema
                );
        if(getID(selectQuery, "ProjectID", stmt) != -1){
            return true;
        }
        return false;


    }*/

    //Related to local DB
    public static void setProcessed(final String projectSchema, final String cacheTable, final int dateGroupID, final Statement stmt) throws SQLException
    {
        String updateQuery = "UPDATE \"" + projectSchema + "\".\"" + cacheTable + "\" SET \"Processed\" = TRUE WHERE \"DateGroupID\"=" + dateGroupID + ";";
        stmt.execute(updateQuery);
    }

    public static void setProcessedComposite(final String globalSchema, final String projectSchema, final String cacheTable, final int startYear, final int startDay, final int daysInComposite,
            final Statement stmt) throws SQLException
    {
        Connection con = RDS.getCentralDBconnection();
        Statement stmt1 = con.createStatement();
        LocalDate currentDate = LocalDate.ofYearDay(startYear, startDay);
        String updateQueryFormat = "UPDATE \"" + projectSchema + "\".\"" + cacheTable + "\" SET \"Processed\" = TRUE WHERE \"DateGroupID\"=%1$d;";
        //get the DateGroupID from Cloud DB
        int dateGroupID = getDateGroupID(globalSchema, currentDate, stmt1);
        //int dateGroupID_ = getDateGroupID(globalSchema, currentDate, stmt1);
        stmt.addBatch(String.format(updateQueryFormat, dateGroupID));

        int numOfDaysSetFor = 1;
        while(numOfDaysSetFor <= daysInComposite) {
            currentDate = currentDate.plusDays(1);
            dateGroupID = getDateGroupID(globalSchema, currentDate, stmt1);
            //dateGroupID_ = getDateGroupID(globalSchema, currentDate, stmt1);
            stmt.addBatch(String.format(updateQueryFormat, dateGroupID));
            numOfDaysSetFor += 1;
        }
        stmt.executeBatch();
    }

    public static boolean registerGlobalDownloader(final String globalEASTWebSchema, final String pluginName, final String dataName, final Statement stmt) throws SQLException
    {
        /*Connection conn = RDS.getCentralDBconnection();
        Statement stmt1 = conn.createStatement();*/
        final int pluginID = Schemas.getPluginID(globalEASTWebSchema, pluginName, stmt);
        //final int pluginID_ = Schemas.getPluginID(globalEASTWebSchema, pluginName, stmt1);

        String selectQuery = "SELECT \"GlobalDownloaderID\" FROM \"" + globalEASTWebSchema + "\".\"GlobalDownloader\" WHERE \"PluginID\" = " + pluginID + " AND \"DataName\" like '" + escapeUnderScoresAndPercents(dataName) + "';";
        String insertQuery = "INSERT INTO \"" + globalEASTWebSchema + "\".\"GlobalDownloader\" (\"PluginID\", \"DataName\") VALUES (" + pluginID + ", '" + dataName + "');";

        //String selectQuery_ = "SELECT \"GlobalDownloaderID\" FROM \"" + globalEASTWebSchema + "\".\"GlobalDownloader\" WHERE \"PluginID\" = " + pluginID_ + " AND \"DataName\" like '" + escapeUnderScoresAndPercents(dataName) + "';";
        //String insertQuery_ = "INSERT INTO \"" + globalEASTWebSchema + "\".\"GlobalDownloader\" (\"PluginID\", \"DataName\") VALUES (" + pluginID_ + ", '" + dataName + "');";

        //addRowIFNotExistent(selectQuery_, insertQuery_, stmt1);
        //stmt1.close();
        //conn.close();
        return addRowIFNotExistent(selectQuery, insertQuery, stmt);
        //return addRowIFNotExistent(selectQuery_, insertQuery_, stmt1);

    }

    public static int getProjectSummaryID(final String globalEASTWebSchema, final String projectName, final Integer summaryNumID, final Statement stmt) throws SQLException
    {
        if(globalEASTWebSchema == null || summaryNumID == null || projectName == null) {
            return -1;
        }

        int projectID = getProjectID(globalEASTWebSchema, projectName, stmt);
        String selectQuery = String.format("SELECT \"ProjectSummaryID\" FROM \"%1$s\".\"ProjectSummary\" WHERE \"ProjectID\" = " + projectID + " AND \"SummaryIDNum\" = " + summaryNumID + ";",
                globalEASTWebSchema);
        return getID(selectQuery, "ProjectSummaryID", stmt);
    }

    public static int getProjectSummaryID(final String globalEASTWebSchema, final Integer projectID, final Integer summaryNumID, final Statement stmt) throws SQLException
    {
        if(globalEASTWebSchema == null || summaryNumID == null || projectID == null) {
            return -1;
        }

        String selectQuery = String.format("SELECT \"ProjectSummaryID\" FROM \"%1$s\".\"ProjectSummary\" WHERE \"ProjectID\" = " + projectID + " AND \"SummaryIDNum\" = " + summaryNumID + ";",
                globalEASTWebSchema);
        return getID(selectQuery, "ProjectSummaryID", stmt);
    }

    public static boolean addProjectSummaryID(final String globalEASTWebSchema, final String projectName, final Integer summaryNumID, final String areaNameField, final String shapeFilePath,
            final String areaValueField, final String temporalCompositionStrategyClassName, final Statement stmt) throws SQLException
    {
        if(globalEASTWebSchema == null || summaryNumID == null || projectName == null || areaNameField == null || shapeFilePath == null || areaValueField == null) {
            return false;
        }

        int projectID = getProjectID(globalEASTWebSchema, projectName, stmt);
        Integer compStrategyID = getTemporalSummaryCompositionStrategyID(globalEASTWebSchema, temporalCompositionStrategyClassName, stmt);
        if(temporalCompositionStrategyClassName != null && compStrategyID < 0) {
            addTemporalSummaryCompositionStrategy(globalEASTWebSchema, temporalCompositionStrategyClassName, stmt);
            compStrategyID = getTemporalSummaryCompositionStrategyID(globalEASTWebSchema, temporalCompositionStrategyClassName, stmt);
        }
        String selectQuery = String.format("SELECT \"ProjectSummaryID\" FROM \"%1$s\".\"ProjectSummary\" WHERE \"ProjectID\" = " + projectID + " AND \"SummaryIDNum\" = " + summaryNumID + ";",
                globalEASTWebSchema);
        String insertQuery = String.format("INSERT INTO \"%1$s\".\"ProjectSummary\" (\"ProjectID\", \"SummaryIDNum\", \"AreaNameField\", \"ShapeFile\", \"AreaCodeField\", " +
                "\"TemporalSummaryCompositionStrategyID\") VALUES (%2$d, %3$d, '%4$s', '%5$s', '%6$s', %7$d);",
                globalEASTWebSchema,
                projectID,
                summaryNumID,
                areaNameField,
                shapeFilePath,
                areaValueField,
                compStrategyID
                );
        return addRowIFNotExistent(selectQuery, insertQuery, stmt);
    }

    public static int getDateGroupID(final String globalEASTWebSchema, final LocalDate lDate, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null || lDate == null) {
            return -1;
        }
        int res;
        /*Connection conn = RDS.getCentralDBconnection();
        Statement stmt1 = conn.createStatement();*/

        String selectQuery = String.format("SELECT \"DateGroupID\" FROM \"%1$s\".\"DateGroup\" " +
                "WHERE \"Year\"=" + lDate.getYear() + " AND \"DayOfYear\"=" + lDate.getDayOfYear() + ";",
                globalEASTWebSchema
                );
        String insertQuery = String.format(
                "INSERT INTO \"%1$s\".\"DateGroup\" (\"Year\", \"DayOfYear\") VALUES (" + lDate.getYear() + "," + lDate.getDayOfYear() + ");",
                globalEASTWebSchema
                );
        res =  getOrInsertIfMissingID(selectQuery, "DateGroupID", insertQuery, stmt);
        /*if (IsUpdated && !IsCreated){
            if (conn != null){
                getOrInsertIfMissingID(selectQuery, "DateGroupID", insertQuery, stmt1);
                IsUpdated= false;
                IsCreated = false;
            }
        }*/
        //stmt1.close();
        //conn.close();

        return res;
    }

    public static Integer getIndexID(final String globalEASTWebSchema, final String indexNm, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null || indexNm == null) {
            return null;
        }
        /*Connection conn = RDS.getCentralDBconnection();
        Statement stmt1 = conn.createStatement();*/
        int res;

        String selectQuery = String.format("SELECT \"IndexID\" FROM \"%1$s\".\"Index\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(indexNm) + "';",
                globalEASTWebSchema
                );
        String insertQuery = String.format(
                "INSERT INTO \"%1$s\".\"Index\" (\"Name\") VALUES ('" + indexNm + "');",
                globalEASTWebSchema
                );

        /* if (IsUpdated){
            getOrInsertIfMissingID(selectQuery, "IndexID", insertQuery, stmt1);
            IsUpdated = false;
        }*/
        res =  getOrInsertIfMissingID(selectQuery, "IndexID", insertQuery, stmt);
        /*stmt1.close();
        conn.close();*/
        return res;
    }

    public static int getGlobalDownloaderID(final String globalEASTWebSchema, final String pluginName, final String dataName, final Statement stmt) throws SQLException,
    ClassNotFoundException, ParserConfigurationException, SAXException, IOException
    {
        int ID = -1;
        int pluginID = getPluginID(globalEASTWebSchema, pluginName, stmt);
        String selectQuery = String.format(
                "SELECT \"GlobalDownloaderID\" FROM \"%1$s\".\"GlobalDownloader\" WHERE \n" +
                        "\"PluginID\"=" + pluginID + " AND \"DataName\" like '" + escapeUnderScoresAndPercents(dataName) + "' ORDER BY \"GlobalDownloaderID\" DESC;",
                        globalEASTWebSchema
                );
        ID = getID(selectQuery, "GlobalDownloaderID", stmt);
        if(ID == -1)
        {
            registerGlobalDownloader(globalEASTWebSchema, pluginName, dataName, stmt);
            ID = getID(selectQuery, "GlobalDownloaderID", stmt);
        }
        return ID;
    }

    public static int getPluginID(final String globalEASTWebSchema, final String pluginName, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null || pluginName == null) {
            return -1;
        }

        String selectQuery = String.format("SELECT \"PluginID\" FROM \"%1$s\".\"Plugin\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(pluginName) + "';",
                globalEASTWebSchema
                );
        return getID(selectQuery, "PluginID", stmt);
    }

    public static int getDownloadID(final String globalEASTWebSchema, final Integer globalDownloaderID, final Integer dateGroupID, final Statement stmt) throws ClassNotFoundException, SQLException,
    ParserConfigurationException, SAXException, IOException {
        if(globalEASTWebSchema == null || globalDownloaderID == null || dateGroupID == null) {
            return -1;
        }
        String selectQuery = String.format(
                "SELECT \"DownloadID\" FROM \"%1$s\".\"Download\" WHERE \n" +
                        "\"GlobalDownloaderID\" = " + globalDownloaderID + " AND \"DateGroupID\" = " + dateGroupID + ";",
                        globalEASTWebSchema
                );
        return getID(selectQuery, "DownloadID", stmt);
    }

    public static int getProjectID(final String globalEASTWebSchema, final String projectName, final Statement stmt) throws SQLException
    {
        if(globalEASTWebSchema == null) {
            return -1;
        }
        String selectQuery = String.format("SELECT \"ProjectID\" FROM \"%1$s\".\"Project\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(projectName) + "';",
                globalEASTWebSchema
                );
        return getID(selectQuery, "ProjectID", stmt);
    }

    public static Integer getTemporalSummaryCompositionStrategyID(final String globalEASTWebSchema, final String temporalSummaryCompositionStrategyClassName, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null) {
            return null;
        }

        if(temporalSummaryCompositionStrategyClassName == null) {
            return null;
            //            temporalSummaryCompositionStrategyClassName = "";
        }
        String selectQuery = String.format("SELECT \"TemporalSummaryCompositionStrategyID\" FROM \"%1$s\".\"TemporalSummaryCompositionStrategy\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(temporalSummaryCompositionStrategyClassName) + "';",
                globalEASTWebSchema
                );
        return getID(selectQuery, "TemporalSummaryCompositionStrategyID", stmt);
    }

    public static LocalDate getLatestDateInIndicesCache(final String globalEASTWebSchema, final String projectSchema, Statement stmt) throws SQLException
    {
        Connection con = RDS.getCentralDBconnection();
        Statement stmt1 = con.createStatement();
        LocalDate latest = null;
        /*String selectQuery = String.format("SELECT D.\"Year\", D.\"DayOfYear\" from \"%1$s\".\"IndicesCache\" I "
                + "INNER JOIN \"%2$s\".\"DateGroup\" D ON I.\"DateGroupID\"=D.\"DateGroupID\" "
                + "ORDER BY \"Year\" desc, \"DayOfYear\" desc;",
                projectSchema,
                globalEASTWebSchema
                );*/

        String selectQuery = String.format("SELECT \"DateGroupID\" from \"%1$s\".\"IndicesCache\"; ",
                projectSchema);

        /*String selectQuery1 = String.format("SELECT D.\"Year\", D.\"DayOfYear\", D.\"DateGroupID\" from \"%1$s\".\"DateGroup\" D"
                + "ORDER BY \"Year\" desc, \"DayOfYear\" desc;",
                globalEASTWebSchema);*/


        ResultSet rs = stmt.executeQuery(selectQuery);

        ArrayList<LocalDate> Dates = new ArrayList<LocalDate>();

        if(rs != null){
            while(rs.next()){
                String selectQuery1 = String.format("SELECT \"Year\", \"DayOfYear\", \"DateGroupID\" from \"%1$s\".\"DateGroup\""
                        + " Where \"DateGroupID\"= "+rs.getInt("DateGroupID")
                        + " ORDER BY \"Year\" desc, \"DayOfYear\" desc;",
                        globalEASTWebSchema);
                ResultSet rs1 = stmt1.executeQuery(selectQuery1);
                if (rs1.next()){
                    Dates.add(LocalDate.ofYearDay(rs1.getInt("Year"), rs1.getInt("DayOfYear")));
                }
                rs1.close();
            }
            latest = Dates.get(0);
            rs.close();
        }
        /*if(rs != null && rs1 != null){
            if(rs.next() && rs1.next())
            {
                while(rs.next() && rs1.next()){
                    if (rs.getInt("DateGroupID") == rs1.getInt("DateGroupID")){
                        Dates.add(LocalDate.ofYearDay(rs.getInt("Year"), rs.getInt("DayOfYear")));
                    }
                }
            }
            latest = Dates.get(0);
            rs.close();
            rs1.close();
        }*/
        /*if(rs != null)
        {
            if(rs.next())
            {
                latest = LocalDate.ofYearDay(rs.getInt("Year"), rs.getInt("DayOfYear"));
            }
            rs.close();
        }*/
        return latest;
    }

    /**
     * Adds the given TemporalSummaryCompositionStrategy class name to the list of registered temporal composition strategies if not already registered.
     * @param globalEASTWebSchema
     * @param temporalSummaryCompositionStrategyClassName
     * @param stmt
     * @throws SQLException
     */
    public static void addTemporalSummaryCompositionStrategy(final String globalEASTWebSchema, final String temporalSummaryCompositionStrategyClassName, final Statement stmt) throws SQLException
    {
        if(globalEASTWebSchema == null || temporalSummaryCompositionStrategyClassName == null) {
            return;
        }


        String selectQuery = String.format("SELECT \"TemporalSummaryCompositionStrategyID\" FROM \"%1$s\".\"TemporalSummaryCompositionStrategy\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(temporalSummaryCompositionStrategyClassName) + "';",
                globalEASTWebSchema
                );
        String insertQuery = String.format(
                "INSERT INTO \"%1$s\".\"TemporalSummaryCompositionStrategy\" (\"Name\") VALUES " +
                        "('" + escapeUnderScoresAndPercents(temporalSummaryCompositionStrategyClassName) + "');",
                        globalEASTWebSchema
                );
        addRowIFNotExistent(selectQuery, insertQuery, stmt);
    }

    /**
     * Gets the name of the specified project's database schema. The returned name does not need to be quoted to use in SQL.
     *
     * @param projectName  - project name the schema is for
     * @param pluginName  - plugin name the schema is for
     * @return name of schema within database formatted as seen in database
     */
    public static String getSchemaName(final String projectName, final String pluginName) {
        String name = projectName + "_" + pluginName;
        StringBuilder builder = new StringBuilder();
        int codePointOut, codePointIn;

        for (int index = 0; index < name.length(); index += Character.charCount(codePointIn)) {
            codePointIn = name.codePointAt(index);

            if (codePointIn >= 'A' && codePointIn <= 'Z') {
                // Convert upper-case letters to lower-case
                codePointOut = codePointIn - 'A' + 'a';
            } else if ((codePointIn >= 'a' && codePointIn <= 'z') ||
                    (codePointIn >= '0' && codePointIn <= '9')) {
                // Preserve lower-case letters and digits
                codePointOut = codePointIn;
            } else {
                // Make anything else an underscore
                codePointOut = '_';
            }
            builder.appendCodePoint(codePointOut);
        }
        return FileSystem.StandardizeName(builder.toString());
    }

    public static String escapeUnderScoresAndPercents(String input)
    {
        String output = input.replaceAll("\\\\", "\\\\\\\\");
        output = output.replaceAll("_", "\\\\_");

        return output;
    }

    @SuppressWarnings("unused")
    private static int getFilesPerDay(final String globalEASTWebSchema, final int pluginID, final Statement stmt) throws SQLException
    {
        /*Connection conn = RDS.getCentralDBconnection();
        Statement stmt = conn.createStatement();*/
        int filesPerDay = 0;
        ResultSet rs = stmt.executeQuery("SELECT \"FilesPerDay\" FROM \"" + globalEASTWebSchema + "\".\"Plugin\" WHERE \"PluginID\" = " + pluginID);
        if(rs != null && rs.next())
        {
            filesPerDay = rs.getInt("FilesPerDay");
        }
        rs.close();
        return filesPerDay;
    }

    private static int addPlugin(final String globalEASTWebSchema, final String pluginName, final Integer daysPerInputFile, final Integer filesPerDay, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null || pluginName == null || daysPerInputFile == null) {
            return -1;
        }

        String selectQuery = String.format("SELECT \"PluginID\" FROM \"%1$s\".\"Plugin\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(pluginName) + "';",
                globalEASTWebSchema
                );
        String insertQuery = String.format(
                "INSERT INTO \"%1$s\".\"Plugin\" (\"Name\", \"DaysPerInputFile\", \"FilesPerDay\") VALUES " +
                        "('" + pluginName + "', " + daysPerInputFile + ", " + filesPerDay + ");",
                        globalEASTWebSchema
                );

        return  getOrInsertIfMissingID(selectQuery, "PluginID", insertQuery, stmt);
    }

    private static int addProject(final String globalEASTWebSchema, final String projectName, final Integer dateGroupID, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null || dateGroupID == null || dateGroupID == -1) {
            return -1;
        }

        String selectQuery = String.format("SELECT \"ProjectID\" FROM \"%1$s\".\"Project\" " +
                "WHERE \"Name\" like '" + escapeUnderScoresAndPercents(projectName) + "' AND \"StartDate_DateGroupID\"=" + dateGroupID + ";",
                globalEASTWebSchema
                );
        String insertQuery = String.format(
                "INSERT INTO \"%1$s\".\"Project\" (\"Name\", \"StartDate_DateGroupID\")\n" +
                        "VALUES ('" + projectName + "', " + dateGroupID + ");",
                        globalEASTWebSchema
                );
        return getOrInsertIfMissingID(selectQuery, "ProjectID", insertQuery, stmt);
    }

    private static void createDownloadExpectedTotalOutput(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException
    {
        if(globalEASTWebSchema == null || stmt == null) {
            return;
        }

        String query = "CREATE TABLE IF NOT EXISTS \"" + globalEASTWebSchema + "\".\"DownloadExpectedTotalOutput\" (\n" +
                "   \"DownloadExpectedTotalOutputID\" serial PRIMARY KEY, \n" +
                "   \"DataName\" varchar(255) NOT NULL, \n" +
                "   \"ProjectID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"Project\" (\"ProjectID\") " : "") + "NOT NULL, \n" +
                "   \"PluginID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"Plugin\" (\"PluginID\") " : "") + "NOT NULL, \n" +
                "   \"ExpectedNumOfOutputs\" integer NOT NULL \n" +
                ")";
        stmt.executeUpdate(query);
    }

    private static void createProcessorExpectedTotalOutput(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException
    {
        if(globalEASTWebSchema == null || stmt == null) {
            return;
        }

        String query = "CREATE TABLE IF NOT EXISTS \"" + globalEASTWebSchema + "\".\"ProcessorExpectedTotalOutput\" (\n" +
                "   \"ProcessorExpectedTotalOutputID\" serial PRIMARY KEY, \n" +
                "   \"ProjectID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"Project\" (\"ProjectID\") " : "") + "NOT NULL, \n" +
                "   \"PluginID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"Plugin\" (\"PluginID\") " : "") + "NOT NULL, \n" +
                "   \"ExpectedNumOfOutputs\" integer NOT NULL \n" +
                ")";
        stmt.executeUpdate(query);
    }

    private static void createIndicesExpectedTotalOutput(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException
    {
        if(globalEASTWebSchema == null || stmt == null) {
            return;
        }

        String query = "CREATE TABLE IF NOT EXISTS \"" + globalEASTWebSchema + "\".\"IndicesExpectedTotalOutput\" (\n" +
                "   \"IndicesExpectedTotalOutputID\" serial PRIMARY KEY, \n" +
                "   \"ProjectID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"Project\" (\"ProjectID\") " : "") + "NOT NULL, \n" +
                "   \"PluginID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"Plugin\" (\"PluginID\") " : "") + "NOT NULL, \n" +
                "   \"ExpectedNumOfOutputs\" integer NOT NULL \n" +
                ")";
        stmt.executeUpdate(query);
    }

    private static void createSummaryExpectedTotalOutput(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException
    {
        if(globalEASTWebSchema == null || stmt == null) {
            return;
        }

        String query = "CREATE TABLE IF NOT EXISTS \"" + globalEASTWebSchema + "\".\"SummaryExpectedTotalOutput\" (\n" +
                "   \"SummaryExpectedTotalOutputID\" serial PRIMARY KEY, \n" +
                "   \"ProjectSummaryID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"ProjectSummary\" (\"ProjectSummaryID\") " : "") + "NOT NULL, \n" +
                "   \"PluginID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"" + globalEASTWebSchema + "\".\"Plugin\" (\"PluginID\") " : "") + "NOT NULL, \n" +
                "   \"ExpectedNumOfOutputs\" integer NOT NULL \n" +
                ")";
        stmt.executeUpdate(query);
    }


    //local DB tables
    private static void createIndicesCacheTableIfNotExists(String globalEASTWebSchema, final Statement stmt, final String mSchemaName,
            boolean createTablesWithForeignKeyReferences) throws SQLException {
        if(globalEASTWebSchema == null || mSchemaName == null) {
            return;
        }

        StringBuilder query_;
        /*query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"IndicesCache\"\n" +
                                "(\n" +
                                "  \"IndicesCacheID\" serial PRIMARY KEY,\n" +
                                "  \"DataFilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL,\n" +
                                "  \"IndexID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"Index\" (\"IndexID\") " : "") + "DEFAULT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName,
                                globalEASTWebSchema
                        ));*/
        query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"IndicesCache\"\n" +
                                "(\n" +
                                "  \"IndicesCacheID\" serial PRIMARY KEY,\n" +
                                "  \"DataFilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DateGroupID\" integer NOT NULL,\n" +
                                "  \"IndexID\" integer DEFAULT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName
                        ));
        stmt.executeUpdate(query_.toString());

        ResultSet rs = stmt.executeQuery("SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema='" + mSchemaName + "' and table_name='IndicesCache' and column_name='TimeStamp';");
        if(rs == null || !rs.next())
        {
            stmt.executeUpdate("ALTER TABLE \"" + mSchemaName + "\".\"IndicesCache\" ADD COLUMN \"TimeStamp\" timestamp without time zone;");
        }

        if(rs != null) {
            rs.close();
        }
    }

    private static void createProcessorCacheTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final String mSchemaName, final boolean createTablesWithForeignKeyReferences)
            throws SQLException {
        if(globalEASTWebSchema == null || mSchemaName == null) {
            return;
        }

        StringBuilder query_;
        /*query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"ProcessorCache\"\n" +
                                "(\n" +
                                "  \"ProcessorCacheID\" serial PRIMARY KEY,\n" +
                                "  \"DataFilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName,
                                globalEASTWebSchema
                        ));*/
        query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"ProcessorCache\"\n" +
                                "(\n" +
                                "  \"ProcessorCacheID\" serial PRIMARY KEY,\n" +
                                "  \"DataFilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DateGroupID\" integer NOT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName
                        ));
        stmt.executeUpdate(query_.toString());

        ResultSet rs = stmt.executeQuery("SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema='" + mSchemaName + "' and table_name='ProcessorCache' and column_name='TimeStamp';");
        if(rs == null || !rs.next())
        {
            stmt.executeUpdate("ALTER TABLE \"" + mSchemaName + "\".\"ProcessorCache\" ADD COLUMN \"TimeStamp\" timestamp without time zone;");
        }

        if(rs != null) {
            rs.close();
        }
    }

    private static void createDownloadCacheExtraTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final String mSchemaName, final boolean createTablesWithForeignKeyReferences)
            throws SQLException {
        if(globalEASTWebSchema == null || mSchemaName == null) {
            return;
        }

        StringBuilder query_;
        /*query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"DownloadCacheExtra\"\n" +
                                "(\n" +
                                "  \"DownloadCacheExtraID\" serial PRIMARY KEY,\n" +
                                "  \"DataName\" varchar(255) NOT NULL,\n" +
                                "  \"FilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DownloadExtraID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"DownloadExtra\" (\"DownloadExtraID\") " : "") + "NOT NULL,\n" +
                                "  \"DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL,\n" +
                                "  \"DownloadCacheID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"DownloadCache\" (\"DownloadCacheID\") " : "") + "DEFAULT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"Complete\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName,
                                globalEASTWebSchema
                        ));*/

        query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"DownloadCacheExtra\"\n" +
                                "(\n" +
                                "  \"DownloadCacheExtraID\" serial PRIMARY KEY,\n" +
                                "  \"DataName\" varchar(255) NOT NULL,\n" +
                                "  \"FilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DownloadExtraID\" integer NOT NULL,\n" +
                                "  \"DateGroupID\" integer NOT NULL,\n" +
                                "  \"DownloadCacheID\" integer DEFAULT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"Complete\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName
                        ));
        stmt.executeUpdate(query_.toString());

        ResultSet rs = stmt.executeQuery("SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema='" + mSchemaName + "' and table_name='DownloadCacheExtra' and column_name='TimeStamp';");
        if(rs == null || !rs.next())
        {
            stmt.executeUpdate("ALTER TABLE \"" + mSchemaName + "\".\"DownloadCacheExtra\" ADD COLUMN \"TimeStamp\" timestamp without time zone;");
        }

        if(rs != null) {
            rs.close();
        }
    }

    private static void createDownloadCacheTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final String mSchemaName, boolean createTablesWithForeignKeyReferences) throws SQLException {
        if(globalEASTWebSchema == null || mSchemaName == null) {
            return;
        }

        StringBuilder query_;
        /*query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"DownloadCache\"\n" +
                                "(\n" +
                                "  \"DownloadCacheID\" serial PRIMARY KEY,\n" +
                                "  \"DataFilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DownloadID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"Download\" (\"DownloadID\") " : "") + "NOT NULL,\n" +
                                "  \"DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"Complete\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName,
                                globalEASTWebSchema
                        ));*/

        query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"DownloadCache\"\n" +
                                "(\n" +
                                "  \"DownloadCacheID\" serial PRIMARY KEY,\n" +
                                "  \"DataFilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"DownloadID\" integer NOT NULL,\n" +
                                "  \"DateGroupID\" integer NOT NULL,\n" +
                                "  \"Retrieved\" boolean DEFAULT FALSE,\n" +
                                "  \"Processed\" boolean DEFAULT FALSE,\n" +
                                "  \"Complete\" boolean DEFAULT FALSE,\n" +
                                "  \"TimeStamp\" timestamp without time zone\n" +
                                ")",
                                mSchemaName
                        ));
        stmt.executeUpdate(query_.toString());

        ResultSet rs = stmt.executeQuery("SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema='" + mSchemaName + "' and table_name='DownloadCache' and column_name='TimeStamp';");
        if(rs == null || !rs.next())
        {
            stmt.executeUpdate("ALTER TABLE \"" + mSchemaName + "\".\"DownloadCache\" ADD COLUMN \"TimeStamp\" timestamp without time zone;");
        }

        if(rs != null) {
            rs.close();
        }
    }

    private static void createZonalStatTableIfNotExists(final String globalEASTWebSchema, final String mSchemaName, final ArrayList<String> summaryNames, final Statement stmt,
            final boolean createTablesWithForeignKeyReferences) throws SQLException {
        if(globalEASTWebSchema == null || mSchemaName == null || summaryNames == null) {
            return;
        }

        StringBuilder query_;
        /*query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"ZonalStat\"\n" +
                                "(\n" +
                                "  \"ZonalStatID\" serial PRIMARY KEY,\n" +
                                "  \"ProjectSummaryID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"ProjectSummary\" (\"ProjectSummaryID\") " : "") + "NOT NULL,\n" +
                                "  \"DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL,\n" +
                                "  \"IndexID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%2$s\".\"Index\" (\"IndexID\") " : "") + "NOT NULL,\n" +
                                "  \"AreaCode\" integer NOT NULL,\n" +
                                "  \"AreaName\" varchar(255) NOT NULL,\n" +
                                "  \"FilePath\" varchar(255) NOT NULL",
                                mSchemaName,
                                globalEASTWebSchema
                        ));*/

        query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"ZonalStat\"\n" +
                                "(\n" +
                                "  \"ZonalStatID\" serial PRIMARY KEY,\n" +
                                "  \"ProjectSummaryID\" integer NOT NULL,\n" +
                                "  \"DateGroupID\" integer NOT NULL,\n" +
                                "  \"IndexID\" integer NOT NULL,\n" +
                                "  \"AreaCode\" integer NOT NULL,\n" +
                                "  \"AreaName\" varchar(255) NOT NULL,\n" +
                                "  \"FilePath\" varchar(255) NOT NULL",
                                mSchemaName
                        ));
        for(String summary : summaryNames)
        {
            query_.append(",\n  \"" + summary + "\" double precision");
        }
        query_.append(",\n\"TimeStamp\" timestamp without time zone);");
        stmt.executeUpdate(query_.toString());

        ResultSet rs = stmt.executeQuery("SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema='" + mSchemaName + "' and table_name='ZonalStat' and column_name='TimeStamp';");
        if(rs == null || !rs.next() || rs.getString("column_name").equals(""))
        {
            stmt.executeUpdate("ALTER TABLE \"" + mSchemaName + "\".\"ZonalStat\" ADD COLUMN \"TimeStamp\" timestamp without time zone;");
        }

        if(rs != null) {
            rs.close();
        }
    }

    private static void createIndexTableIfNotExists(final String globalEASTWebSchema, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null) {
            return;
        }

        String query;
        query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"Index\"\n" +
                        "(\n" +
                        "  \"IndexID\" serial PRIMARY KEY,\n" +
                        "  \"Name\" varchar(255) UNIQUE NOT NULL\n" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createDownloadExtraTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException
    {
        if(globalEASTWebSchema == null) {
            return;
        }

        String query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"DownloadExtra\"\n" +
                        "(\n" +
                        "  \"DownloadExtraID\" serial PRIMARY KEY,\n" +
                        "  \"DownloadID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"Download\" (\"DownloadID\") " : "") + "DEFAULT NULL,\n" +
                        "  \"GlobalDownloaderID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"GlobalDownloader\" (\"GlobalDownloaderID\") " : "") + "NOT NULL,\n" +
                        "  \"DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL,\n" +
                        "  \"DataName\" varchar(255) NOT NULL,\n" +
                        "  \"FilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                        "  \"Complete\" boolean DEFAULT FALSE\n" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createDownloadTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences)
            throws SQLException {
        if(globalEASTWebSchema == null) {
            return;
        }

        StringBuilder query_ = new StringBuilder(
                String.format(
                        "CREATE TABLE IF NOT EXISTS \"%1$s\".\"Download\"\n" +
                                "(\n" +
                                "  \"DownloadID\" serial PRIMARY KEY,\n" +
                                "  \"GlobalDownloaderID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"GlobalDownloader\" (\"GlobalDownloaderID\") " : "") + "NOT NULL,\n" +
                                "  \"DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL,\n" +
                                "  \"DataFilePath\" varchar(255) UNIQUE NOT NULL,\n" +
                                "  \"Complete\" boolean DEFAULT FALSE\n" +
                                ")",
                                globalEASTWebSchema
                        ));
        stmt.executeUpdate(query_.toString());
    }

    private static void createGlobalDownloaderTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException {
        if(globalEASTWebSchema == null) {
            return;
        }

        String query;
        query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"GlobalDownloader\"\n" +
                        "(\n" +
                        "  \"GlobalDownloaderID\" serial PRIMARY KEY,\n" +
                        "  \"PluginID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"Plugin\" (\"PluginID\") " : "") + "NOT NULL\n," +
                        "  \"DataName\" varchar(255) NOT NULL\n" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createProjectSummaryTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException
    {
        if(globalEASTWebSchema == null || stmt == null) {
            return;
        }
        String query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"ProjectSummary\" (\n" +
                        "  \"ProjectSummaryID\" serial PRIMARY KEY,\n" +
                        "  \"ProjectID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"Project\" (\"ProjectID\") " : "") + "NOT NULL,\n" +
                        "  \"SummaryIDNum\" integer NOT NULL,\n" +
                        "  \"AreaNameField\" varchar(255) NOT NULL,\n" +
                        "  \"ShapeFile\" varchar(255) NOT NULL,\n" +
                        "  \"AreaCodeField\" varchar(255) NOT NULL,\n" +
                        "  \"TemporalSummaryCompositionStrategyID\" integer "
                        + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"TemporalSummaryCompositionStrategy\" (\"TemporalSummaryCompositionStrategyID\") " : "") + "\n" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createTemporalSummaryCompositionStrategyTableIfNotExists(final String globalEASTWebSchema, final Statement stmt) throws SQLException
    {
        if(globalEASTWebSchema == null) {
            return;
        }

        String query;
        query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"TemporalSummaryCompositionStrategy\"\n" +
                        "(\n" +
                        "  \"TemporalSummaryCompositionStrategyID\" serial PRIMARY KEY,\n" +
                        "  \"Name\" varchar(255) UNIQUE NOT NULL\n" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createProjectTableIfNotExists(final String globalEASTWebSchema, final Statement stmt, final boolean createTablesWithForeignKeyReferences) throws SQLException {
        if(globalEASTWebSchema == null) {
            return;
        }

        String query;
        query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"Project\"\n" +
                        "(\n" +
                        "  \"ProjectID\" serial PRIMARY KEY,\n" +
                        "  \"Name\" varchar(255) UNIQUE NOT NULL,\n" +
                        // Represents the project's start date
                        "  \"StartDate_DateGroupID\" integer " + (createTablesWithForeignKeyReferences ? "REFERENCES \"%1$s\".\"DateGroup\" (\"DateGroupID\") " : "") + "NOT NULL\n" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createPluginTableIfNotExists(final String globalEASTWebSchema, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null) {
            return;
        }

        String query;
        query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"Plugin\"\n" +
                        "(\n" +
                        "  \"PluginID\" serial PRIMARY KEY,\n" +
                        "  \"Name\" varchar(255) UNIQUE NOT NULL,\n" +
                        "  \"DaysPerInputFile\" integer NOT NULL,\n" +
                        "  \"FilesPerDay\" integer NOT NULL" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createDateGroupTable(final String globalEASTWebSchema, final Statement stmt) throws SQLException {
        if(globalEASTWebSchema == null) {
            return;
        }

        String query;
        query = String.format(
                "CREATE TABLE IF NOT EXISTS \"%1$s\".\"DateGroup\"\n" +
                        "(\n" +
                        "  \"DateGroupID\" serial PRIMARY KEY,\n" +
                        "  \"Year\" integer NOT NULL,\n" +
                        "  \"DayOfYear\" integer NOT NULL\n" +
                        ")",
                        globalEASTWebSchema
                );
        stmt.executeUpdate(query);
    }

    private static void createSchemaIfNotExist(final String mSchemaName, final Statement stmt) throws SQLException {
        if(mSchemaName == null) {
            return;
        }

        String query;
        query = String.format(
                "CREATE SCHEMA IF NOT EXISTS \"%s\"",
                mSchemaName
                );
        stmt.executeUpdate(query);
    }

    // @SuppressWarnings("unused")
    public static void dropSchemaIfExists(final String mSchemaName, final Statement stmt) throws SQLException {
        if(mSchemaName == null) {
            System.out.println("Schema name is null");
            return;
        }
        System.out.println("processing droping");
        String query;
        query = String.format(
                "DROP SCHEMA IF EXISTS \"%s\" CASCADE;",
                mSchemaName
                );
        System.out.println("before Executing");
        stmt.executeUpdate(query);
        System.out.println("Drop schema successfuly ");
        return;
    }

    private static int getID(final String selectQuery, final String IDField, final Statement stmt) throws SQLException
    {
        int ID = -1;

        ResultSet rs = stmt.executeQuery(selectQuery);
        if(rs != null && rs.next())
        {
            ID = rs.getInt(IDField);
            rs.close();
        }

        return ID;
    }

    private static int getOrInsertIfMissingID(final String selectQuery, final String IDField, final String insertQuery, final Statement stmt) throws SQLException
    {
        int ID = -1;

        ResultSet rs = stmt.executeQuery(selectQuery);
        if(rs != null && rs.next())
        {
            ID = rs.getInt(IDField);
            rs.close();
        }
        else
        {
            stmt.execute(insertQuery);
            rs = stmt.executeQuery(selectQuery);
            if(rs != null && rs.next())
            {
                //IsUpdated = true;
                ID = rs.getInt(IDField);
                rs.close();
            }
        }

        return ID;
    }

    /**
     * Adds the row via the insertQuery if it doesn't exist according to a check with the given selectQuery.
     * @param selectQuery
     * @param insertQuery
     * @param stmt
     * @return TRUE if a new row is added or pre-existing, otherwise FALSE if failure.
     * @throws SQLException
     */
    private static boolean addRowIFNotExistent(final String selectQuery, final String insertQuery, final Statement stmt) throws SQLException
    {
        ResultSet rs = stmt.executeQuery(selectQuery);
        if(rs != null && rs.next())
        {
            rs.close();
            return true;
        }
        else
        {
            if(stmt.executeUpdate(insertQuery) > 0) {
                //IsUpdated = true;
                return true;

            } else {
                return false;
            }
        }
    }
}
