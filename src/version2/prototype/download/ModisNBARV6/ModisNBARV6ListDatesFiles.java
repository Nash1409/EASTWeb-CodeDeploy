package version2.prototype.download.ModisNBARV6;

import java.io.IOException;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.download.ModisDownloadUtils.ModisListDatesFiles;

public class ModisNBARV6ListDatesFiles extends ModisListDatesFiles
{
    public ModisNBARV6ListDatesFiles(DataDate startDate, DataDate endDate, DownloadMetaData data, ProjectInfoFile project) throws IOException
    {
        super(startDate, endDate, data, project);
    }

}
