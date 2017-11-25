package version2.prototype.processor.ModisNBARV6;

import java.io.File;
import java.util.ArrayList;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.processor.PrepareProcessTask;
import version2.prototype.util.FileSystem;

public class ModisNBARV6PrepareProcessTask extends PrepareProcessTask {

    public ModisNBARV6PrepareProcessTask(ProjectInfoFile mProject,
            ProjectInfoPlugin pPlugin, PluginMetaData plugin, DataDate mDate) {
        super(mProject, pPlugin, plugin, mDate);
    }

    @Override
    public String[] getInputFolders(int stepId) {
        ArrayList<String> folders = new ArrayList<String>();

        // Format: Input (of this step) -> Output (of this step)
        switch(stepId)
        {
        case 1:
            // Download -> MosaicData
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Download", date.getYear(), date.getDayOfYear()));
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "QCDownload", date.getYear(), date.getDayOfYear()));
            break;
        case 2:
            // QCDownload -> MosaicQC
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "QCDownload", date.getYear(), date.getDayOfYear()));
        case 3:
            // Mosaic -> Filter
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "MosaicData", date.getYear(), date.getDayOfYear()));
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "MosaicQC", date.getYear(), date.getDayOfYear()));
            break;
        case 4:
            // Filter -> Reproject
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Filter", date.getYear(), date.getDayOfYear()));
            break;
        case 5:
            // Reproject -> Mask
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Reproject", date.getYear(), date.getDayOfYear()));
            break;
        case 6:
            // Mask -> Clip
            folders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Mask", date.getYear(), date.getDayOfYear()));
            break;
        default:
            folders = null;
            break;
        }
        return folders.toArray(new String[folders.size()]);
    }

    @Override
    public String getOutputFolder(int stepId) {
        String outputFolder = "";

        // Format: Input (of this step) -> Output (of this step)
        switch(stepId)
        {
        case 1:
            // Download -> MozaicData
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "MosaicData", date.getYear(), date.getDayOfYear());
            break;
        case 2:
            // QCDownload -> MozaicQC
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "MosaicQC", date.getYear(), date.getDayOfYear());
            break;
        case 3:
            // Mozaic -> Filter
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Filter", date.getYear(), date.getDayOfYear());
            break;
        case 4:
            // Filter -> Reproject
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Reproject", date.getYear(), date.getDayOfYear());
            break;
        case 5:
            // Reproject -> Mask
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Mask", date.getYear(), date.getDayOfYear());
            break;
        case 6:
            // Mask -> Output (Clip)
            outputFolder = String.format("%s%04d" + File.separator+"%03d",
                    FileSystem.GetProcessOutputDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    date.getYear(), date.getDayOfYear());
            break;
        default:
            outputFolder = null;
            break;
        }

        return outputFolder;
    }

    @Override
    // bands 8 - 14
    public int[] getDataBands() {
        return new int[] {1, 2, 3, 4, 5, 6, 7};
    }

    @Override
    // bands 12 - 18
    public int[] getQCBands() {
        return new int[] {1, 2, 3, 4, 5, 6, 7};
    }
}
