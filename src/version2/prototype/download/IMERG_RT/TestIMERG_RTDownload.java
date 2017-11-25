package version2.prototype.download.IMERG_RT;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;

import org.xml.sax.SAXException;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.PluginMetaData.FTP;
import version2.prototype.PluginMetaData.HTTP;
import version2.prototype.download.DownloadFailedException;

public class TestIMERG_RTDownload
{
    static public void main(String [ ] args)
    {
        String mode = "FTP";// the protocol type: ftp or http
        FTP myFtp = new FTP("jsimpson.pps.eosdis.nasa.gov", "/data/imerg/gis",
                "eastweb.system@gmail.com", "eastweb.system@gmail.com");
        HTTP myHttp = null;
        String className = null;
        String timeZone = null;
        int filesPerDay = 1;
        String datePatternStr = "\\d{4}";
        String fileNamePatternStr =
                "3B-HHR-L\\.MS\\.MRG\\.3IMERG\\.(\\d{8})-S233000-E235959\\.1410\\.V(\\d{2})[A-Z]\\.1day\\.tif((\\.gz)?)";

        //LocalDate ld = LocalDate.parse("Mon Dec 28 00:00:01 CDT 2015", DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz uuuu"));
        LocalDate ld = LocalDate.parse("Wed Sep 14 00:00:01 CDT 2016", DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz uuuu"));

        DownloadMetaData data = new DownloadMetaData(null, null, null, 10000, true, false, null,
                mode, myFtp, myHttp, className, timeZone, filesPerDay,
                datePatternStr, fileNamePatternStr, ld);

        /* String Title, ArrayList<String> QualityControlMetaData,
        Integer DaysPerInputData, Integer Resolution,
        Boolean CompositesContinueIntoNextYear,
        ArrayList<String> ExtraDownloadFiles,
        String mode, FTP myFtp, HTTP myHttp,
        String downloadFactoryClassName, String timeZone,
        int filesPerDay, String datePatternStr, String fileNamePatternStr,
        LocalDate originDate
         */

        IMERG_RTListDatesFiles iList;
        try {
            iList = new IMERG_RTListDatesFiles(new DataDate(data.originDate), new DataDate(LocalDate.now()), data, null);
            Map<DataDate, ArrayList<String>> tempDatesFiles = iList.CloneListDatesFiles();

            for (Map.Entry<DataDate, ArrayList<String>> entry : tempDatesFiles.entrySet())
            {
                System.out.println(entry.getKey() + ":/ " +  entry.getValue());

                // download the file

                IMERG_RTDownloader dImerg =
                        new IMERG_RTDownloader(entry.getKey(), "e:\\eastweb\\testing\\IMERG_RT_Downnload", data, entry.getValue().get(0));
                try {
                    dImerg.download();
                } catch (DownloadFailedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (SAXException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }
}
