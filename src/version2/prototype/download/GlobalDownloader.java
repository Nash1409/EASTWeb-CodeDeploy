package version2.prototype.download;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.TaskState;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.Schemas;


/**
 * @author michael.devos
 *
 */
public abstract class GlobalDownloader extends Observable implements Runnable{
    /**
     * This GlobalDownloader's assigned unique ID. Only expected to be unique when compared to other currently existing GlobalDownloaders.
     */
    public final int ID;
    /**
     * The loaded config data.
     */
    public final Config configInstance;
    /**
     * The plugin name of the associated plugin metadata this GlobalDownloader uses.
     */
    public final String pluginName;
    /**
     * The associated download metadata element loaded.
     */
    public final DownloadMetaData metaData;
    /**
     * The ListDatesFiles object used for getting the list of server files to download.
     */
    public final ListDatesFiles listDatesFiles;
    protected TaskState state;
    protected LocalDate currentStartDate;
    protected LocalDate endDate;
    protected Boolean isRegistered;

    /**
     * Sets this GlobalDownloader super to have an initial state (TaskState) of STOPPED and registers the GlobalDownloader in the database.
     *
     * @param myID  - unique identifier ID of this GlobalDownloader
     * @param pluginName
     * @param metaData
     * @param listDatesFiles
     * @throws SQLException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws ClassNotFoundException
     * @throws RegistrationException
     */
    protected GlobalDownloader(int myID, Config configInstance, String pluginName, DownloadMetaData metaData, ListDatesFiles listDatesFiles, LocalDate startDate, LocalDate endDate) throws
    ClassNotFoundException, ParserConfigurationException, SAXException, IOException, SQLException, RegistrationException
    {
        state = TaskState.STOPPED;
        ID = myID;
        this.configInstance = configInstance;
        this.pluginName = pluginName;
        this.metaData = metaData;
        this.listDatesFiles = listDatesFiles;
        currentStartDate = startDate;
        this.endDate = endDate;
        isRegistered = false;
    }

    /**
     * Sets this GlobalDownloader instance running state to STOPPED.
     */
    public final void Stop()
    {
        synchronized(state) {
            state = TaskState.STOPPED;
        }
    }

    /**
     * Sets this GlobalDownloader instance running state to STARTED.
     */
    public final void Start()
    {
        synchronized(state) {
            state = TaskState.STARTED;
        }
    }

    public void setStatetoActive(){
        synchronized(state) {
            state = TaskState.STARTED;
        }
    }
    /**
     * Gets this GlobalDownloader instance's running state.
     *
     * @return TaskState indicating current state of this GlobalDownloader's running status
     */
    public final TaskState GetRunningState()
    {
        TaskState myState;
        synchronized(state) {
            myState = state;
        }
        return myState;
    }

    /**
     * Checks for any new data that's ready to be loaded by a LocalDownlader, flags them, and then sends notifications of those updates, if any, to observing LocalDownloaders.
     * This will all be done on the calling thread including the work that is done by the LocalDownloaders to perform these updates.
     *
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws RegistrationException
     */
    public void SetCompleted() throws ClassNotFoundException, SQLException, ParserConfigurationException, SAXException, IOException, RegistrationException
    {
        //DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        Connection con = RDS.getCentralDBconnection();
        if(con == null) {
            return;
        }
        Statement stmt = con.createStatement();
        SetCompleted(stmt);
        stmt.close();
        con.close();
    }

    /**
     * Gets the date this GlobalDownloader will/has started downloading from.
     *
     * @return start date for downloading
     */
    public final LocalDate GetStartDate()
    {
        LocalDate myCurrentStartDate;
        synchronized(currentStartDate) {
            myCurrentStartDate = currentStartDate;
        }
        return myCurrentStartDate;
    }

    public final LocalDate GetEndDate()
    {
        LocalDate myCurrentEndDate;
        synchronized(endDate) {
            myCurrentEndDate = endDate;
        }
        return myCurrentEndDate;
    }

    /**
     * Changes the start date for this GlobalDownloader and causes it to start downloading from the given date. Does not cause the GlobalDownloader to redownload anything already
     * downloaded but if the date is earlier than the current start date then it will start downloading from that date onward with the next set of downloads until caught up, or if
     * it's later than the current start date then it is simply ignored and the original start date is kept.
     *
     * @param newStartDate  - new date to state downloading from
     */
    public final void SetStartDate(LocalDate newStartDate)
    {
        synchronized(currentStartDate) {
            if(currentStartDate.isAfter(newStartDate)) {
                currentStartDate = newStartDate;
            }
        }
    }

