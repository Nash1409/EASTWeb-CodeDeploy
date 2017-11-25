package version2.prototype.processor.IMERG;

import java.io.File;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.processor.PrepareProcessTask;
import version2.prototype.util.FileSystem;

public class IMERGPrepareProcessTask extends PrepareProcessTask{

    private String [] inputFolders;
    private String outputFolder;

    public IMERGPrepareProcessTask(ProjectInfoFile mProject,
            ProjectInfoPlugin pPlugin, PluginMetaData plugin, DataDate mDate) {
        super(mProject, pPlugin, plugin, mDate);
    }

    @Override
    public String[] getInputFolders(int stepId) {

        inputFolders = new String[1];

        /*
        Step1: Convert: Download->Convert
        Step2: Reproject: Convert->Reproject
        Step3: Clip: Reproject->Clip
        Step4: Mask: Clip->Mask
         */
        switch (stepId){
        case 1:
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "download", date.getYear(), date.getDayOfYear());
            break;
        case 2:
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "convert", date.getYear(), date.getDayOfYear());
            break;
        case 3:
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "reproject", date.getYear(), date.getDayOfYear());
            break;
        case 4:
            inputFolders[0] = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "clip", date.getYear(), date.getDayOfYear());
            break;
        default:
            break;
        }
        return inputFolders;
    }

    @Override
    public String getOutputFolder(int stepId) {
        outputFolder = null;

        /*
        Step1: Convert: Download->Convert
        Step2: Reproject: Convert->Reproject
        Step3: Clip: Reproject->Clip
        Step4: Mask: Clip->Mask
         */
        switch (stepId){
        case 1:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "convert", date.getYear(), date.getDayOfYear());
            break;
        case 2:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "reproject", date.getYear(), date.getDayOfYear());
            break;
        case 3:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "clip", date.getYear(), date.getDayOfYear());
            break;
        case 4:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessOutputDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "mask", date.getYear(), date.getDayOfYear());
            break;
        default:
            outputFolder = String.format("%s%04d" + File.separator + "%03d",
                    FileSystem.GetProcessOutputDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    date.getYear(), date.getDayOfYear());
        }

        return outputFolder;
    }

    @Override
    public int[] getDataBands() {
        return null;
    }

    @Override
    public int[] getQCBands() {
        return null;
    }

}
