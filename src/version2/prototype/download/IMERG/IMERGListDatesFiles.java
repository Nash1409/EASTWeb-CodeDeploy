package version2.prototype.download.IMERG;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.joda.time.LocalDate;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.download.ConnectionContext;
import version2.prototype.download.ListDatesFiles;

public class IMERGListDatesFiles extends ListDatesFiles{

    public IMERGListDatesFiles(DataDate startDate, DataDate endDate, DownloadMetaData data, ProjectInfoFile project) throws IOException
    {
        super(startDate, endDate, data, project);
    }


    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesHTTP()
    {
        return null;
    }

    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesFTP()
    {
        Map<DataDate, ArrayList<String>>  tempMapDatesToFiles = new HashMap<DataDate, ArrayList<String>>();

        FTPClient ftpC = null;

        try
        {
            ftpC = (FTPClient) ConnectionContext.getConnection(mData);
        }
        catch (ConnectException e)
        {
            ErrorLog.add(Config.getInstance(), "IMERG", mData.name, "IMERG_RTListDatesFiles.ListDatesFiles: "
                    + "Can't connect to download website, please check your URL.", e);
            return null;
        }

        try
        {
            LocalDate startDate = new LocalDate(sDate.getYear(), sDate.getMonth(), sDate.getDay());
            LocalDate endDate = new LocalDate(eDate.getYear(), eDate.getMonth(), eDate.getDay());

            for (LocalDate d = startDate; d.isBefore(endDate)|| d.isEqual(endDate);
                    d = d.plusDays(1))
            {
                // format the file directory
                // ftp://arthurhou.pps.eosdis.nasa.gov/gpmdata/yyyy/mm/dd

                String fileDir = String.format("%s/%04d/%02d/%02d/gis", mData.myFtp.rootDir,
                        d.getYear(), d.getMonthOfYear(), d.getDayOfMonth());

                if (!ftpC.changeWorkingDirectory(fileDir))
                {
                    continue;
                }

                for (FTPFile file : ftpC.listFiles())
                {
                    if(Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    //System.out.println(mData.fileNamePattern.matcher(file.getName()).matches());
                    ArrayList<String> fileNames = new ArrayList<String>();

                    //filename pattern:
                    //3B-DAY-GIS\.MS\.MRG\.3IMERG\.(\d{8})-S000000-E235959\.(\d{4}|\d{5})\.V(\d{2})[A-Z]\.tif
                    //if (file.isFile() &&

                    if (mData.fileNamePattern.matcher(file.getName()).matches())
                    {
                        // System.out.println(file.getName());
                        fileNames.add(file.getName());

                        // always get the last hour of the day -  23
                        DataDate dataDate = new DataDate(23, d.getDayOfMonth(), d.getMonthOfYear(), d.getYear());

                        tempMapDatesToFiles.put(dataDate, fileNames);
                    } else {
                        continue;
                    }
                }
            }

            ftpC.disconnect();
            ftpC = null;

            /* for (Map.Entry<DataDate, ArrayList<String>> entry : tempMapDatesToFiles.entrySet())
            {
                System.out.println(entry.getKey() + "/" + entry.getValue());
            }
             */

            return tempMapDatesToFiles;
        }
        catch (Exception e)
        {
            ErrorLog.add(Config.getInstance(), "IMERG", mData.name, "IMERGListDatesFiles.ListDatesFilesFTP problem while creating list using FTP.", e);
            return null;
        }
    }
}