    public final void SetEndDate(LocalDate newEndDate)
    {
        synchronized(endDate) {
            if(endDate.isAfter(newEndDate)) {
                endDate = newEndDate;
            }
        }
    }
    /**
     * Gets all the current Download table entries for this GlobalDownloader. Represents all the dates and files downloaded for this plugin global downloader shareable across all
     * projects.
     *
     * @return list of all Download table entries for the plugin name associated with this GlobalDownloader instance
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws RegistrationException
     */
    public final ArrayList<DataFileMetaData> GetAllDownloadedFiles() throws ClassNotFoundException, SQLException, ParserConfigurationException, SAXException, IOException, RegistrationException
    {
        return GetAllDownloadedFiles(null,null);
    }

    /**
     * Gets all the current Download table entries for this GlobalDownloader. Represents all the dates and files downloaded for this plugin global downloader shareable across all
     * projects.
     *
     * @param startDate  - the earliest date from which to start getting files
     * @return list of all Download table entries for the plugin name associated with this GlobalDownloader instance
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws RegistrationException
     */
    public ArrayList<DataFileMetaData> GetAllDownloadedFiles(LocalDate startDate, LocalDate endDate) throws SQLException, ClassNotFoundException, ParserConfigurationException, SAXException,
    IOException, RegistrationException
    {
        if(!isRegistered) {
            RegisterGlobalDownloader();
        }
        //DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        Connection con = RDS.getCentralDBconnection();
        if(con == null) {
            return new ArrayList<DataFileMetaData>();
        }
        Statement stmt = con.createStatement();
        final int gdlID = Schemas.getGlobalDownloaderID(configInstance.getGlobalSchema(), pluginName, metaData.name, stmt);
        TreeSet<Record> downloadsSet = new TreeSet<Record>();
        int dateGroupID;
        ResultSet rs;

        StringBuilder query = new StringBuilder(String.format(
                "SELECT A.\"DateGroupID\", A.\"DataFilePath\", B.\"Year\", B.\"DayOfYear\" " +
                        "FROM \"%1$s\".\"Download\" A INNER JOIN \"%1$s\".\"DateGroup\" B ON A.\"DateGroupID\"=B.\"DateGroupID\" " +
                        "WHERE A.\"GlobalDownloaderID\"=" + gdlID,
                        configInstance.getGlobalSchema()
                ));
        if(startDate != null)
        {
            int year = startDate.getYear();
            int dayOfYear = startDate.getDayOfYear();
            query.append(" AND ((B.\"Year\" = " + year + " AND B.\"DayOfYear\" >= " + dayOfYear + ") OR (B.\"Year\" > " + year + "))");
            query.append(" AND ((B.\"Year\" = " + endDate.getYear() + " AND B.\"DayOfYear\" <= " + endDate.getDayOfYear() + ") OR (B.\"Year\" < " + endDate.getYear() + "))");
        }
        query.append(";");
        rs = stmt.executeQuery(query.toString());
        if(rs != null)
        {
            while(rs.next())
            {
                dateGroupID = rs.getInt("DateGroupID");
                if(!downloadsSet.add(new Record(dateGroupID, "Data", new DataFileMetaData("Data", rs.getString("DataFilePath"), dateGroupID, rs.getInt("Year"), rs.getInt("DayOfYear"))))) {
                    ErrorLog.add(configInstance, pluginName, "Data", "Problem adding download file to download file return list.", new Exception("Element could not be added."));
                }
            }
        }
        rs.close();

        query = new StringBuilder(String.format(
                "SELECT A.\"DataName\", A.\"FilePath\", A.\"DateGroupID\", B.\"Year\", B.\"DayOfYear\" " +
                        "FROM \"%1$s\".\"DownloadExtra\" A INNER JOIN \"%1$s\".\"DateGroup\" B ON A.\"DateGroupID\"=B.\"DateGroupID\" " +
                        "WHERE A.\"GlobalDownloaderID\"=" + gdlID,
                        configInstance.getGlobalSchema()));
        if(startDate != null)
        {
            int year = startDate.getYear();
            int dayOfYear = startDate.getDayOfYear();
            query.append(" AND ((B.\"Year\" = " + year + " AND B.\"DayOfYear\" >= " + dayOfYear + ") OR (B.\"Year\" > " + year + "))");
            query.append(" AND ((B.\"Year\" = " + endDate.getYear() + " AND B.\"DayOfYear\" <= " + endDate.getDayOfYear() + ") OR (B.\"Year\" < " + endDate.getYear() + "))");
        }
        query.append(";");
        rs = stmt.executeQuery(query.toString());
        if(rs != null)
        {
            String dataName;
            while(rs.next())
            {
                dataName = rs.getString("DataName");
                dateGroupID = rs.getInt("DateGroupID");
                if(!downloadsSet.add(new Record(dateGroupID, dataName, new DataFileMetaData(dataName, rs.getString("FilePath"), dateGroupID, rs.getInt("Year"), rs.getInt("DayOfYear"))))) {
                    ErrorLog.add(configInstance, pluginName, dataName, "Problem adding download file to download file return list.", new Exception("Element could not be added."));
                }
            }
        }
        rs.close();
        stmt.close();
        con.close();

        ArrayList<DataFileMetaData> output = new ArrayList<DataFileMetaData>();
        for(Record rec : downloadsSet)
        {
            output.add(rec.data);
        }
        return output;
    }

