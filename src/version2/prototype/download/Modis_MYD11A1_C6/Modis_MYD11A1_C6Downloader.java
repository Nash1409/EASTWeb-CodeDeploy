package version2.prototype.download.Modis_MYD11A1_C6;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.download.ModisDownloadUtils.ModisDownloader;

public class Modis_MYD11A1_C6Downloader extends ModisDownloader
{
    public Modis_MYD11A1_C6Downloader(DataDate date, String outFolder, DownloadMetaData data, String fileToDownload)
    {
        super(date, outFolder, data, fileToDownload);
    }
}