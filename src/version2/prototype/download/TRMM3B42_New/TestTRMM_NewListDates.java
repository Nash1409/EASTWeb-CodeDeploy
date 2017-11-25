package version2.prototype.download.TRMM3B42_New;
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

public class TestTRMM_NewListDates
{
    static public void main(String [ ] args)
    {
        String mode = "http";// the protocol type: ftp or http
        FTP myFtp = null;
        HTTP myHttp = new HTTP("http://disc2.gesdisc.eosdis.nasa.gov/data/TRMM_L3/TRMM_3B42_Daily.7/");
        String className = null;
        String timeZone = null;
        int filesPerDay = 1;
        String datePatternStr = "\\d{4}";
        String fileNamePatternStr =
                "3B42_Daily\\.\\d{8}\\.7\\.nc4";

        //LocalDate ld = LocalDate.parse("Wed Sep 14 00:00:01 CDT 2016", DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz uuuu"));
        LocalDate ld = LocalDate.parse("Wed Jun 22 00:00:01 CDT 2016", DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz uuuu"));

        DownloadMetaData data = new DownloadMetaData(null, null, null, 25000, true, false, null,
                mode, myFtp, myHttp, className, timeZone, filesPerDay,
                datePatternStr, fileNamePatternStr, ld);

        TRMM3B42_NewListDatesFiles iList;
        try {
            iList = new TRMM3B42_NewListDatesFiles(new DataDate(data.originDate), new DataDate(LocalDate.now()), data, null);
            Map<DataDate, ArrayList<String>> tempDatesFiles = iList.CloneListDatesFiles();

            for (Map.Entry<DataDate, ArrayList<String>> entry : tempDatesFiles.entrySet())
            {
                System.out.println(entry.getKey() + ":/ " +  entry.getValue());

                // download the file

                TRMM3B42_NewDownloader dTRMM = new TRMM3B42_NewDownloader(entry.getKey(), "d:\\test\\newTRMM", data, entry.getValue().get(0));
                try {
                    dTRMM.download();
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