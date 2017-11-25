package version2.prototype.download;

import java.io.IOException;
import org.xml.sax.SAXException;

public abstract class DownloaderFramework {

    //process of downloading one file
    /**
     * Downloads a single file determined by the DataDate and DownloadMetaData the implementing class is created with.
     *
     * @throws IOException
     * @throws DownloadFailedException
     * @throws SAXException
     * @throws Exception
     */
    public abstract void download() throws IOException, DownloadFailedException, SAXException, Exception;

    /**
     * Returns the path of the downloaded file.
     *
     * @return downloaded file path
     */
    public abstract String getOutputFilePath();

}
