package version2.prototype.Scheduler;

import java.io.File;
import java.io.IOException;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import version2.prototype.PluginMetaData.PluginMetaDataCollection;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;

/**
 * @author michael.devos
 *
 */
public class SchedulerData {
    /**
     * The project metadata to use within the Scheduler this object is sent to.
     */
    public final ProjectInfoFile projectInfoFile;
    /**
     * The PluginMetaDataCollection to use within the Scheduler this object is sent to.
     */
    public final PluginMetaDataCollection pluginMetaDataCollection;
    /**
     * TRUE if directories/files in Process temp directories and in output directories for Download, Processor, & Indices should be deleted once no longer needed.
     */
    public final boolean clearIntermediateFiles;

    /**
     * Creates a SchedulerData object containing the project's metadata and the collection of available plugin metadata.
     *
     * @param projectInfoFile  - the project metadata to use within the Scheduler this object is sent to
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws DOMException
     * @throws PatternSyntaxException
     */
    public SchedulerData(ProjectInfoFile projectInfoFile) throws PatternSyntaxException, DOMException, ParserConfigurationException, SAXException, IOException
    {
        this(projectInfoFile, false);
    }

    /**
     * Creates a SchedulerData object containing the project's metadata and the collection of available plugin metadata.
     *
     * @param projectInfoFile  - the project metadata to use within the Scheduler this object is sent to
     * @param clearIntermediateFiles  - TRUE if directories/files in Process temp directories and in output directories for Download, Processor, & Indices should be deleted once
     * no longer needed, otherwise FALSE.
     * @param projectMetaData
     * @param clearIntermediateFiles
     * @throws PatternSyntaxException
     * @throws DOMException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public SchedulerData(ProjectInfoFile projectMetaData, boolean clearIntermediateFiles) throws PatternSyntaxException, DOMException, ParserConfigurationException, SAXException,
    IOException
    {
        projectInfoFile = projectMetaData;
        this.clearIntermediateFiles = clearIntermediateFiles;
        pluginMetaDataCollection = PluginMetaDataCollection.getInstance();
    }

    /**
     * Creates a SchedulerData object containing the project's metadata and the collection of available plugin metadata.
     *
     * @param projectInfoFile  - the project metadata to use within the Scheduler this object is sent to
     * @param pluginMetaDataCollection  - the PluginMetaDataCollection to use within the Scheduler this object is sent to
     * @param clearIntermediateFiles  - TRUE if directories/files in Process temp directories and in output directories for Download, Processor, & Indices should be deleted once
     * no longer needed, otherwise FALSE.
     */
    public SchedulerData(ProjectInfoFile projectInfoFile, PluginMetaDataCollection pluginMetaDataCollection, boolean clearIntermediateFiles)
    {
        this.projectInfoFile = projectInfoFile;
        this.pluginMetaDataCollection = pluginMetaDataCollection;
        this.clearIntermediateFiles = clearIntermediateFiles;
    }

    /**
     * Creates a SchedulerData object containing the given project metadata and the plugin metadata from the given file path.
     *
     * @param projectInfoFile  - the project metadata to use within the Scheduler this object is sent to
     * @param pluginMetaDataFile  - The path to the plugin metadata xml to use within the Scheduler this object is sent to. If an empty string then pluginMetaDataCollection will
     * be null.
     * @param clearIntermediateFiles  - TRUE if directories/files in Process temp directories and in output directories for Download, Processor, & Indices should be deleted once
     * no longer needed, otherwise FALSE.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws DOMException
     * @throws PatternSyntaxException
     */
    public SchedulerData(ProjectInfoFile projectInfoFile, String pluginMetaDataFile, boolean clearIntermediateFiles) throws ParserConfigurationException, SAXException, IOException,
    DOMException, PatternSyntaxException
    {
        this.projectInfoFile = projectInfoFile;
        if(pluginMetaDataFile != null) {
            if(pluginMetaDataFile.isEmpty()) {
                pluginMetaDataCollection = null;
            } else {
                pluginMetaDataCollection = PluginMetaDataCollection.getInstance(new File(pluginMetaDataFile));
            }
        } else {
            pluginMetaDataCollection = null;
        }
        this.clearIntermediateFiles = clearIntermediateFiles;
    }
}
