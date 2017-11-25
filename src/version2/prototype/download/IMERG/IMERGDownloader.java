package version2.prototype.download.IMERG;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.xml.sax.SAXException;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.PluginMetaData.FTP;
import version2.prototype.download.ConnectionContext;
import version2.prototype.download.DownloadFailedException;
import version2.prototype.download.DownloadUtils;
import version2.prototype.download.DownloaderFramework;
import version2.prototype.download.FTPClientPool;

/*
 * @Author: Yi Liu
 */

public class IMERGDownloader extends DownloaderFramework
{

    private DataDate mDate;
    private String mOutputFolder;
    private String mMode;
    private String mHost;
    private String mRoot;
    private String mFileToDownload;
    private DownloadMetaData mData;
    private String outFilePath;

    public IMERGDownloader(DataDate date, String outFolder, DownloadMetaData data, String fileToDownload)
    {
        mDate = date;
        mOutputFolder = outFolder;
        mData = data;
        mFileToDownload = fileToDownload;
        setFTPValues();
    }

    // get the values from DownloadMetaData
    private void setFTPValues()
    {
        mMode = mData.mode;
        FTP f = mData.myFtp;
        mHost = f.hostName;
        mRoot = f.rootDir;
    }
    @Override
    public void download() throws IOException, DownloadFailedException,
    Exception, SAXException {
        if (mMode.equalsIgnoreCase("FTP"))
        {
            FTPClient ftpC =
                    (FTPClient) ConnectionContext.getConnection(mData);

            try {

                final String dayDirectory =
                        String.format("%s/%04d/%02d/%02d/gis",
                                mRoot, mDate.getYear(), mDate.getMonth(), mDate.getDay());

                if (!ftpC.changeWorkingDirectory(dayDirectory))
                {
                    throw new IOException("Couldn't navigate to directory: "
                            + dayDirectory);
                }

                // set the directory to store the download file
                String dir = String.format("%s"+"%04d" + File.separator+"%03d",
                        mOutputFolder, mDate.getYear(), mDate.getDayOfYear());

                if(!(new File(dir).exists()))
                {
                    FileUtils.forceMkdir(new File(dir));
                }

                outFilePath = String.format("%s"+File.separator+"%s",dir, mFileToDownload);

                File outputFile  = new File(outFilePath);

                DownloadUtils.download(ftpC, mFileToDownload, outputFile);

            } catch (IOException e)
            {
                throw e;
            }
            finally
            {
                FTPClientPool.returnFtpClient(mHost, ftpC);
                //FIXME:  need to disconnect the connection !
                //ftpC.disconnect();
            }
        } ;

    }

    @Override
    public String getOutputFilePath() {
        return outFilePath;
    }

}
