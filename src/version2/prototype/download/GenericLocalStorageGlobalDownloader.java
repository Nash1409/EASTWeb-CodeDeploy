/**
 *
 */
package version2.prototype.download;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.amazonaws.samples.DeployCode.S3;
import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.TaskState;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.DownloadFileMetaData;
import version2.prototype.util.FileSystem;

/**
 * @author michael.devos
 *
 */
public class GenericLocalStorageGlobalDownloader extends GlobalDownloader {
    private Constructor<?> downloadCtr;
    /**
     * Creates a generic GlobalDownloader that expects to be writing downloaded files to a local storage and updating a local database.
     *
     * @param myID  - unique identifier ID of this GlobalDownloader
     * @param configInstance
     * @param pluginName
     * @param metaData
     * @param listDatesFiles
     * @param startDate
     * @param downloaderClassName
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws SQLException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws RegistrationException
     */
    public GenericLocalStorageGlobalDownloader(int myID, Config configInstance, String pluginName, DownloadMetaData metaData, ListDatesFiles listDatesFiles, LocalDate startDate, LocalDate endDate, String downloaderClassName)
            throws ClassNotFoundException, NoSuchMethodException, SecurityException, ParserConfigurationException, SAXException, IOException, SQLException, RegistrationException
    {
        super(myID, configInstance, pluginName, metaData, listDatesFiles, startDate, endDate);
        Class<?> downloaderClass = Class.forName("version2.prototype.download." + pluginName + "." + downloaderClassName);
        downloadCtr = downloaderClass.getConstructor(DataDate.class, String.class, DownloadMetaData.class, String.class);
    }

    @Override
    /*
     * Step 1: Get all downloads from ListDatesFiles
     * Step 2: Pull all cached downloads
     * Step 3: Remove already downloaded files from ListDatesFiles
     * Step 4: Create downloader and run downloader for all that's left
     */
    public void run() {
        //flag = false;
        try {
            if(state == TaskState.STOPPED || state == TaskState.STOPPING){
                return;
            }
            // DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
            Connection con = RDS.getCentralDBconnection();
            if(con == null) {
                return;
            }
            Statement stmt = con.createStatement();
            System.out.println("[GDL " + ID + " on Thread " + Thread.currentThread().getId() + "] GlobalDownloader of '" + metaData.name + "' files for plugin '"
                    + pluginName + "' starting from " + currentStartDate + ".");
            System.out.println("Running: " + downloadCtr.getName().substring((downloadCtr.getName().lastIndexOf(".") > -1 ? downloadCtr.getName().lastIndexOf(".") + 1 : 0)));

            // Step 1: Get all downloads from ListDatesFiles
            Map<DataDate, ArrayList<String>> filesTemp = listDatesFiles.CloneListDatesFiles();
            Map<DataDate, ArrayList<String>> datesFiles = new TreeMap<DataDate, ArrayList<String>>(filesTemp);

            // Remove from downloads list any which are before the current start date
            Set<DataDate> dateKeys = datesFiles.keySet();
            ArrayList <String> files;
            Iterator<String> fIter;
            ArrayList<DataDate> removeDates = new ArrayList<DataDate>();
            for(DataDate dd : dateKeys)
            {
                if(dd.getLocalDate().isBefore(currentStartDate) || dd.getLocalDate().isAfter(endDate))
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

                fIter = files.iterator();

                String fileTemp;
                while (fIter.hasNext())
                {
                    String strPath = downloaded.dataFilePath;
                    String path = strPath;
                    //                    System.out.println(strPath);
                    strPath = strPath.substring(strPath.lastIndexOf(File.separator)+1, strPath.lastIndexOf("."));
                    // remove the file if it is found in the downloaded list
                    fileTemp = fIter.next();
                    if ((fileTemp.toLowerCase()).contains((strPath.toLowerCase())))
                    {
                        S3.GetDownloadFile(path,"eastweb");
                        fIter.remove();
                    }
                }

                datesFiles.put(thisDate, files);
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
                            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "GenericLocalStorageGlobalDownloader.run problem with running running DownloaderFramework or AddDownloadFile.", e);
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
