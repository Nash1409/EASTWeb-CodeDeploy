package version2.prototype.download.TRMM3B42RT_New;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.PluginMetaData.HTTP;
import version2.prototype.download.DownloadFailedException;
import version2.prototype.download.DownloadUtils;
import version2.prototype.download.DownloaderFramework;

/*
 * @Author: Yi Liu
 */

public class TRMM3B42RT_NewDownloader extends DownloaderFramework
{
    protected DataDate mDate;
    protected String mOutFolder;
    protected String mHostURL;
    protected String mMode;
    protected DownloadMetaData mData;
    protected String mFileToD;
    protected String outFilePath;

    /*
     * @param date:  The date for the files to be downloaded
     * @param outFolder: the folder to hold the file to be downloaded
     * @param data: DownloadMetaData that holds the information of URLs, communication prototol and such
     * @param fileToDownload: the file name of the file to be downloaded
     */
    public TRMM3B42RT_NewDownloader(DataDate date, String outFolder, DownloadMetaData data, String fileToDownload)
    {
        mDate = date;
        mOutFolder = outFolder;
        mData = data;
        mFileToD = fileToDownload;
        outFilePath = null;
        setHttpValues();
    }

    //set the http values from DownloadMetaData
    private void setHttpValues()
    {
        mMode = mData.mode;
        HTTP h = mData.myHttp;
        mHostURL = h.url;
    }

    @Override
    public void download() throws IOException, DownloadFailedException, SAXException, Exception
    {
        /*set the directory to store the file to be downloaded
         *all the tiles on the same day will be placed in a folder
         *workingDir\download\ProductName\year\dayOfYear
         */
        String dir = String.format(
                "%s"+"%04d" + File.separator+"%03d" ,
                mOutFolder, mDate.getYear(), mDate.getDayOfYear());

        if(!(new File(dir).exists()))
        {
            FileUtils.forceMkdir(new File(dir));
        }

        outFilePath = String.format("%s"+File.separator+"%s",dir, mFileToD);

        File outputFile  = new File(outFilePath);

        if (mMode.equalsIgnoreCase("HTTP"))
        {
            try
            {
                // form the complete url for the file to be downloaded
                String fileURL = mHostURL +
                        String.format("%04d/%02d/%s",
                                mDate.getYear(), mDate.getMonth(), mFileToD);

                //DownloadUtils.downloadToFile(new URL(fileURL), outputFile);

                //FIXIT:  add uname and password in the project XML file and retrieve there from there
                DownloadUtils.downloadWithCred(new URL(fileURL), outputFile, "EASTWeb", "Framew0rk!", 5);
            }
            catch (IOException e)
            {
                ErrorLog.add(Config.getInstance(), "TRMM3B42RT_New", mData.name, "TRMM3B42RT_NewDownloader.download problem while attempting to download to file.", e);
                return;
            }
        }

    }

    @Override
    public String getOutputFilePath()
    {
        return outFilePath;
    }

}