/**
 *
 */
package version2.prototype.download;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.Scheduler;
import version2.prototype.util.DatabaseCache;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;

/**
 * @author michael.devos
 *
 */
public final class LocalStorageDownloadFactory implements DownloaderFactory {
    private final Config configInstance;
    private final String downloaderClassName;
    private final ProjectInfoFile projectInfoFile;
    private final ProjectInfoPlugin pluginInfo;
    private final DownloadMetaData downloadMetaData;
    private final PluginMetaData pluginMetaData;
    private final Scheduler scheduler;
    private final DatabaseCache outputCache;
    private final ListDatesFiles listDatesFiles;
    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * @param manager
     * @param configInstance
     * @param downloaderClassName
     * @param projectInfoFile
     * @param pluginInfo
     * @param downloadMetaData
     * @param pluginMetaData
     * @param scheduler
     * @param outputCache
     * @param listDatesFiles
     * @param startDate
     * @param endDate
     */
    public LocalStorageDownloadFactory(Config configInstance, String downloaderClassName, ProjectInfoFile projectInfoFile, ProjectInfoPlugin pluginInfo, DownloadMetaData downloadMetaData,
            PluginMetaData pluginMetaData, Scheduler scheduler, DatabaseCache outputCache, ListDatesFiles listDatesFiles, LocalDate startDate, LocalDate endDate)
    {
        this.configInstance = configInstance;
        this.downloaderClassName = downloaderClassName;
        this.projectInfoFile = projectInfoFile;
        this.pluginInfo = pluginInfo;
        this.downloadMetaData = downloadMetaData;
        this.pluginMetaData = pluginMetaData;
        this.scheduler = scheduler;
        this.outputCache = outputCache;
        this.listDatesFiles = listDatesFiles;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /* (non-Javadoc)
     * @see version2.prototype.download.DownloaderFactory#CreateLocalDownloader(int, version2.prototype.ProjectInfoMetaData.ProjectInfoFile, version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin,
     * version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData, version2.prototype.Scheduler.Scheduler, version2.prototype.util.DatabaseCache)
     */
    @Override
    public LocalDownloader CreateLocalDownloader(GlobalDownloader gdl) {
        return new GenericLocalRetrievalLocalDownloader(configInstance, gdl, projectInfoFile, pluginInfo, pluginMetaData, scheduler, outputCache, listDatesFiles);
    }

    /* (non-Javadoc)
     * @see version2.prototype.download.DownloaderFactory#CreateGlobalDownloader(int, java.lang.String, version2.prototype.PluginMetaData.PluginMetaDataCollection.DownloadMetaData,
     * version2.prototype.download.ListDatesFiles)
     */
    @Override
    public GlobalDownloader CreateGlobalDownloader(int myID) {
        GlobalDownloader gdl = null;
        DatabaseConnection con = null;
        Statement stmt = null;
        try {
            gdl = new GenericLocalStorageGlobalDownloader(myID, configInstance, pluginInfo.GetName(), downloadMetaData, listDatesFiles, startDate, endDate, downloaderClassName);
            con = DatabaseConnector.getConnection(configInstance);
            if(con == null) {
                return null;
            }
            stmt = con.createStatement();
            gdl.RegisterGlobalDownloader();
        } catch (RegistrationException e) {
            return null;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | ParserConfigurationException | SAXException | IOException | SQLException e) {
            ErrorLog.add(Config.getInstance(), pluginInfo.GetName(), downloadMetaData.name, "LocalStorageDownloadFactory.CreateGlobalDownloader problem with creating new GenericLocalStorageGlobalDownloader.", e);
        } catch (Exception e) {
            ErrorLog.add(Config.getInstance(), pluginInfo.GetName(), downloadMetaData.name, "LocalStorageDownloadFactory.CreateGlobalDownloader problem with creating new GenericLocalStorageGlobalDownloader.", e);
        } finally {
            if(stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) { /* do nothing */}
            }
            con.close();
        }
        return gdl;
    }
}