package version2.prototype.processor.ModisLSTV6;

import java.io.File;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.processor.PrepareProcessTask;
import version2.prototype.util.FileSystem;


// Rewritten by Yi Liu

public class ModisLSTV6PrepareProcessTask extends PrepareProcessTask {

    public ModisLSTV6PrepareProcessTask(ProjectInfoFile mProject,
            ProjectInfoPlugin pPlugin, PluginMetaData plugin, DataDate mDate) {
        super(mProject, pPlugin, plugin, mDate);
    }

    @Override
    public String[] getInputFolders(int stepId) {
        String [] inputFolders =  new String [1];
        // Format: Input (of this step) -> Output (of this step)
        switch(stepId)
        {
        case 1:
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "download", date.getYear(), date.getDayOfYear());
            break;
        case 2:
            // Mozaic -> Filter
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Mozaic", date.getYear(), date.getDayOfYear());
            break;
        case 3:
            // Filter -> Reproject
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Filter", date.getYear(), date.getDayOfYear());
            break;
        case 4:
            // Reproject -> Mask
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Reproject", date.getYear(), date.getDayOfYear());
            break;
        case 5:
            // Reproject -> clip
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Mask", date.getYear(), date.getDayOfYear());
            break;
        default:
            break;
        }
        return inputFolders;
    }

    @Override
    public String getOutputFolder(int stepId) {
        String outputFolder = "";

        // Format: Input (of this step) -> Output (of this step)
        switch(stepId)
        {
        case 1:
            // Download -> Mozaic
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Mozaic", date.getYear(), date.getDayOfYear());
            break;
        case 2:
            // Mozaic -> Filter
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Filter", date.getYear(), date.getDayOfYear());
            break;
        case 3:
            // Filter -> Reproject
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Reproject", date.getYear(), date.getDayOfYear());
            break;
        case 4:
            // Reproject -> Mask
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Mask", date.getYear(), date.getDayOfYear());
            break;
        case 5:
            // Mask - >clip
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
    public int[] getDataBands() {
        return new int[] {1,5};
    }

    @Override
    public int[] getQCBands() {
        return new int[] {2,6};
    }
}

