/**
 *
 */
package version2.prototype.download;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.Process;
import version2.prototype.ProcessWorker;
import version2.prototype.ProcessWorkerReturn;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseCache;

/**
 * @author Michael DeVos
 *
 */
public class DownloadWorker extends ProcessWorker {
    protected final GlobalDownloader gdl;

    /**
     * @param gdl
     * @param configInstance
     * @param process
     * @param projectInfoFile
     * @param pluginInfo
     * @param pluginMetaData
     * @param cachedFiles
     * @param outputCache
     */
    public DownloadWorker(GlobalDownloader gdl, Config configInstance, Process process, ProjectInfoFile projectInfoFile, ProjectInfoPlugin pluginInfo,
            PluginMetaData pluginMetaData, ArrayList<DataFileMetaData> cachedFiles, DatabaseCache outputCache) {
        super(configInstance, "DownloadWorker", process, projectInfoFile, pluginInfo, pluginMetaData, cachedFiles, outputCache);
        this.gdl = gdl;
    }

    @Override
    public ProcessWorkerReturn process() {
        try {
            //javax.swing.JOptionPane.showMessageDialog(null,"Start date: "+gdl.GetStartDate().toString()+"\nEnd date: "+gdl.GetEndDate().toString());
            gdl.setStatetoActive();
            gdl.SetCompleted();
            outputCache.LoadUnprocessedGlobalDownloadsToLocalDownloader(configInstance.getGlobalSchema(), projectInfoFile.GetProjectName(), pluginInfo.GetName(),
                    gdl.metaData.name, projectInfoFile.GetStartDate(), pluginMetaData.ExtraDownloadFiles, pluginInfo.GetModisTiles(), gdl.listDatesFiles);
        } catch (ClassNotFoundException | SQLException | ParserConfigurationException | SAXException | IOException e) {
            ErrorLog.add(process, "LocalDownloader.AttemptUpdate error", e);
        } catch (Exception e) {
            ErrorLog.add(process, "Failed to connect. Check your network connection or please try again later.", e);
        }
        return null;
    }

    @Override
    public boolean verifyResults() {
        // TODO Auto-generated method stub
        return true;
    }

}
