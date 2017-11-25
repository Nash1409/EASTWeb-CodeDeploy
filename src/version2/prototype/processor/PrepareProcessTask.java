package version2.prototype.processor;

import java.time.LocalDate;
import java.time.MonthDay;

import version2.prototype.DataDate;
import version2.prototype.Projection;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;

/* Overridden by each plugin's prepareProcessTask class
 * name convention:  Plugin_namePrepareProcessTask.class
 *
 * @Author:  Yi Liu
 */
public abstract class PrepareProcessTask {
    protected ProjectInfoFile project;
    protected final DataDate date;
    protected final ProjectInfoPlugin pPlugin;
    protected final PluginMetaData pluginInfo;

    public PrepareProcessTask(ProjectInfoFile mProject, ProjectInfoPlugin mPlugin,
            PluginMetaData plugin, DataDate mDate) {
        project = mProject;
        date = mDate;
        pPlugin = mPlugin;
        pluginInfo = plugin;
    }

    /* pre-condition: input the ID of the step specified in the plugin metadata
     * post-condition: return the temp folder(s) for getting the input files for processing step with "stepId"
     */
    public abstract String [] getInputFolders(int stepId);

    /* pre-condition: input the ID of the step specified in the plugin metadata
     * post-condition: return the temp folder to write the output to after the step with "stepId" is done
     */
    public abstract String getOutputFolder(int stepId);

    /* post-condition: return the bands of the data of the plugin */
    public abstract int [] getDataBands();

    /* post-condition:
     *          return the QC bands of the plugin if there is any
     *          if no QC bands, return null
     */
    public abstract int [] getQCBands();

    //post-condition: return the date of the file which is under processing
    public DataDate getDataDate()
    {   return date;       }

    /* post-condition: return the master shapefile given in the project */
    public String getShapeFile() {
        return project.GetMasterShapeFile();
    }

    /* post-condition:
     *          return the mask file given in the project if there is any
     */
    public String getMaskFile() {
        return project.GetMaskingFile();
    }

    // post-condition: return the set projection for the plugin in the project
    public Projection getProjection(){
        return project.GetProjection();
    }

    // post-condition: return the set qcLevel for the plugin in the project
    public String getQC(){
        return pPlugin.GetQC();
    }

    // post-condition: return the mask resolution for the maskfile in the project
    public Integer getMaskResolution()
    {       return project.GetMaskingResolution();  }

    // post-condition: return the data resolution of the plugin product
    public Integer getDataResolution()
    {   return pluginInfo.Resolution ;  }

    // post-condition: return true if clipping is needed; otherwise, false
    public Boolean getClipOrNot()
    {   return project.GetClipping();   }

    // post-condition: return month/day of freezing date use set in the project
    public MonthDay getFreezingDate()
    {
        LocalDate cDate = project.GetFreezingDate();
        return MonthDay.of(cDate.getMonthValue(), cDate.getDayOfMonth());
    }

    // post-condition: return month/day of heating date use set in the project
    public MonthDay getHeatingDate()
    {
        LocalDate hDate = project.GetHeatingDate();
        return MonthDay.of(hDate.getMonthValue(), hDate.getDayOfMonth());
    }

    public double getHeatingDegree()
    {   return project.GetHeatingDegree();  }

    public double getFreezingDegree()
    {   return project.GetCoolingDegree();  }

}
