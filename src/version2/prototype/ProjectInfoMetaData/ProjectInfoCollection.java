package version2.prototype.ProjectInfoMetaData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import version2.prototype.Config;
import version2.prototype.ErrorLog;
import version2.prototype.util.FileSystem;

/**
 * Collection handles reading in project xml files, parsing, and storing the list of ProjectInfoFile objects. Once read in the list will not be recreated unless
 * {@link #ClearProjectList() ClearProjectList()} is called.
 *
 * @author michael.devos
 */
public class ProjectInfoCollection {
    private static ArrayList<ProjectInfoFile> projects = null;
    private static Boolean projectsListLock = new Boolean(true);    // Attain lock on this object before proceeding to modify 'projects'

    /**
     * Retrieves the list of loaded ProjectInfoFile objects. If the list has not been loaded then it parses the project xml files and loads the successfully parsed files into the list as
     * ProjectInfoFile objects.
     *
     * @param configInstance  - Config instance to use
     * @return Cloned ArrayList of successfully loaded ProjectInfoFile objects
     */
    @SuppressWarnings("unchecked")
    public static ArrayList<ProjectInfoFile> GetAllProjectInfoFiles(Config configInstance)
    {
        ArrayList<ProjectInfoFile> projectListClone;
        synchronized(projectsListLock)
        {
            //if(projects == null)
            //{
            projects = new ArrayList<ProjectInfoFile>();
            File fileDir = new File(System.getProperty("user.dir") + "\\projects\\");
            File[] fl = getXMLFiles(fileDir);
            if(fl.length > 0)
            {
                ProjectInfoFile metaData;
                for(File fi : fl)
                {
                    if(fi.getName().contains("-SUB")) {
                        continue;
                    }
                    //System.out.print(fi.getName());
                    try{
                        metaData = new ProjectInfoFile(configInstance, fi.getCanonicalPath());
                        if(metaData.error) {
                            ErrorLog.add(Config.getInstance(), "Parsing failure" + (metaData.GetErrorMessages().size() > 1 ? "s" : "") + " for file project meta data file '" + fi.getName() + "'. "
                                    + metaData.GetErrorMessages().toString(),
                                    new Exception("Parsing failure" + (metaData.GetErrorMessages().size() > 1 ? "s" : "") + " for file project meta data file '" + fi.getName() + "'."));
                        } else {
                            projects.add(metaData);
                        }
                    } catch(Exception e) {
                        ErrorLog.add(Config.getInstance(), "Project meta data file, " + fi.getName() + " has an error in it.", e);
                    }
                }
            }
            //}

            projectListClone = (ArrayList<ProjectInfoFile>) projects.clone();
        }

        return projectListClone;
    }

    public static ArrayList<ProjectInfoFile> GetAllSubProjectsFiles(Config configInstance, String ProjectName)
    {
        ArrayList<ProjectInfoFile> subprojectsList;
        synchronized(projectsListLock)
        {
            boolean haveEndDate = true;
            projects = new ArrayList<ProjectInfoFile>();
            File fileDir = new File(System.getProperty("user.dir") + "\\projects\\");
            File[] fl = getXMLFiles(fileDir);
            if(fl.length > 0)
            {
                ProjectInfoFile metaData;
                for(File fi : fl)
                {
                    //System.out.println(ProjectName.trim()+"-SUB");
                    if(!fi.getName().replaceAll(" ", "_").contains(ProjectName.trim()+"-SUB")){
                        continue;
                    }
                    //System.out.println(fi.getName());
                    try{
                        metaData = new ProjectInfoFile(configInstance, fi.getCanonicalPath(),haveEndDate);
                        if(metaData.error) {
                            ErrorLog.add(Config.getInstance(), "Parsing failure" + (metaData.GetErrorMessages().size() > 1 ? "s" : "") + " for file project meta data file '" + fi.getName() + "'. "
                                    + metaData.GetErrorMessages().toString(),
                                    new Exception("Parsing failure" + (metaData.GetErrorMessages().size() > 1 ? "s" : "") + " for file project meta data file '" + fi.getName() + "'."));
                        } else {
                            projects.add(metaData);
                        }
                    } catch(Exception e) {
                        ErrorLog.add(Config.getInstance(), "Project meta data file, " + fi.getName() + " has an error in it.", e);
                    }
                }
            }
            subprojectsList = (ArrayList<ProjectInfoFile>) projects.clone();
        }
        return subprojectsList;
    }

    /**
     * Gets a single ProjectInfoFile object that is associated with the given projectName if such a project can be loaded.
     *
     * @param configInstance  - Config instance to use
     * @param projectName  - name of the project to search through loaded ProjectInfoFile objects for
     * @return  ProjectInfoFile object if project can be loaded, otherwise null
     */
    public static ProjectInfoFile GetProject(Config configInstance, String projectName)
    {
        String cleanedProjectName = FileSystem.StandardizeName(projectName);
        ArrayList<ProjectInfoFile> projectList = GetAllProjectInfoFiles(configInstance);
        ProjectInfoFile project;
        for(int i=0; i < projectList.size(); i++){
            project = projectList.get(i);
            if(project.GetProjectName() != null && project.GetProjectName().equalsIgnoreCase(cleanedProjectName)) {
                return project;
            }
        }
        return null;
    }

    /**
     * Clears the loaded project list forcing the next read from this list to have to recreate it from the list of project xml files.
     */
    public static void ClearProjectList()
    {
        synchronized(projectsListLock){
            projects = null;
        }
    }

    public static boolean WriteProjectToFile(Document doc, String projectName)
    {
        File theDir = new File(System.getProperty("user.dir") + "\\projects\\" + projectName + ".xml" );

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(theDir);

            transformer.transform(source, result);
            return true;
        } catch (TransformerException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static File[] getXMLFiles(File folder) {
        List<File> aList = new ArrayList<File>();
        File[] files = folder.listFiles();
        for (File pf : files) {

            if (pf.isFile() && getFileExtensionName(pf).indexOf("xml") != -1) {
                aList.add(pf);
            }
        }
        return aList.toArray(new File[aList.size()]);
    }

    private static String getFileExtensionName(File f) {
        if (f.getName().indexOf(".") == -1) {
            return "";
        } else {
            return f.getName().substring(f.getName().length() - 3, f.getName().length());
        }
    }
}