    /**
     * Registers the GlobalDownloader in the system.
     * @throws SQLException
     * @throws RegistrationException
     */
    public void RegisterGlobalDownloader() throws SQLException, RegistrationException
    {
        if(isRegistered) {
            return;
        }
        synchronized(isRegistered) {
            //DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
            Connection con = RDS.getCentralDBconnection();
            if(con == null) {
                return;
            }
            Statement stmt = con.createStatement();
            boolean registered = Schemas.registerGlobalDownloader(configInstance.getGlobalSchema(), pluginName, metaData.name, stmt);
            if(!registered) {
                throw new RegistrationException();
            }
            stmt.close();
            con.close();
            isRegistered = true;
        }
    }

    /**
     * Adds the given file to the database records and updates statuses for observing LocalDownloaders.
     *
     * @param con
     * @param year  - year the file's data is associated with
     * @param dayOfYear  - day of the year the file's data is associated with
     * @param filePath  - full path to the file
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws RegistrationException
     */
    protected void AddDownloadFile(Statement stmt, int year, int dayOfYear, String filePath) throws ClassNotFoundException, SQLException, ParserConfigurationException, SAXException, IOException, RegistrationException {
        final LocalDate lDate = LocalDate.ofYearDay(year, dayOfYear);
        final String fileName = filePath.substring((filePath.lastIndexOf("/") > -1 ? filePath.lastIndexOf("/") + 1 : filePath.lastIndexOf("\\") + 1));

        if(!isRegistered) {
            RegisterGlobalDownloader();
        }
        System.out.println("[GDL " + ID + " on Thread " + Thread.currentThread().getId() + "] Adding download file '" + fileName + "' for day " + dayOfYear + " of " + year
                + " for plugin '" + pluginName + "'.");
        // If inserting a "Data" labeled file then insert its record into the 'Download' table
        if(metaData.name.toLowerCase().equals("data"))
        {
            //Schemas.IsCreated = true;
            int gdlID = Schemas.getGlobalDownloaderID(configInstance.getGlobalSchema(), pluginName, metaData.name, stmt);
            int dateGroupID = Schemas.getDateGroupID(configInstance.getGlobalSchema(), lDate, stmt);

            // Insert new download
            String query = String.format(
                    "INSERT INTO \"%1$s\".\"Download\" (\"GlobalDownloaderID\", \"DateGroupID\", \"DataFilePath\") VALUES\n" +
                            "(" + gdlID + ", " + dateGroupID + ", '" + filePath + "');",
                            configInstance.getGlobalSchema()
                    );
            stmt.executeUpdate(query);
            //stmt1.executeUpdate(query_);
        }
        // Else, insert the record into the 'ExtraDownload' table
        else
        {
            //Schemas.IsCreated = true;
            int gdlID = Schemas.getGlobalDownloaderID(configInstance.getGlobalSchema(), pluginName, metaData.name, stmt);
            int dateGroupID = Schemas.getDateGroupID(configInstance.getGlobalSchema(), lDate, stmt);

            //            int downloadID = Schemas.getDownloadID(globalSchema, gdlID, dateGroupID, stmt);

            String query = String.format(
                    "INSERT INTO \"%1$s\".\"DownloadExtra\" (\"GlobalDownloaderID\", \"DateGroupID\", \"DataName\", \"FilePath\") VALUES\n" +
                            "(" + gdlID + ", " + dateGroupID + ", '" + metaData.name + "', '" + filePath + "');",
                            configInstance.getGlobalSchema()
                    );

            stmt.executeUpdate(query);
            //stmt1.executeUpdate(query_);
        }

        SetCompleted(stmt);
        // SetCompleted(stmt1);
        // conn.close();
    }

