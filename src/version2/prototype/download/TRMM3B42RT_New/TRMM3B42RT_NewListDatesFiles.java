package version2.prototype.download.TRMM3B42RT_New;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.download.DownloadUtils;
import version2.prototype.download.ListDatesFiles;

// @Author: Yi Liu

public class TRMM3B42RT_NewListDatesFiles extends ListDatesFiles
{
    public TRMM3B42RT_NewListDatesFiles(DataDate startDate, DataDate endDate,DownloadMetaData data, ProjectInfoFile project) throws IOException
    {
        super(startDate, endDate, data, project);
    }

    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesFTP() {
        return null;
    }

    @Override
    protected Map<DataDate, ArrayList<String>> ListDatesFilesHTTP()
    {
        HashMap<DataDate, ArrayList<String>> tempMapDatesFiles =  new HashMap<DataDate, ArrayList<String>>();

        final String mHostURL = mData.myHttp.url;

        // DataDate.toCompactString() returns a date in the format of yyyy-mm-dd-hh
        // get the substring of yyyy-mm-dd and remove the '-'
        String startDateStr = (sDate.toCompactString().substring(0, 10)).replaceAll("-", "");
        String endDateStr = (eDate.toCompactString().substring(0, 10)).replaceAll("-", "");

        String yearPattern = "(19|20)\\d\\d/";
        String monthPattern = "(0?[1-9]|1[012])/";

        try
        {
            ByteArrayOutputStream folderOutStream = new ByteArrayOutputStream();
            DownloadUtils.downloadToStream(new URL(mHostURL), folderOutStream);

            List<String> availableYears = Arrays.asList(folderOutStream.toString().split("[\\r\\n]+"));

            outerLoop: for(String paramY : availableYears)
            {
                if(Thread.currentThread().isInterrupted())
                {
                    break;
                }


                //String datePattern = "((19|20)\\d\\d)(0?[1-9]|1[012])(0?[1-9]|[12][0-9]|3[01])/";
                Pattern pattern = Pattern.compile(yearPattern);
                Matcher matcher = pattern.matcher(paramY);

                // URL structure:  host/yyyy/mm
                if(matcher.find())
                {
                    try
                    {
                        //get rid of the last character '/'
                        int year = Integer.parseInt(matcher.group().substring(0, 4));

                        if (year >= sDate.getYear() && year <= eDate.getYear())
                        {
                            String yearFolderURL = mHostURL + String.format("%04d", year);

                            //System.out.println(yearFolderURL);

                            ByteArrayOutputStream monthFolderOutstream = new ByteArrayOutputStream();
                            DownloadUtils.downloadToStream(new URL(yearFolderURL), monthFolderOutstream);

                            List<String> availableMonths = Arrays.asList(monthFolderOutstream.toString().split("[\\r\\n]+"));

                            for(String paramM : availableMonths)
                            {
                                Pattern patternM = Pattern.compile(monthPattern);
                                Matcher matcherM = patternM.matcher(paramM);

                                if (matcherM.find())
                                {
                                    try
                                    {
                                        // two digits of month and a '/'
                                        if (matcherM.group().length() == 3)
                                        {
                                            //get rid of the '/' at the end
                                            int month = Integer.parseInt(matcherM.group().substring(0, 2));

                                            // check if the month of the startDate is not starting at 1
                                            if (((year == sDate.getYear()) && (month >= sDate.getMonth()))
                                                    || ((year == eDate.getYear()) && (month <= eDate.getMonth())))
                                            {
                                                String monthFolderURL = mHostURL + String.format("%04d/%s", year, matcherM.group());
                                                // System.out.println("url : " + monthFolderURL);

                                                ByteArrayOutputStream fileFolderOutstream = new ByteArrayOutputStream();
                                                DownloadUtils.downloadToStream(new URL(monthFolderURL), fileFolderOutstream);

                                                List<String> availableFiles = Arrays.asList(fileFolderOutstream.toString().split("[\\r\\n]+"));

                                                for(String paramF : availableFiles)
                                                {
                                                    //FIXME: cannot match if mData.fileNamePattern is used.
                                                    // file pattern: 3B42RT_daily\.(\d{8})\.7\.nc4
                                                    Pattern patternF = Pattern.compile("3B42RT_Daily\\.(\\d{8})\\.7\\.nc4");
                                                    //Pattern patternF = mData.fileNamePattern;
                                                    Matcher matcherF = patternF.matcher(paramF);

                                                    if(paramF.contains(".nc4") && !paramF.contains(".xml") && (matcherF.find()))
                                                    {
                                                        /* check the date in each file to see if it is equal to or late
                                                         * than the start date
                                                         */
                                                        String fileDate = matcherF.group(1);
                                                        //System.out.println(fileDate);
                                                        if (fileDate.compareTo(startDateStr) >= 0 && fileDate.compareTo(endDateStr) <= 0)
                                                        {
                                                            // System.out.println(matcherF.group());
                                                            ArrayList<String> fileList = new ArrayList<String>();
                                                            // add file name to the list
                                                            fileList.add(matcherF.group());
                                                            //get the day
                                                            int day = Integer.parseInt(fileDate.substring(6));

                                                            // add file name and the associated date to the map
                                                            tempMapDatesFiles.put(new DataDate(day, month, year), fileList);
                                                        }
                                                    }

                                                }

                                            }

                                        }

                                    }
                                    catch(Exception e)
                                    {
                                        ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "TRMM3B42RT_NewListDatesFiles.ListDatesFiles: HTTP problem while getting file list.", e);
                                    }
                                }
                            }

                        }
                    }
                    catch(Exception e)
                    {
                        ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "TRMM3B42RT_NewListDatesFiles.ListDatesFiles: HTTP problem while getting file list.", e);
                    }
                }
            }

        }
        catch(Exception e)
        {
            ErrorLog.add(Config.getInstance(), mData.Title, mData.name, "TRMM3B42RT_NewListDatesFiles.ListDatesFiles: HTTP problem while getting file list.", e);
        }

        return tempMapDatesFiles;
    }
}