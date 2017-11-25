/**
 *
 */
package version2.prototype.download.ModisDownloadUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.TaskState;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.download.DownloaderFramework;
import version2.prototype.download.GlobalDownloader;
import version2.prototype.download.ListDatesFiles;
import version2.prototype.download.RegistrationException;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.DownloadFileMetaData;
import version2.prototype.util.FileSystem;
import version2.prototype.util.Schemas;

/**
 * @author michael.devos
 *
 */
public class ModisLocalStorageGlobalDownloader extends GlobalDownloader {
    private Constructor<?> downloadCtr;
    private static ArrayList<String> modisTiles = new ArrayList<String>();

    /**
     * @param myID
     * @param configInstance
     * @param projectMetaData
     * @param pluginName
     * @param metaData
     * @param listDatesFiles
     * @param startDate
     * @param downloaderClassName
     * @throws ClassNotFoundException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws SQLException
     * @throws RegistrationException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public ModisLocalStorageGlobalDownloader(int myID, Config configInstance, ProjectInfoFile projectMetaData, String pluginName, DownloadMetaData metaData, ListDatesFiles listDatesFiles, LocalDate startDate,
            LocalDate endDate, String downloaderClassName) throws ClassNotFoundException, ParserConfigurationException, SAXException, IOException, SQLException, RegistrationException, NoSuchMethodException, SecurityException
    {
        super(myID, configInstance, pluginName, metaData, listDatesFiles, startDate, endDate);
        Class<?> downloaderClass = Class.forName("version2.prototype.download." + pluginName + "." + downloaderClassName);
        downloadCtr = downloaderClass.getConstructor(DataDate.class, String.class, DownloadMetaData.class, String.class);

        ArrayList<String> tempTiles = new ArrayList<String>();
        for(ProjectInfoPlugin pluginInfo : projectMetaData.GetPlugins())
        {
            if(pluginInfo.GetName().equals(pluginName))
            {
                tempTiles = pluginInfo.GetModisTiles();
                break;
            }
        }
        for(String tile : tempTiles)
        {
            if(!modisTiles.contains(tile))
            {
                modisTiles.add(tile);
            }
        }
    }

    @Override
    public void SetCompleted(Statement stmt1) throws ClassNotFoundException, SQLException, ParserConfigurationException, SAXException, IOException
    {
        Connection con = RDS.getCentralDBconnection();
        Statement stmt = con.createStatement();

        final int gdlID = Schemas.getGlobalDownloaderID(configInstance.getGlobalSchema(), pluginName, metaData.name, stmt);
        //        int filesPerDay = metaData.filesPerDay;
        int filesPerDay = modisTiles.size();
        int dateGroupID;
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
                dateGroupID = rs.getInt("DateGroupID");
                countOfDates.put(dateGroupID, (countOfDates.get(dateGroupID) != null ? countOfDates.get(dateGroupID) : 0) + rs.getInt("DateGroupIDCount"));
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
                throw e;
            }

            setChanged();
            notifyObservers();
        }

        if(rs != null) {
            rs.close();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        try {
            // DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
            Connection con = RDS.getCentralDBconnection();
            if(con == null) {
                return;
            }
            Statement stmt = con.createStatement();
            System.out.println("[GDL " + ID + " on Thread " + Thread.currentThread().getId() + "] GlobalDownloader of '" + metaData.name + "' files for plugin '"
                    + pluginName + "' starting from " + currentStartDate + ".");
            System.out.println("Running: " + downloadCtr.getName().substring((downloadCtr.getName().lastIndexOf(".") > -1 ? downloadCtr.getName().lastIndexOf(".") + 1 : 0)));

            // Step 1: Get all downloads from ListDatesFiles and remove unneeded modis tile files
            Map<DataDate, ArrayList<String>> originalDatesFiles = listDatesFiles.CloneListDatesFiles();
            Iterator<DataDate> keysIt = originalDatesFiles.keySet().iterator();
            ArrayList<String> daysTemp;
            Map<DataDate, ArrayList<String>> datesFiles = new HashMap<DataDate, ArrayList<String>>();
            DataDate tempKey;
            String tempFilePath;
            while(keysIt.hasNext())
            {
                tempKey = keysIt.next();
                daysTemp = originalDatesFiles.get(tempKey);
                for(int i=0; i < daysTemp.size(); i++)
                {
                    tempFilePath = daysTemp.get(i);
                    for(String tile : modisTiles)
                    {
                        if(tempFilePath.contains(tile))
                        {
                            if(datesFiles.get(tempKey) != null) {
                                datesFiles.get(tempKey).add(tempFilePath);
                            } else {
                                ArrayList<String> newDatesFilesList = new ArrayList<String>();
                                newDatesFilesList.add(tempFilePath);
                                datesFiles.put(tempKey, newDatesFilesList);
                            }
                            break;
                        }
                    }
                }
            }
            originalDatesFiles.clear();
            originalDatesFiles = null;

            //            Map<DataDate, ArrayList<String>> datesFiles = new TreeMap<DataDate, ArrayList<String>>(filesTemp);

            // Remove from downloads list any which are before the current start date
            Set<DataDate> dateKeys = datesFiles.keySet();
            ArrayList <String> files;
            Iterator<String> fIter;
            ArrayList<DataDate> removeDates = new ArrayList<DataDate>();
            for(DataDate dd : dateKeys)
            {
                if(dd.getLocalDate().isBefore(currentStartDate))
                {
                    removeDates.add(dd);
                }
            }
            for(DataDate dd : removeDates) {
                datesFiles.remove(dd);
            }

            // Step 2: Pull all cached downloads
            ArrayList<DataFileMetaData> cachedD = GetAllDownloadedFiles(currentStartDate,endDate);

            // Step 3: Remove already downloaded files from ListDatesFiles
            for (DataFileMetaData d: cachedD)
            {
                DownloadFileMetaData downloaded =  d.ReadMetaDataForProcessor();

                // get the year and dayOfyear from each downloaded file
                DataDate thisDate = new DataDate(downloaded.day, downloaded.year);

                // get the files associated with the date in the ListDatesFiles
                files = datesFiles.get(thisDate);

                if(files != null)
                {
                    fIter = files.iterator();

                    String fileTemp;
                    while (fIter.hasNext())
                    {
                        String strPath = downloaded.dataFilePath;
                        //                        System.out.println(strPath);
                        strPath = strPath.substring(strPath.lastIndexOf(File.separator)+1, strPath.lastIndexOf("."));
                        // remove the file if it is found in the downloaded list
                        fileTemp = fIter.next();
                        if ((fileTemp.toLowerCase()).contains((strPath.toLowerCase())))
                        {
                            fIter.remove();
                        }
                    }

                    datesFiles.put(thisDate, files);
                }
            }

            // Step 4: Create downloader and run downloader for all that's left
            downloadLoop: for(Map.Entry<DataDate, ArrayList<String>> entry : datesFiles.entrySet())
            {
                String outFolder = FileSystem.GetGlobalDownloadDirectory(configInstance, pluginName, metaData.name);
                DataDate dd = entry.getKey();

                for (String f : entry.getValue())
                {
                    if(state == TaskState.STOPPED || state == TaskState.STOPPING) {
                        System.out.println("[GDL " + ID + " on Thread " + Thread.currentThread().getId() + "] Breaking out of download loop.");
                        break downloadLoop;
                    } else if(Thread.currentThread().isInterrupted()) {
                        break downloadLoop;
                    }
                    if (f != null)
                    {
                        DownloaderFramework downloader = (DownloaderFramework) downloadCtr.newInstance(dd, outFolder, metaData, f);
                        try {
                            downloader.download();
                            AddDownloadFile(stmt, dd.getYear(), dd.getDayOfYear(), downloader.getOutputFilePath());
                        } catch (Exception e) {
                            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "ModisLocalStorageGlobalDownloader.run problem with running running DownloaderFramework or AddDownloadFile.", e);
                        }
                    }
                }
            }
            stmt.close();
            con.close();
        } catch (ParserConfigurationException | SAXException | IOException | InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException | ClassNotFoundException | SQLException e) {
            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "GenericLocalStorageGlobalDownloader.run problem with running GenericLocalStorageGlobalDownloader.", e);
        } catch (Exception e) {
            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "GenericLocalStorageGlobalDownloader.run problem with running GenericLocalStorageGlobalDownloader.", e);
        }
    }

}
