
package version2.prototype.download.TRMM3B42RT_New;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.ConfigReadException;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.download.DownloadFailedException;
import version2.prototype.download.GlobalDownloader;
import version2.prototype.download.ListDatesFiles;
import version2.prototype.download.RegistrationException;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.DownloadFileMetaData;
import version2.prototype.util.FileSystem;

/**
 * @author Yi Liu
 *
 */
public class TRMM3B42RT_NewGlobalDownloader extends GlobalDownloader {

    public TRMM3B42RT_NewGlobalDownloader(int myID, Config configInstance, String pluginName, DownloadMetaData metaData, ListDatesFiles listDatesFiles, LocalDate startDate, LocalDate endDate) throws ClassNotFoundException,
    ParserConfigurationException, SAXException, IOException, SQLException, RegistrationException {
        super(myID, configInstance, pluginName, metaData, listDatesFiles, startDate, endDate);
    }

    @Override
    //get the files to in the TRMM3B42RTListDatesFiles, remove from the list the files downloaded already by checking the cached files in the database
    //Download the files in the list by calling the download() method in TRMM3B42RTDownloader
    public void run()
    {
        //DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        Connection con = RDS.getCentralDBconnection();
        if(con == null) {
            return;
        }
        Statement stmt;
        try {
            stmt = con.createStatement();
        } catch (SQLException e) {
            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "TRMM3B42RT_NewGlobalDownloader.run problem while attempting to download file.", e);
            return;
        }

        // Step 1: get all downloads from ListDatesFiles
        Map<DataDate, ArrayList<String>> datesFiles = listDatesFiles.CloneListDatesFiles();

        // Step 2: Pull all cached downloads
        ArrayList<DataFileMetaData> cachedD = new ArrayList<DataFileMetaData>();

        try {
            cachedD = GetAllDownloadedFiles();

            // Step 3: Remove already downloaded files from ListDatesFiles
            for (DataFileMetaData d: cachedD)
            {
                DownloadFileMetaData downloaded =  d.ReadMetaDataForProcessor();

                // get the year and dayOfyear from each downloaded file
                DataDate thisDate = new DataDate( downloaded.day, downloaded.year);

                // get the files associated with the date in the ListDatesFiles
                ArrayList <String> files = datesFiles.get(thisDate);

                Iterator<String> fIter = files.iterator();

                while (fIter.hasNext())
                {
                    String strPath = downloaded.dataFilePath;
                    // System.out.println(strPath);
                    strPath = strPath.substring(strPath.lastIndexOf(File.separator)+1, strPath.lastIndexOf("."));
                    // remove the file if it is found in the downloaded list
                    if ((fIter.next().toLowerCase()).contains((strPath.toLowerCase())))
                    {
                        fIter.remove();
                    }
                }

                datesFiles.put(thisDate, files);
            }

        }catch (ClassNotFoundException | SQLException | ParserConfigurationException | SAXException | IOException | RegistrationException e) {
            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "TRMM3B42RT_NewGlobalDownloader.run problem while attempting to download file.", e);
        }

        // Step 4: Create downloader and run downloader for all that's left
        for(Map.Entry<DataDate, ArrayList<String>> entry : datesFiles.entrySet())
        {
            String outFolder;

            try {
                outFolder = FileSystem.GetGlobalDownloadDirectory(configInstance, pluginName, metaData.name);

                DataDate dd = entry.getKey();

                for (String f : entry.getValue())
                {

                    if (f != null)
                    {
                        TRMM3B42RT_NewDownloader downloader = new TRMM3B42RT_NewDownloader(dd, outFolder, metaData, f);

                        try{
                            downloader.download();
                        } catch (IOException | DownloadFailedException | SAXException e) {
                            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "TRMM3B42RT_NewGlobalDownloader.run problem while attempting to download file.", e);
                        } catch (Exception e) {
                            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "TRMM3B42RT_NewGlobalDownloader.run problem while attempting to download file.", e);
                        }


                        try {
                            AddDownloadFile(stmt, dd.getYear(), dd.getDayOfYear(), downloader.getOutputFilePath());
                        } catch (ClassNotFoundException | SQLException | ParserConfigurationException | SAXException | IOException | RegistrationException e) {
                            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "TRMM3B42RT_NewGlobalDownloader.run problem while attempting to add download file.", e);
                        }
                    }
                }
            } catch (ConfigReadException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
        }
        try {
            stmt.close();
            con.close();
        } catch (SQLException e) { /* do nothing */ }
    }

}
