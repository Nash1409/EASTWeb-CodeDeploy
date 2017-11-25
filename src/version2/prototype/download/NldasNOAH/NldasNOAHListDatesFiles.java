package version2.prototype.download.NldasNOAH;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
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

public class NldasNOAHListDatesFiles extends ListDatesFiles{

    public NldasNOAHListDatesFiles(DataDate startDate, DataDate endDate, DownloadMetaData data, ProjectInfoFile project) throws IOException
    {
        super(startDate, endDate, data, project);
    }


    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesHTTP()
    {
        Map<DataDate, ArrayList<String>>  tempMapDatesToFiles = new HashMap<DataDate, ArrayList<String>>();
        String startDateStr = (sDate.toCompactString().substring(0, 10).replaceAll("-", ""));
        final Pattern yearDirPattern = Pattern.compile("(19|20)\\d\\d/");
        final Pattern dayDirPattern = Pattern.compile("(0|1|2|3)\\d\\d/");

        final String mHostURL = mData.myHttp.url;
        //javax.swing.JOptionPane.showMessageDialog(null,"Start date: "+sDate.toString()+"\nEnd date: "+eDate.toString());
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

                                            //                                            if((year == sDate.getYear() && day >= sDate.getDayOfYear())
                                            //                                                    || (year > sDate.getYear() && year < eDate.getYear())
                                            //                                                    || (year == eDate.getYear() && day <= eDate.getDayOfYear()))
                                            if((year >= sDate.getYear() && year <= eDate.getYear() && day >= sDate.getDayOfYear() && day <= eDate.getDayOfYear()))
                                            {
                                                String dayDir = mHostURL + String.format("%04d/%s", year, matcherD.group());

                                                ByteArrayOutputStream fileFolderOutStream = new ByteArrayOutputStream();
                                                DownloadUtils.downloadToStream(new URL(dayDir), fileFolderOutStream);

                                                List<String> availableFiles = Arrays.asList(fileFolderOutStream.toString().split("[\\r\\n]+"));
                                                ArrayList<String> fileList = new ArrayList<String>();

                                                for(String paramF : availableFiles)
                                                {
                                                    if(availableFiles.size() < 86)//94)
                                                    {
                                                        break;
                                                    }

                                                    Pattern patternF = Pattern.compile("NLDAS_NOAH0125_H\\.A(\\d{4})(\\d{2})(\\d{2})\\.(\\d{4})\\.002\\.grb");
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
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    catch(Exception e)
                                    {
                                        ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "NldasNOAHListDatesFiles.ListDatesFilesHTTP problem while getting file list.", e);
                                    }
                                }
                            }

                        }
                    }
                    catch(Exception e)
                    {
                        ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "NldasNOAHListDatesFiles.ListDatesFilesHTTP problem while getting file list.", e);
                    }
                }
            }

        }
        catch(Exception e)
        {
            ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "NldasNOAHListDatesFiles.ListDatesFilesHTTP problem while getting file list.", e);
        }

        return tempMapDatesToFiles;

    }

    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesFTP()
    {
        Map<DataDate, ArrayList<String>>  tempMapDatesToFiles = new HashMap<DataDate, ArrayList<String>>();
        //System.out.println("START: "+sDate+" END: "+eDate);
        final Pattern yearDirPattern = Pattern.compile("\\d{4}");
        final Pattern dayDirPattern = Pattern.compile("\\d{3}");

        FTPClient ftpC = null;

        try
        {
            ftpC = (FTPClient) ConnectionContext.getConnection(mData);
        }
        catch (ConnectException e)
        {
            System.out.println("Can't connect to download website, please check your URL.");
            return null;
        }

        String mRoot = mData.myFtp.rootDir;
        try
        {
            if (!ftpC.changeWorkingDirectory(mRoot))
            {
                throw new IOException("Couldn't navigate to directory: " + mRoot);
            }

            tempMapDatesToFiles =  new HashMap<DataDate, ArrayList<String>>();

            outerLoop: for (FTPFile yearFile : ftpC.listFiles())
            {
                if(Thread.currentThread().isInterrupted()) {
                    break;
                }

                if (!yearFile.isDirectory()
                        || !yearDirPattern.matcher(yearFile.getName()).matches()) {
                    continue;
                }

                int year = Integer.parseInt(yearFile.getName());
                if (year < sDate.getYear() || year > eDate.getYear()) {
                    continue;
                }

                // List days in this year
                String yearDirectory =
                        String.format("%s/%s", mRoot, yearFile.getName());

                if (!ftpC.changeWorkingDirectory(yearDirectory)) {
                    throw new IOException(
                            "Couldn't navigate to directory: " + yearDirectory);
                }

                for(FTPFile dayFile : ftpC.listFiles())
                {
                    if (!dayFile.isDirectory()
                            || !dayDirPattern.matcher(dayFile.getName()).matches()) {
                        continue;
                    }

                    // List 24 hours in this day
                    String dayDirectory =
                            String.format("%s/%s", yearDirectory, dayFile.getName());

                    if (!ftpC.changeWorkingDirectory(dayDirectory)) {
                        throw new IOException(
                                "Couldn't navigate to directory: " + dayDirectory);
                    }

                    ArrayList<String> fileNames = new ArrayList<String>();

                    for (FTPFile file : ftpC.listFiles())
                    {
                        if(Thread.currentThread().isInterrupted()) {
                            break outerLoop;
                        }

                        if (file.isFile() &&
                                mData.fileNamePattern.matcher(file.getName()).matches())
                        {
                            /* pattern of NLDASNOAH
                             * {productname}.A%y4%m2%d2.%h4.002.grb
                             */

                            fileNames.add(file.getName());

                            String[] strings = file.getName().split("[.]");
                            final int month = Integer.parseInt(strings[1].substring(5, 7));
                            final int day = Integer.parseInt(strings[1].substring(7, 9));
                            final int hour = Integer.parseInt(strings[2]);
                            DataDate dataDate = new DataDate(hour, day, month, year);
                            if (dataDate.compareTo(sDate) >= 0 && dataDate.compareTo(eDate) <= 0)
                            {
                                tempMapDatesToFiles.put(dataDate, fileNames);
                            }
                        }
                    }
                }

            }

            ftpC.disconnect();
            ftpC = null;
            return tempMapDatesToFiles;
        }
        catch (Exception e)
        {
            ErrorLog.add(Config.getInstance(), "NldasNOAH", mData.name, "NldasNOAHListDatesFiles.ListDatesFilesFTP problem while creating list using FTP.", e);
            return null;
        }

    }

}
