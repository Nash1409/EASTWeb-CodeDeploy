package version2.prototype.processor.NldasNOAH;

import java.io.File;
import java.util.ArrayList;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.processor.PrepareProcessTask;
import version2.prototype.util.FileSystem;

public class NldasNOAHPrepareProcessTask extends PrepareProcessTask {

    public NldasNOAHPrepareProcessTask(ProjectInfoFile mProject,
            ProjectInfoPlugin pPlugin, PluginMetaData plugin, DataDate mDate) {
        super(mProject, pPlugin, plugin, mDate);
    }

    @Override
    public String[] getInputFolders(int stepId) {
        // TODO Auto-generated method stub

        ArrayList<String> inputFolders = new ArrayList<String>();
        /*
        Step1: Composite: Download->Composite
        Step2: Reproject: Composite->Reproject
        Step3: Clip: Reproject->Clip
        Step4: Mask: Clip->Mask
         */
        switch(stepId)
        {
        case 1:
            inputFolders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Download", date.getYear(), date.getDayOfYear()));
            break;
        case 2:
            inputFolders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Composite", date.getYear(), date.getDayOfYear()));
            break;
        case 3:
            inputFolders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Reproject", date.getYear(), date.getDayOfYear()));
            break;
        case 4:
            inputFolders.add(String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Clip", date.getYear(), date.getDayOfYear()));
            break;
        default:
            inputFolders = null;
            break;
        }

        return inputFolders.toArray(new String[inputFolders.size()]);
    }

    @Override
    public String getOutputFolder(int stepId) {
        // TODO Auto-generated method stub
        String outputFolder = "";
        /*
        Step1: Composite: Download->Composite
        Step2: Reproject: Composite->Reproject
        Step3: Clip: Reproject->Clip
        Step4: Mask: Clip->Mask
         */

        switch(stepId)
        {
        case 1:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Composite", date.getYear(), date.getDayOfYear());
            break;
        case 2:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Reproject", date.getYear(), date.getDayOfYear());
            break;
        case 3:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Clip", date.getYear(), date.getDayOfYear());
            break;
        case 4:
            outputFolder = String.format("%s%s" + File.separator + "%04d" + File.separator+"%03d",
                    FileSystem.GetProcessWorkerTempDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    "Mask", date.getYear(), date.getDayOfYear());
            break;
        default:
            outputFolder = String.format("%s%04d" + File.separator+"%03d",
                    FileSystem.GetProcessOutputDirectoryPath(project.GetFullPath(), pPlugin.GetName(), ProcessName.PROCESSOR),
                    date.getYear(), date.getDayOfYear());
            break;
        }

        return outputFolder;
    }

    @Override
    public int[] getDataBands() {
        // TODO Auto-generated method stub
        /*
        NOAH
        -019 -> 85:TSOIL0_10cm:0-10cmSoiltemperature[K]
        -020 -> 85:TSOIL10_40cm:10-40cmSoiltemperature[K]
        -021 -> 85:TSOIL40_100cm:40-100cmSoiltemperature[K]
        -022 -> 85:TSOIL100_200cm:100-200cmSoiltemperature[K]
        -030 -> 151:LSOIL0_10cm:0-10cmLiquidSoilMoistureContent(non-frozen)[kg/m^2]
        -031 -> 151:LSOIL10_40cm:10-40cmLiquidSoilMoistureContent(non-frozen)[kg/m^2]
        -032 -> 151:LSOIL40_100cm:40-100cmLiquidSoilMoistureContent(non-frozen)[kg/m^2]
        -033 -> 151:LSOIL100_200cm:100-200cmLiquidSoilMoistureContent(non-frozen)[kg/m^2]
        -042 -> 66:SNOD:SnowDepth[m]
        -043 -> 238:SNOWC:SnowCover[fraction]
         */
        int[] dataBands = {19, 20, 21, 22, 30, 31, 32, 33, 42, 43};

        return dataBands;
    }

    @Override
    public int[] getQCBands() {
        // TODO Auto-generated method stub
        return null;
    }



}
