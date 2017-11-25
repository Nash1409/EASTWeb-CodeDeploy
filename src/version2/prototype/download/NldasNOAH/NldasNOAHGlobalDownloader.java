package version2.prototype.download.NldasNOAH;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.TaskState;
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

public class NldasNOAHGlobalDownloader extends GlobalDownloader{

    public NldasNOAHGlobalDownloader(int myID, Config configInstance, String pluginName, DownloadMetaData metaData, ListDatesFiles listDatesFiles, LocalDate startDate, LocalDate endDate) throws ClassNotFoundException, ParserConfigurationException, SAXException,
    IOException, SQLException, RegistrationException {
        super(myID, configInstance, pluginName, metaData, listDatesFiles, startDate, endDate);
    }

    @Override
    public void run()
    {
        DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        if(con == null) {
            return;
        }
        Statement stmt;
        try {
            stmt = con.createStatement();
        } catch (SQLException e) {
            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "NldasNOAHGlobalDownloader.run problem while running NldasNOAHDownloader.", e);
            return;
        }
        if(state == TaskState.STOPPED){
            return;
        }
        // Step 1: get all downloads from ListDatesFiles
        Map<DataDate, ArrayList<String>> datesFiles = listDatesFiles.CloneListDatesFiles();

        // Step 2: Pull all cached downloads
        //ArrayList<DataFileMetaData> cachedD = new ArrayList<DataFileMetaData>();
        ArrayList<DataFileMetaData> cachedD = null;

        try {
            cachedD = GetAllDownloadedFiles();

            // Step 3: Remove already downloaded files from ListDatesFiles
            for (DataFileMetaData d: cachedD)
            {
                DownloadFileMetaData downloaded =  d.ReadMetaDataForProcessor();
                // get the year and dayOfyear from each downloaded file
                DataDate thisDate = new DataDate(downloaded.day, downloaded.year);

                // get the files associated with the date in the ListDatesFiles
                ArrayList <String> files = datesFiles.get(thisDate);

                Iterator<String> fIter = files.iterator();

                while (fIter.hasNext())
                {
                    String strPath = downloaded.dataFilePath;
                    System.out.println(strPath);
                    strPath = strPath.substring(strPath.lastIndexOf(File.separator)+1, strPath.lastIndexOf("."));
                    // remove the file if it is found in the downloaded list
                    if ((fIter.next().toLowerCase()).contains((strPath.toLowerCase())))
                    {
                        fIter.remove();
                    }
                }

                datesFiles.put(thisDate, files);
            }

        } catch (ClassNotFoundException | SQLException | ParserConfigurationException | SAXException | IOException | RegistrationException e) {
            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "NldasNOAHGlobalDownloader.run problem while running NldasNOAHDownloader.", e);
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
                    if(f != null)
                    {
                        NldasNOAHDownloader downloader = new NldasNOAHDownloader(dd, outFolder, metaData, f);

                        try {
                            downloader.download();
                        } catch (DownloadFailedException e) {
                            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "NldasNOAHGlobalDownloader.run problem while running NldasNOAHDownloader.", e);
                        } catch (Exception e) {
                            ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "NldasNOAHGlobalDownloader.run problem while running NldasNOAHDownloader.", e);
                        }
                    }

                }
            } catch (IOException e) {
                ErrorLog.add(Config.getInstance(), pluginName, metaData.name, "NldasNOAHGlobalDownloader.run problem while attempting to handle downloading.", e);
            }
        }
        try {
            stmt.close();
        } catch (SQLException e) { /* do nothing */ }
        con.close();
    }

}