    protected void SetCompleted(Statement stmt) throws ClassNotFoundException, SQLException, ParserConfigurationException, SAXException, IOException, RegistrationException
    {
        /*Connection con = RDS.getCentralDBconnection();
        Statement stmt1 = con.createStatement();*/
        if(!isRegistered) {
            RegisterGlobalDownloader();
        }
        final int gdlID = Schemas.getGlobalDownloaderID(configInstance.getGlobalSchema(), pluginName, metaData.name, stmt);
        int filesPerDay = metaData.filesPerDay;
        ArrayList<Integer> datesCompleted = new ArrayList<Integer>();
        Map<Integer, Integer> countOfDates = new TreeMap<Integer, Integer>();
        ResultSet rs = null;
        StringBuilder update;

        // Check if all files downloaded for any additional days
        rs = stmt.executeQuery("SELECT \"DateGroupID\", COUNT(\"DateGroupID\") AS \"DateGroupIDCount\" FROM \"" + configInstance.getGlobalSchema() + "\".\"Download\" " +
                "WHERE \"GlobalDownloaderID\" = " + gdlID + " AND \"Complete\" = FALSE " +
                "GROUP BY \"DateGroupID\";");
        if(rs != null)
        {
            while(rs.next())
            {
                countOfDates.put(rs.getInt("DateGroupID"), rs.getInt("DateGroupIDCount"));
            }
        }
        rs.close();
        rs = stmt.executeQuery("SELECT A.\"DateGroupID\", COUNT(A.\"DateGroupID\") AS \"DateGroupIDCount\" FROM \"" + configInstance.getGlobalSchema() + "\".\"DownloadExtra\" A " +
                "WHERE A.\"GlobalDownloaderID\" = " + gdlID + " AND A.\"Complete\" = FALSE " +
                "GROUP BY \"DateGroupID\";");
        if(rs != null)
        {
            while(rs.next())
            {
                countOfDates.put(rs.getInt("DateGroupID"), countOfDates.get(rs.getInt("DateGroupID")) + rs.getInt("DateGroupIDCount"));
            }
        }
        rs.close();

        Iterator<Integer> iterator = countOfDates.keySet().iterator();
        int idx;
        while(iterator.hasNext())
        {
            idx = iterator.next();
            if(countOfDates.get(idx) == filesPerDay)
            {
                datesCompleted.add(idx);
            }
        }

        if(datesCompleted.size() > 0)
        {
            StringBuilder dateGroups = new StringBuilder("\"DateGroupID\" = " + datesCompleted.get(0));
            for(int i=1; i < datesCompleted.size(); i++)
            {
                dateGroups.append(" OR \"DateGroupID\" = " + datesCompleted.get(i));
            }

            try{
                stmt.execute("BEGIN");

                update = new StringBuilder("UPDATE \"" + configInstance.getGlobalSchema() + "\".\"Download\" SET \"Complete\" = TRUE WHERE " + dateGroups.toString() + ";");
                stmt.executeUpdate(update.toString());

                update = new StringBuilder("UPDATE \"" + configInstance.getGlobalSchema() + "\".\"DownloadExtra\" SET \"Complete\" = TRUE WHERE " + dateGroups.toString() + ";");
                stmt.executeUpdate(update.toString());

                stmt.execute("COMMIT");
            }
            catch(SQLException e)
            {
                stmt.execute("ROLLBACK");
                if(rs != null) {
                    rs.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
                throw e;
            }

            if(!Thread.currentThread().isInterrupted()) {
                setChanged();
                notifyObservers();
            }
        }

        if(rs != null) {
            rs.close();
        }
    }

    protected class Record implements Comparable<Record>
    {
        public final int dateGroupID;
        public final String dataName;
        public final DataFileMetaData data;

        public Record(int dateGroupID, String dataName, DataFileMetaData data)
        {
            this.dateGroupID = dateGroupID;
            this.dataName = dataName;
            this.data = data;
        }

        @Override
        public int compareTo(Record o) {
            if(dateGroupID < o.dateGroupID) {
                return -1;
            } else if(dateGroupID > o.dateGroupID) {
                return 1;
            }

            if(dataName.equals(o.dataName)) {
                return data.ReadMetaDataForProcessor().dataFilePath.compareTo(o.data.ReadMetaDataForProcessor().dataFilePath);
            } else if(dataName.equalsIgnoreCase("Data")) {
                return -1;
            } else {
                return dataName.compareTo(o.dataName);
            }
        }
    }
}
