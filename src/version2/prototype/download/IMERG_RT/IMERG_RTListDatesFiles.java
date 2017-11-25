package version2.prototype.download.IMERG_RT;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.download.ConnectionContext;
import version2.prototype.download.ListDatesFiles;

public class IMERG_RTListDatesFiles extends ListDatesFiles{

    private Map<DataDate, ArrayList<String>>  tempMapDatesToFiles = new HashMap<DataDate, ArrayList<String>>();

    // although exactly the same, the pattern fetched from the plugin file does not work
    private String fileNamePatternStr =
            "3B-HHR-L\\.MS\\.MRG\\.3IMERG\\.(\\d{8})-S233000-E235959\\.1410\\.V(\\d{2})[A-Z]\\.1day\\.tif((\\.gz)?)";

    public IMERG_RTListDatesFiles(DataDate startDate, DataDate endDate, DownloadMetaData data, ProjectInfoFile project) throws IOException
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
        FTPClient ftpC = null;

        try
        {
            ftpC = (FTPClient) ConnectionContext.getConnection(mData);
        }
        catch (ConnectException e)
        {
            ErrorLog.add(Config.getInstance(), "IMERG_RT", mData.name, "IMERG_RTListDatesFiles.ListDatesFiles: "
                    + "Can't connect to download website, please check your URL.", e);
            return null;
        }

        try
        {
            /* the location of the file for downloading follows the following rules
             * 1. if it is for the current year, search the files in the month directory under
             *            jsimpson.pps.eosdis.nasa.gov/data/imerg/gis
             * 2. if it is for the previous years, search the files in the yyyy/mm directory
             */
            //int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            //int currentMonth = Calendar.getInstance().get(Calendar.MONTH)+ 1;

            //int currentYear = 2016;
            //int currentMonth = 1;
            //javax.swing.JOptionPane.showMessageDialog(null,"Start date: "+sDate.toString()+"\nEnd date: "+eDate.toString());
            int startYear = sDate.getYear();
            int startMonth = sDate.getMonth();

            //int monthInPath = startMonth;

            String rootDir = mData.myFtp.rootDir;

            for (int year = startYear; year <= eDate.getYear(); year ++)
            {
                int months;
                if (year == startYear) {
                    months = startMonth;
                } else {
                    months = 1;
                }

                // access month folder
                if (year == eDate.getYear())
                {
                    for (int month = months; month <= eDate.getMonth(); month++)
                    {
                        String monthDir = String.format("%s/%04d/%02d", rootDir, eDate.getYear(),month);
                        AddFiles(ftpC, monthDir, month, year);
                    }
                }
                else   // not the current year. need to access folder yyyy/mm
                {
                    for (int month = months; month <= 12; month++)
                    {
                        String monthDir = String.format("%s/%04d/%02d", rootDir, year, month);
                        AddFiles(ftpC, monthDir, month, year);
                    }
                }
            }

            ftpC.disconnect();
            ftpC = null;
            return tempMapDatesToFiles;

        }
        catch (Exception e)
        {
            ErrorLog.add(Config.getInstance(), "IMERG_RT", mData.name, "IMERG_RTListDatesFiles.ListDatesFiles: FTP problem while creating list using FTP.", e);
            return null;
        }
    }

    private void AddFiles(FTPClient ftpC, String monthDir, int month, int year) throws IOException
    {
        if (!ftpC.changeWorkingDirectory(monthDir))
        {
            throw new IOException("Couldn't navigate to directory: " + monthDir);
        }

        for (FTPFile file : ftpC.listFiles())
        {
            if(Thread.currentThread().isInterrupted()) {
                break;
            }

            ArrayList<String> fileNames = new ArrayList<String>();

            //filename pattern:
            //3B-HHR-L\.MS\.MRG\.3IMERG\.(\d{8})-S233000-E235959\.1410\.V(\d{2})[A-Z]\.1day\.tif((\.gz)?)
            /*  if (file.isFile() &&
                    mData.fileNamePattern.matcher(file.getName()).matches())
             */
            //System.out.println(Pattern.compile(fileNamePatternStr).matcher(file.getName()).matches());
            if (file.isFile() &&
                    Pattern.compile(fileNamePatternStr).matcher(file.getName()).matches())
            {
                System.out.println(file.getName());
                //fileNames.add(file.getName());

                String[] str = file.getName().split("[.]");

                //year = Integer.parseInt(str[4].substring(0, 4));
                //month = Integer.parseInt(str[4].substring(4, 6));
                final int day = Integer.parseInt(str[4].substring(6, 8));
                // always get the last hour of the day -  23
                DataDate dataDate = new DataDate(23, day, month, year);
                //if (dataDate.compareTo(sDate) >= 0)
                if(dataDate.compareTo(sDate) >= 0 && dataDate.compareTo(eDate) <= 0)
                {
                    //System.out.println(file.getName());
                    fileNames.add(file.getName());
                    tempMapDatesToFiles.put(dataDate, fileNames);
                }
            } else {
                continue;
            }
        }
    }
}
