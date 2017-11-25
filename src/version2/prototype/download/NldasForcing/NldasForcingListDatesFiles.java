package version2.prototype.download.NldasForcing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.download.ConnectionContext;
import version2.prototype.download.DownloadUtils;
import version2.prototype.download.ListDatesFiles;

public class NldasForcingListDatesFiles extends ListDatesFiles {

    public NldasForcingListDatesFiles(DataDate startDate, DataDate endDate, DownloadMetaData data, ProjectInfoFile project) throws IOException {
        super(startDate, endDate, data, project);
    }

    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesFTP()
    {
        Map<DataDate, ArrayList<String>>  tempMapDatesToFiles = new HashMap<DataDate, ArrayList<String>>();
        FTPClient ftpClient = null;
        String mRootDir = mData.myFtp.rootDir;

        try
        {
            ftpClient = (FTPClient) ConnectionContext.getConnection(mData);

            ftpClient.enterLocalPassiveMode();

            if(!ftpClient.changeWorkingDirectory(mRootDir)) {
                throw new IOException("Couldn't navigate to " + mRootDir);
            }

            // List out all of the year directories.
            FTPFile[] yearDirs = ftpClient.listDirectories();

            outerLoop: for(FTPFile yearDir : yearDirs) {
                if(Thread.currentThread().isInterrupted()) {
                    break;
                }

                // There is another file named "doc" in the host's file system
                if(yearDir.getName().equalsIgnoreCase("doc") || !yearDir.isDirectory()) {
                    continue;
                }

                if(Integer.parseInt(yearDir.getName()) >= sDate.getYear() && Integer.parseInt(yearDir.getName()) <= eDate.getYear())
                {
                    for(FTPFile dayOfYearDir : ftpClient.listDirectories(mRootDir + yearDir.getName() + "/"))
                    {
                        // Continue if the day of year is less than the start date or greater than the end date
                        if((Integer.parseInt(yearDir.getName()) ==  sDate.getYear() && Integer.parseInt(dayOfYearDir.getName()) < sDate.getDayOfYear())
                                ||(Integer.parseInt(yearDir.getName()) == eDate.getYear() && Integer.parseInt(dayOfYearDir.getName()) > eDate.getDayOfYear())){
                            continue;
                        }

                        ArrayList<String> files = new ArrayList<String>();
                        for(FTPFile hourlyFile : ftpClient.listFiles(mRootDir + yearDir.getName() + "/" + dayOfYearDir.getName() + "/"))
                        {
                            if(Thread.currentThread().isInterrupted()) {
                                break outerLoop;
                            }

                            // Continue if the file is the .xml companion file
                            if(!hourlyFile.getName().endsWith(".grb")) {
                                continue;
                            }

                            if(Integer.parseInt(yearDir.getName()) == sDate.getYear() && Integer.parseInt(dayOfYearDir.getName()) == sDate.getDayOfYear())
                            {
                                int startIndex = hourlyFile.getName().indexOf(".002.grb") - 4;
                                if(Integer.parseInt(hourlyFile.getName().substring(startIndex, (startIndex+2))) >= sDate.getHour()) {
                                    files.add(hourlyFile.getName());
                                }
                            }
                            else if(Integer.parseInt(yearDir.getName()) == eDate.getYear() && Integer.parseInt(dayOfYearDir.getName()) == eDate.getDayOfYear())
                            {
                                int startIndex = hourlyFile.getName().indexOf(".002.grb") - 4;
                                if(Integer.parseInt(hourlyFile.getName().substring(startIndex, (startIndex+2))) <= eDate.getHour()) {
                                    files.add(hourlyFile.getName());
                                }
                            }
                            else {
                                files.add(hourlyFile.getName());
                            }
                        }
                        if(files.size() >= mData.filesPerDay) {
                            tempMapDatesToFiles.put(new DataDate(Integer.parseInt(dayOfYearDir.getName()), Integer.parseInt(yearDir.getName())), files);
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            ErrorLog.add(Config.getInstance(), "NldasForcing", mData.name, "NldasForcingListDatesFiles.Actual_ListDatesFilesFTP problem while creating list using FTP.", e);
        }
        finally
        {
            try {
                if(ftpClient != null && ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            }
            catch (IOException e) {
                ErrorLog.add(Config.getInstance(), "NldasForcing", mData.name, "NldasForcingListDatesFiles.Actual_ListDatesFilesFTP problem while logging out and disconnecting FTP connection.", e);
            }
        }

        return tempMapDatesToFiles;
    }

    /*
     * This is the theoretical method to find the files available from the ftp server.
     * This method builds the list without actually traversing the ftp server.
     * (this method takes 250 milliseconds compared to approximately 3 minutes for actually traversing the ftp server)
     * This method can be either removed or left if there is a desire to have it regardless.
     * - Chris Plucker 7/26/2015
     */
    protected Map<DataDate, ArrayList<String>> Theoretical_ListDatesFilesFTP()
    {
        String fileFormat = "NLDAS_FORA0125_H.A%04d%02d%02d.%02d00.002.grb";
        Map<DataDate, ArrayList<String>>  mapDatesToFiles = new HashMap<DataDate, ArrayList<String>>();
        //DataDate today = new DataDate(LocalDate.now());

        // Get the files from the start year
        for (int month = sDate.getMonth(); month <= 12; month++)
        {
            if(month != sDate.getMonth())
            {
                for(int day = 1; day <= 31; day++)
                {
                    ArrayList<String> files = new ArrayList<String>();
                    for (int hour = 0; hour <= 23; hour++)
                    {
                        files.add(String.format(fileFormat, sDate.getYear(), month, day, hour));
                    }

                    try { mapDatesToFiles.put(new DataDate(day, month, sDate.getYear()), files); }
                    catch(DateTimeException e) { }
                }
            }
            else
            {
                for(int day = sDate.getDay(); day <= 31; day++)
                {
                    if(day != sDate.getDay())
                    {
                        ArrayList<String> files = new ArrayList<String>();
                        for (int hour = 0; hour <= 23; hour++)
                        {
                            files.add(String.format(fileFormat, sDate.getYear(), month, day, hour));
                        }
                        try { mapDatesToFiles.put(new DataDate(day, month, sDate.getYear()), files); }
                        catch(DateTimeException e) { }
                    }
                    else
                    {
                        ArrayList<String> files = new ArrayList<String>();
                        for (int hour = sDate.getHour(); hour <= 23; hour++)
                        {
                            files.add(String.format(fileFormat, sDate.getYear(), month, day, hour));
                        }
                        try { mapDatesToFiles.put(new DataDate(day, month, sDate.getYear()), files); }
                        catch(DateTimeException e) { }
                    }
                }
            }
        }

        // Get the files between the start year and the current year
        for(int year = sDate.getYear()+1; year < eDate.getYear(); year++)
        {
            for(int month = 1; month <= 12; month++)
            {
                for(int day = 1; day <= 31; day++)
                {
                    ArrayList<String> files = new ArrayList<String>();
                    for (int hour = 0; hour <= 23; hour++)
                    {
                        files.add(String.format(fileFormat, year, month, day, hour));
                    }
                    try { mapDatesToFiles.put(new DataDate(day, month, year), files); }
                    catch(DateTimeException e) { }
                }
            }
        }

        // Get the files up up to the current date (approximately, as they take about 3 days to process, so up to today's DayOfMonth-3);
        for (int month = 1; month <= eDate.getMonth(); month++)
        {
            if(month != eDate.getMonth())
            {
                for(int day = 1; day <= 31; day++)
                {
                    ArrayList<String> files = new ArrayList<String>();
                    for (int hour = 0; hour <= 23; hour++)
                    {
                        files.add(String.format(fileFormat, eDate.getYear(), month, day, hour));
                    }
                    try { mapDatesToFiles.put(new DataDate(day, month, eDate.getYear()), files); }
                    catch(DateTimeException e) { }
                }
            }
            else
            {
                for (int day = 1; day <= (eDate.getDay()-3); day++)
                {
                    ArrayList<String> files = new ArrayList<String>();
                    for (int hour = 0; hour <= 23; hour++)
                    {
                        files.add(String.format(fileFormat, eDate.getYear(), month, day, hour));
                    }
                    try { mapDatesToFiles.put(new DataDate(day, month, eDate.getYear()), files); }
                    catch(DateTimeException e) { }
                }
            }
        }

        return mapDatesToFiles;
    }

    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesHTTP() {

        Map<DataDate, ArrayList<String>>  tempMapDatesToFiles = new HashMap<DataDate, ArrayList<String>>();
        String startDateStr = (sDate.toCompactString().substring(0, 10).replaceAll("-", ""));
        final Pattern yearDirPattern = Pattern.compile("(19|20)\\d\\d/");
        final Pattern dayDirPattern = Pattern.compile("(0|1|2|3)\\d\\d/");

        final String mHostURL = mData.myHttp.url;

        try
        {

            tempMapDatesToFiles =  new HashMap<DataDate, ArrayList<String>>();
            ByteArrayOutputStream folderOutStream = new ByteArrayOutputStream();
            DownloadUtils.downloadToStream(new URL(mHostURL), folderOutStream);

            List<String> availableYears = Arrays.asList(folderOutStream.toString().split("[\\r\\n]+"));

            for (String paramY : availableYears)
            {
                if(Thread.currentThread().isInterrupted()) {
                    break;
                }

                Matcher matcher = yearDirPattern.matcher(paramY);

                // URL structure: host/yyyy/ddd

                if(matcher.find())
                {
                    try
                    {
                        int year = Integer.parseInt(matcher.group().substring(0, 4));

                        if(year >= sDate.getYear() && year <= eDate.getYear())
                        {
                            String yearDir = mHostURL + String.format("%04d", year);

                            ByteArrayOutputStream dayFolderOutStream = new ByteArrayOutputStream();
                            DownloadUtils.downloadToStream(new URL(yearDir), dayFolderOutStream);

                            List<String> availableDays = Arrays.asList(dayFolderOutStream.toString().split("[\\r\\n]+"));

                            for(String paramD : availableDays)
                            {
                                Matcher matcherD = dayDirPattern.matcher(paramD);

                                if(matcherD.find())
                                {
                                    try
                                    {
                                        if(matcherD.group().length() == 4)
                                        {
                                            int day = Integer.parseInt(matcherD.group().substring(0, 3));

                                            if((year == sDate.getYear() && day >= sDate.getDayOfYear())
                                                    || year > sDate.getYear())
                                            {
                                                String dayDir = mHostURL + String.format("%04d/%s", year, matcherD.group());

                                                ByteArrayOutputStream fileFolderOutStream = new ByteArrayOutputStream();
                                                DownloadUtils.downloadToStream(new URL(dayDir), fileFolderOutStream);

                                                List<String> availableFiles = Arrays.asList(fileFolderOutStream.toString().split("[\\r\\n]+"));
                                                ArrayList<String> fileList = new ArrayList<String>();
                                                ArrayList<String> hourly = new ArrayList<String>();

                                                for (String paramF:availableFiles)
                                                {

                                                    if(paramF.contains(".grb") && !paramF.contains(".xml"))
                                                    {
                                                        hourly.add(paramF);
                                                    }
                                                }
                                                if(hourly.size() < 24)
                                                {
                                                    break;
                                                }

                                                for(String paramF : hourly)
                                                {

                                                    Pattern patternF = Pattern.compile("NLDAS_FORA0125_H\\.A(\\d{4})(\\d{2})(\\d{2}).(\\d{2})00\\.002\\.grb");
                                                    Matcher matcherF = patternF.matcher(paramF);

                                                    if(matcherF.find())
                                                    {
                                                        String fileDate = matcherF.group(1) + matcherF.group(2) + matcherF.group(3);

                                                        if(fileDate.compareTo(startDateStr) >= 0)
                                                        {


                                                            fileList.add(matcherF.group());

                                                            String[] strings = matcherF.group().split("[.]");

                                                            final int month = Integer.parseInt(strings[1].substring(5, 7));
                                                            final int d = Integer.parseInt(strings[1].substring(7, 9));
                                                            final int hour = Integer.parseInt(strings[2]);
                                                            DataDate dataDate = new DataDate(hour, d, month, year);

                                                            tempMapDatesToFiles.put(dataDate, fileList);


                                                        }
                                                    }

                                                    /* for(String paramF : availableFiles)
                                                {
                                                    if(availableFiles.size() < 94)
                                                    {
                                                        break;
                                                    }

                                                    Pattern patternF = Pattern.compile("NLDAS_FORA0125_H\\.A(\\d{4})(\\d{2})(\\d{2}).(\\d{2})00\\.002\\.grb");
                                                    Matcher matcherF = patternF.matcher(paramF);

                                                    if(paramF.contains(".grb") && !paramF.contains(".xml") && (matcherF.find()))
                                                    {
                                                        String fileDate = matcherF.group(1) + matcherF.group(2) + matcherF.group(3);

                                                        if(fileDate.compareTo(startDateStr) >= 0)
                                                        {


                                                            fileList.add(matcherF.group());

                                                            String[] strings = matcherF.group().split("[.]");

                                                            final int month = Integer.parseInt(strings[1].substring(5, 7));
                                                            final int d = Integer.parseInt(strings[1].substring(7, 9));
                                                            final int hour = Integer.parseInt(strings[2]);
                                                            DataDate dataDate = new DataDate(hour, d, month, year);

                                                            tempMapDatesToFiles.put(dataDate, fileList);


                                                        }
                                                    }*/
                                                }
                                            }
                                        }
                                    }
                                    catch(Exception e)
                                    {
                                        ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "NldasForcingListDatesFiles.ListDatesFilesHTTP problem while getting file list.", e);
                                    }
                                }
                            }

                        }
                    }
                    catch(Exception e)
                    {
                        ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "NldasForcingListDatesFiles.ListDatesFilesHTTP problem while getting file list.", e);
                    }
                }
            }

        }
        catch(Exception e)
        {
            ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "NldasForcingListDatesFiles.ListDatesFilesHTTP problem while getting file list.", e);
        }

        return tempMapDatesToFiles;
    }
}
