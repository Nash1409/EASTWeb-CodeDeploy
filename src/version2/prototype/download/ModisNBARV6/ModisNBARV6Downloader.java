package version2.prototype.download.ModisNBARV6;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.download.ModisDownloadUtils.ModisDownloader;

public class ModisNBARV6Downloader extends ModisDownloader
{
    public ModisNBARV6Downloader(DataDate date, String outFolder, DownloadMetaData data, String fileToDownload)
    {
        super(date, outFolder, data, fileToDownload);
    }
}