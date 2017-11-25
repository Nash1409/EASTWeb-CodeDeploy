/**
 *
 */
package version2.prototype.download.ModisLSTV6;

import java.io.IOException;
import java.time.LocalDate;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.Scheduler;
import version2.prototype.download.DownloadFactory;
import version2.prototype.download.DownloaderFactory;
import version2.prototype.download.ListDatesFiles;
import version2.prototype.download.ModisDownloadUtils.ModisLocalStorageDownloadFactory;
import version2.prototype.util.DatabaseCache;

/**
 * @author Yi Liu
 *
 */
public class ModisLSTV6Factory extends DownloadFactory {

    public ModisLSTV6Factory(Config configInstance, ProjectInfoFile projectInfoFile, ProjectInfoPlugin pluginInfo, DownloadMetaData downloadMetaData, PluginMetaData pluginMetaData,
            Scheduler scheduler, DatabaseCache outputCache, LocalDate startDate, LocalDate endDate) {
        super(configInstance, projectInfoFile, pluginInfo, downloadMetaData, pluginMetaData, scheduler, outputCache, startDate, endDate);
    }

    /* (non-Javadoc)
     * @see version2.prototype.download.DownloadFactory#CreateDownloadFactory()
     */
    @Override
    public DownloaderFactory CreateDownloaderFactory(ListDatesFiles listDatesFiles) {
        return new ModisLocalStorageDownloadFactory(configInstance, "ModisLSTV6Downloader", projectInfoFile, pluginInfo, downloadMetaData, pluginMetaData, scheduler, outputCache, listDatesFiles, startDate, endDate);
    }

    /* (non-Javadoc)
     * @see version2.prototype.download.DownloadFactory#CreateListDatesFiles(version2.prototype.DataDate, version2.prototype.PluginMetaData.PluginMetaDataCollection.DownloadMetaData)
     */
    @Override
    public ListDatesFiles CreateListDatesFiles() throws IOException {
        return new ModisLSTV6ListDatesFiles(new DataDate(startDate), new DataDate(endDate), downloadMetaData, projectInfoFile);
    }

}
