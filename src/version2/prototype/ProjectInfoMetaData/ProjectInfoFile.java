package version2.prototype.ProjectInfoMetaData;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import jdk.nashorn.internal.ir.annotations.Immutable;
import version2.prototype.Config;
import version2.prototype.Projection;
import version2.prototype.Projection.ResamplingType;
import version2.prototype.util.FileSystem;
import version2.prototype.ZonalSummary;

/**
 * Represents data parsed from a project info xml file. Does not update itself with changes in the xml or update the xml with its changes.
 *
 * @author michael.devos
 *
 */
@Immutable public class ProjectInfoFile {
    private final Config configInstance;
    private static DateTimeFormatter datesFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz uuuu");
    private DocumentBuilderFactory domFactory;
    private DocumentBuilder builder;
    public Document doc;

    /**
     * XML filepath of the file that was parsed to create this ProjectInfoFile object. Null if this object is custom made instead of from a parsed xml file.
     */
    public final String xmlLocation;
    /**
     * True if there has been an error during parsing.
     */
    public boolean error;

    //    private final String rootElement = "ProjectInfo";
    private final ArrayList<ProjectInfoPlugin> plugins;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String projectName;
    private final String workingDir;
    private final String maskingFile;
    private final Integer maskingResolution;
    private final String masterShapeFile;
    private final String timeZone;
    private final Boolean clipping;
    private final Projection projection;
    private final LocalDate heatingDate;
    private final Double heatingDegree;
    private final LocalDate freezingDate;
    private final Double coolingDegree;
    private final ArrayList<ProjectInfoSummary> summaries;
    private final String fullPath;

    private ArrayList<String> errorMsg;

    /**
     * Creates a ProjectInfoFile object from parsing the given xml file. Doesn't allow its data to be changed and doesn't dynamically update its
     * data if the file changes.
     *
     * @param configInstance
     * @param xmlLocation  - file path of xml to parse
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    public ProjectInfoFile(Config configInstance, String xmlLocation) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException,
    NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        this.configInstance = configInstance;
        error = false;
        errorMsg = new ArrayList<String>();
        this.xmlLocation = xmlLocation;
        domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        builder = domFactory.newDocumentBuilder();
        doc = builder.parse(xmlLocation);
        doc.getDocumentElement().normalize();

        // Get file data
        plugins = ReadPlugins();
        startDate = ReadStartDate();
        endDate = ReadEndDate();//LocalDate.now();
        projectName = ReadProjectName();
        workingDir = ReadWorkingDir();
        maskingFile = ReadMaskingFile();
        maskingResolution = ReadMaskingResolution();
        masterShapeFile = ReadMasterShapeFile();
        timeZone = ReadTimeZone();
        clipping = ReadClipping();
        projection = new Projection(ReadResamplingType(), ReadPixelSize());
        freezingDate = ReadFreezingDate();
        coolingDegree = ReadCoolingDegree();
        heatingDate = ReadHeatingDate();
        heatingDegree = ReadHeatingDegree();
        summaries = ReadSummaries();
        fullPath = ReadFullPath();
    }

    public ProjectInfoFile(Config configInstance, String xmlLocation, boolean HasEndDate) throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException,
    NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        this.configInstance = configInstance;
        error = false;
        errorMsg = new ArrayList<String>();
        this.xmlLocation = xmlLocation;
        domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        builder = domFactory.newDocumentBuilder();
        doc = builder.parse(xmlLocation);
        doc.getDocumentElement().normalize();

        // Get file data
        plugins = ReadPlugins();
        startDate = ReadStartDate();
        endDate = ReadEndDate();
        projectName = ReadProjectName();
        workingDir = ReadWorkingDir();
        maskingFile = ReadMaskingFile();
        maskingResolution = ReadMaskingResolution();
        masterShapeFile = ReadMasterShapeFile();
        timeZone = ReadTimeZone();
        clipping = ReadClipping();
        projection = new Projection(ReadResamplingType(), ReadPixelSize());
        freezingDate = ReadFreezingDate();
        coolingDegree = ReadCoolingDegree();
        heatingDate = ReadHeatingDate();
        heatingDegree = ReadHeatingDegree();
        summaries = ReadSummaries();
        fullPath = ReadFullPath();
    }

    /**
     * Creates a fully custom ProjectInfoFile object from the given parameters. Meant for testing purposes.
     *
     * @param plugins
     * @param startDate
     * @param projectName
     * @param workingDir
     * @param maskingFile
     * @param maskingResolution
     * @param masterShapeFile
     * @param timeZone
     * @param clipping
     * @param projection
     * @param freezingDate
     * @param coolingDegree
     * @param heatingDate
     * @param heatingDegree
     * @param summaries
     */
    @SuppressWarnings("unchecked")
    public ProjectInfoFile(ArrayList<ProjectInfoPlugin> plugins, LocalDate startDate, LocalDate endDate, String projectName, String workingDir, String maskingFile, Integer maskingResolution, String masterShapeFile,
            String timeZone, Boolean clipping, Projection projection, LocalDate freezingDate, Double coolingDegree, LocalDate heatingDate, Double heatingDegree,
            ArrayList<ProjectInfoSummary> summaries)
    {
        configInstance = null;
        xmlLocation = null;
        this.plugins = (ArrayList<ProjectInfoPlugin>) plugins.clone();
        this.startDate = startDate;
        this.endDate = endDate;
        this.projectName = projectName;
        this.workingDir = workingDir;
        this.maskingFile = maskingFile;
        this.maskingResolution = maskingResolution;
        this.masterShapeFile = masterShapeFile;
        this.timeZone = timeZone;
        this.clipping = clipping;
        this.projection = new Projection(projection);
        this.freezingDate = freezingDate;
        this.coolingDegree = coolingDegree;
        this.heatingDate = heatingDate;
        this.heatingDegree = heatingDegree;
        this.summaries = (ArrayList<ProjectInfoSummary>) summaries.clone();
        fullPath = ReadFullPath();
    }

    /**
     * Gets an ArrayList of ProjectInfoPlugins gotten from the once parsed xml file.
     *
     * @return list of ProjectInfoPlugin objects that could be created from the xml's data.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<ProjectInfoPlugin> GetPlugins() { return (ArrayList<ProjectInfoPlugin>) plugins.clone(); }

    /**
     * Gets the start date gotten from the once parsed xml file.
     *
     * @return Date object representing the start date that could be created from the xml's data.
     */
    public LocalDate GetStartDate() { return startDate; }

    public LocalDate GetEndDate(){ return endDate; }

    /**
     * Gets the project name gotten from the once parsed xml file.
     *
     * @return project name gotten from the xml's data.
     */
    public String GetProjectName() { return FileSystem.StandardizeName(projectName); }

    /**
     * Gets the working directory gotten from the once parsed xml file.
     *
     * @return working directory string gotten from the xml's data.
     */
    public String GetWorkingDir() { return FileSystem.CheckDirPath(workingDir); }

    /**
     * Gets the path to masking file gotten from the once parsed xml file.
     *
     * @return masking file path string gotten from the xml's data.
     */
    public String GetMaskingFile() { return maskingFile; }

    /**
     * Gets the path to masking resolution gotten from the once parsed xml file.
     *
     * @return masking resolution size gotten from the xml's data.
     */
    public Integer GetMaskingResolution() { return maskingResolution; }

    /**
     * Gets the master shape file gotten from the once parsed xml file. This shape file is not meant to be used in zonal summary calculations.
     *
     * @return master shape file path string gotten from the xml's data.
     */
    public String GetMasterShapeFile() { return masterShapeFile; }

    /**
     * Gets the time zone gotten from the once parsed xml file.
     *
     * @return time zone string gotten from the xml's data.
     */
    public String GetTimeZone() { return timeZone; }

    /**
     * Gets the clipping gotten from the once parsed xml file.
     *
     * @return get clipping value gotten from the xml's data.
     */
    public boolean GetClipping() { return clipping; }

    /**
     * Gets the list of summaries gotten from the once parsed xml file.
     *
     * @return list of ProjectInfoSummary objects created from the xml's data.
     */
    @SuppressWarnings("unchecked")
    public ArrayList<ProjectInfoSummary> GetSummaries() { return (ArrayList<ProjectInfoSummary>) summaries.clone(); }

    /**
     * Gets the projection information gotten from the once parsed xml file.
     *
     * @return Projection object created from the xml's data.
     */
    public Projection GetProjection() { return new Projection(projection); }

    /**
     * Gets the freezing date gotten from the once parsed xml file.
     *
     * @return Date - the freezing date read from the xml's data.
     */
    public LocalDate GetFreezingDate() { return freezingDate; }

    /**
     * Gets the cooling degree gotten from the once parsed xml file.
     *
     * @return Double - the cooling degree read from the xml's data.
     */
    public Double GetCoolingDegree() { return coolingDegree; }

    /**
     * Gets the heating date gotten from the once parsed xml file.
     *
     * @return Date - the heating date read from the xml's data.
     */
    public LocalDate GetHeatingDate() { return heatingDate; }

    /**
     * Gets the heating degree gotten from the once parsed xml file.
     *
     * @return Double - the heating degree read from the xml's data.
     */
    public Double GetHeatingDegree() { return heatingDegree; }

    /**
     * List of error messages if any. Empty if no errors.
     *
     * @return list of error message strings if any
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> GetErrorMessages() { return (ArrayList<String>) errorMsg.clone(); }

    private ArrayList<ProjectInfoPlugin> ReadPlugins()
    {
        ArrayList<ProjectInfoPlugin> plugins = new ArrayList<ProjectInfoPlugin>();
        String name;
        String qc;
        ArrayList<String> inidicies = null;
        ArrayList<String> modisTiles = null;

        NodeList pluginList = GetUpperLevelNodeListIgnoreIfEmpty("Plugin", "Missing plugins.", "Plugins");
        if(pluginList != null)
        {
            for(int i=0; i < pluginList.getLength(); i++)
            {
                Element plugin = ((Element)pluginList.item(i));

                name = plugin.getAttribute("name");

                ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(plugin.getElementsByTagName("QC"),
                        "Missing QC for plugin '" + name + "'.");
                if(values.size() > 0) {
                    qc = values.get(0);
                } else {
                    qc = null;
                }

                values = GetNodeListValuesIgnoreIfEmpty(plugin.getElementsByTagName("Indices"), "Missing indices for plugin '" + name + "'.");
                if(values.size() > 0) {
                    inidicies = values;
                } else {
                    inidicies = null;
                }

                values = GetNodeListValuesIgnoreIfEmpty(plugin.getElementsByTagName("ModisTile"), "Missing modis tile(s) for plugin '" + name + "'.");
                if(values.size() > 0) {
                    modisTiles = values;
                } else {
                    modisTiles = null;
                }

                plugins.add(new ProjectInfoPlugin(name, inidicies, qc, modisTiles));
            }
        }

        return plugins;
    }

    public String GetFullPath() { return fullPath; }

    public String GetParentProjectName(){
        String projectName = null;
        String fullPath = GetFullPath();
        if(fullPath.endsWith("\\") || fullPath.endsWith("/")){
            projectName = fullPath.substring(0, -1).replace('\\', '/');
        }
        else{
            projectName = fullPath.replace('\\', '/');
        }
        projectName = projectName.substring(projectName.lastIndexOf('/')+1);
        return projectName;
    }
    private LocalDate ReadStartDate() throws DateTimeParseException
    {
        NodeList nodes = GetUpperLevelNodeList("StartDate", "Missing start date.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing start date.");
        if(values.size() > 0) {
            return LocalDate.parse(values.get(0), datesFormatter);
        }
        return null;
        //        } catch (DateTimeParseException e) {
        //            error = true;
        //            errorMsg.add(e.getMessage());
        //            return null;
        //        }
    }

    private LocalDate ReadEndDate() throws DateTimeParseException{
        NodeList nodes = GetUpperLevelNodeListIgnoreIfEmpty("EndDate", "Missing end date.");
        LocalDate now = LocalDate.now();
        if(nodes!=null){
            ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(nodes, "Missing end date.");
            if(values.size() > 0) {
                return LocalDate.parse(values.get(0), datesFormatter);
            }
        } else {
            return now;//LocalDate.parse(now.toString("EEE MMM dd HH:mm:ss zzz yyyy"), datesFormatter);
        }
        return now;
    }

    private String ReadFullPath(){
        if(projectName.contains("-SUB")){
            String parentName = projectName.substring(0, projectName.lastIndexOf("-"));
            return workingDir.replace('\\', '/')+"/Projects/"+parentName;
        }
        else if(projectName.contains("_SUB")){
            String parentName = projectName.substring(0, projectName.lastIndexOf("_"));
            return workingDir.replace('\\', '/')+"/Projects/"+parentName;
        }
        else{
            /* if(workingDir.endsWith("\\") || workingDir.endsWith("/")){
                String temp = workingDir.substring(0, workingDir.length()-1);
                return temp.replace('\\', '/')+"/Projects/"+projectName;
            } else {*/
            //return workingDir.replace('\\', '/')+"/Projects/"+projectName;
            return workingDir+"\\Projects\\"+projectName;
            // }
        }
    }

    private String ReadProjectName()
    {
        NodeList nodes = GetUpperLevelNodeList("ProjectName", "Missing project name.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing project name.");
        if(values.size() > 0) {
            return values.get(0);
        }
        return null;
    }

    private String ReadWorkingDir()
    {
        NodeList nodes = GetUpperLevelNodeList("WorkingDir", "Missing working directory.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing working directory.");
        if(values.size() > 0) {
            return values.get(0);
        }
        return null;
    }

    private String ReadMaskingFile()
    {
        NodeList nodes = GetUpperLevelNodeListIgnoreIfEmpty("File", "Missing masking file.", "Masking");
        ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(nodes, "Missing masking file.");
        if(values.size() > 0) {
            String temp = values.get(0);
            if(temp.startsWith("\\") || temp.startsWith("/"))
            {
                temp = System.getProperty("user.dir") + temp;
            }
            return temp;
        }
        return null;
    }

    private Integer ReadMaskingResolution()
    {
        NodeList nodes = GetUpperLevelNodeListIgnoreIfEmpty("Resolution", "Missing masking resolution.", "Masking");
        ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(nodes, "Missing masking resolution.");
        if(values.size() > 0) {
            return Integer.parseInt(values.get(0));
        }
        return null;
    }

    private String ReadMasterShapeFile()
    {
        NodeList nodes = GetUpperLevelNodeList("MasterShapeFile", "Missing master shape file.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing master shape file.");
        if(values.size() > 0) {
            String temp = values.get(0);
            if(temp.startsWith("\\") || temp.startsWith("/"))
            {
                temp = System.getProperty("user.dir") + temp;
            }
            return temp;
        }
        return null;
    }

    private String ReadTimeZone()
    {
        NodeList nodes = GetUpperLevelNodeList("TimeZone", "Missing time zone.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing time zone.");
        if(values.size() > 0) {
            if(values.get(0).indexOf(")") > -1) {
                return values.get(0).substring(values.get(0).indexOf(") ") + 2);
            } else {
                return values.get(0);
            }
        }
        return null;
    }

    private boolean ReadClipping()
    {
        NodeList nodes = GetUpperLevelNodeList("Clipping", "Missing clipping.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing clipping.");
        if(values.size() > 0) {
            return Boolean.valueOf(values.get(0));
        }
        return false;   // Default to false when element is missing.
    }

    private ResamplingType ReadResamplingType()
    {
        NodeList nodes = GetUpperLevelNodeList("ReSampling", "Missing resampling.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing resampling.");
        if(values.size() > 0) {
            ResamplingType rType = null;
            switch(values.get(0))
            {
            case "NEAREST_NEIGHBOR":
                rType = ResamplingType.NEAREST_NEIGHBOR;
                break;
            case "BILINEAR":
                rType = ResamplingType.BILINEAR;
                break;
            case "CUBIC_CONVOLUTION":
                rType = ResamplingType.CUBIC_CONVOLUTION;
                break;
            }
            return rType;
        }
        return null;
    }

    private int ReadPixelSize()
    {
        NodeList nodes = GetUpperLevelNodeList("PixelSize", "Missing PixelSize.");
        ArrayList<String> values = GetNodeListValues(nodes, "Missing PixelSize.");
        if(values.size() > 0) {
            return Integer.parseInt(values.get(0));
        }
        return 0;
    }

    private LocalDate ReadFreezingDate() throws DateTimeParseException
    {
        NodeList nodes = GetUpperLevelNodeListIgnoreIfEmpty("FreezingDate", "Missing freezing date.");
        // e.g. "Wed May 20 21:21:36 CDT 2015"
        ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(nodes, "Missing freezing date.");
        if(values.size() > 0) {
            return LocalDate.parse(values.get(0), datesFormatter);
        }
        return null;
    }

    private Double ReadCoolingDegree()
    {
        NodeList nodes = GetUpperLevelNodeListIgnoreIfEmpty("CoolingDegree", "Missing cooling degree.");
        ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(nodes, "Missing cooling degree.");
        if(values.size() > 0) {
            return Double.parseDouble(values.get(0));
        }
        return 0.0;
    }

    private LocalDate ReadHeatingDate() throws DateTimeParseException
    {
        NodeList nodes = GetUpperLevelNodeListIgnoreIfEmpty("HeatingDate", "Missing heating date.");
        // e.g. "Wed May 20 21:21:36 CDT 2015"
        ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(nodes, "Missing start date.");
        if(values.size() > 0) {
            return LocalDate.parse(values.get(0), datesFormatter);
        }
        return null;
    }

    private Double ReadHeatingDegree()
    {
        NodeList nodes = GetUpperLevelNodeListIgnoreIfEmpty("HeatingDegree", "Missing heating degree.");
        ArrayList<String> values = GetNodeListValuesIgnoreIfEmpty(nodes, "Missing heating degree.");
        if(values.size() > 0) {
            return Double.parseDouble(values.get(0));
        }
        return 0.0;
    }

    private ArrayList<ProjectInfoSummary> ReadSummaries() throws ClassNotFoundException, NoSuchMethodException, SecurityException,
    InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
    {
        ArrayList<ProjectInfoSummary> summaries = new ArrayList<ProjectInfoSummary>();

        NodeList summaryList = GetUpperLevelNodeListIgnoreIfEmpty("Summary", "Missing zonal summaries.", "Summaries");
        ArrayList<String> summaryStrings = GetNodeListValuesIgnoreIfEmpty(summaryList, "Missing zonal summaries.");
        if(summaryStrings.size() > 0) {
            int ID;
            String shapefile;
            String areaValueField;
            String areaNameField;
            String temporalSummaryCompositionStrategyClassName;

            String summary;
            for(int i=0; i < summaryStrings.size(); i++)
            {
                summary = summaryStrings.get(i);
                ID = Integer.parseInt(((Element)summaryList.item(i)).getAttribute("ID"));
                // Shape File Path: C:\Users\sufi\Desktop\shapefile\shapefile.shp; Field: COUNTYNS10; Temporal Summary: GregorianWeeklyStrategy
                // Shape File Path: C:\Users\sufi\Desktop\shapefile\shapefile.shp; COUNTYNS10

                areaNameField = summary.substring(summary.indexOf(ProjectInfoSummary.AREA_NAME_FIELD_TAG + ": ") + String.valueOf(ProjectInfoSummary.AREA_NAME_FIELD_TAG + ": ").length(),
                        summary.indexOf(";"));
                shapefile = summary.substring(summary.indexOf(ProjectInfoSummary.SHAPE_FILE_TAG + ": ") + String.valueOf(ProjectInfoSummary.SHAPE_FILE_TAG + ": ").length(), summary.indexOf(";",
                        summary.indexOf(ProjectInfoSummary.SHAPE_FILE_TAG + ": ")));
                if(shapefile.startsWith("\\") || shapefile.startsWith("/"))
                {
                    shapefile = System.getProperty("user.dir") + shapefile;
                }
                if(summary.indexOf(ProjectInfoSummary.TEMPORAL_SUMMARY_TAG) == -1)
                {
                    areaValueField = summary.substring(summary.indexOf(ProjectInfoSummary.AREA_CODE_FIELD_TAG + ": ") + String.valueOf(ProjectInfoSummary.AREA_CODE_FIELD_TAG + ": ").length());
                    if(areaValueField.endsWith(";")) {
                        areaValueField = areaValueField.substring(0, areaValueField.length() - 1);
                    }
                    temporalSummaryCompositionStrategyClassName = null;
                }
                else
                {
                    areaValueField = summary.substring(summary.indexOf(ProjectInfoSummary.AREA_CODE_FIELD_TAG + ": ") + String.valueOf(ProjectInfoSummary.AREA_CODE_FIELD_TAG + ": ").length(),
                            summary.indexOf(";", summary.indexOf(ProjectInfoSummary.AREA_CODE_FIELD_TAG + ": ")));
                    temporalSummaryCompositionStrategyClassName = summary.substring(summary.indexOf(ProjectInfoSummary.TEMPORAL_SUMMARY_TAG + ": ") +
                            String.valueOf(ProjectInfoSummary.TEMPORAL_SUMMARY_TAG + ": ").length());
                    if(temporalSummaryCompositionStrategyClassName.endsWith(";")) {
                        temporalSummaryCompositionStrategyClassName = temporalSummaryCompositionStrategyClassName.substring(0, temporalSummaryCompositionStrategyClassName.length() - 1);
                    }

                    boolean valid = false;
                    for(String strategyName : configInstance.getSummaryTempCompStrategies())
                    {
                        if(temporalSummaryCompositionStrategyClassName.equals(strategyName)) {
                            valid = true;
                            break;
                        }
                    }
                    if(!valid) {
                        temporalSummaryCompositionStrategyClassName = null;
                    }
                }
                summaries.add(new ProjectInfoSummary(new ZonalSummary(shapefile, areaValueField, areaNameField), temporalSummaryCompositionStrategyClassName, ID));
            }
        }
        return summaries;
    }

    /**
     * Used to get the NodeList object pertaining to an element containing the list of elements desired. Used even if desired element does not
     * contain a list of elements but only the value desired. Send the returned NodeList object into {@link #GetNodeListValues(NodeList, String)
     * GetNodeListValues(NodeList, String)} to get its value(s).
     *
     * @param element
     * @param errorMsg
     * @param parents
     * @return
     */
    private NodeList GetUpperLevelNodeList(String element, String errorMsg, String...parents)
    {
        NodeList nodes = null;

        for(String parent : parents)
        {
            if(nodes == null) {
                nodes = doc.getElementsByTagName(parent);
            } else {
                nodes = ((Element)nodes.item(0)).getElementsByTagName(parent);
            }
        }

        // Failed to find specified parent
        if(((nodes == null) && (parents.length > 0)) || ((nodes != null) && (nodes.getLength() <= 0)))
        {
            error = true;
            this.errorMsg.add(errorMsg);
            return null;
        }
        else if(nodes == null) {
            nodes = doc.getElementsByTagName(element);
        } else {
            nodes = ((Element)nodes.item(0)).getElementsByTagName(element);
        }

        return nodes;
    }

    /**
     * Used to get the NodeList object pertaining to an element containing the list of elements desired. Used even if desired element does not
     * contain a list of elements but only the value desired. Send the returned NodeList object into {@link #GetNodeListValues(NodeList, String)
     * GetNodeListValues(NodeList, String)} to get its value(s).
     *
     * @param element
     * @param errorMsg
     * @param parents
     * @return
     */
    private NodeList GetUpperLevelNodeListIgnoreIfEmpty(String element, String errorMsg, String...parents)
    {
        NodeList nodes = null;

        for(String parent : parents)
        {
            if(nodes == null) {
                nodes = doc.getElementsByTagName(parent);
            } else {
                nodes = ((Element)nodes.item(0)).getElementsByTagName(parent);
            }
        }

        // Failed to find specified parent
        if(((nodes == null) && (parents.length > 0)) || ((nodes != null) && (nodes.getLength() <= 0)))
        {
            //            error = true;
            //            this.errorMsg.add(errorMsg);
            return null;
        }
        else if(nodes == null) {
            nodes = doc.getElementsByTagName(element);
        } else {
            nodes = ((Element)nodes.item(0)).getElementsByTagName(element);
        }

        return nodes;
    }

    /**
     * Used to get the values of the NodeList that's passed in. Used for both single value and multiple value cases where there is no lower
     * hierarchy of elements (i.e. the passed in NodeList must be the lowest elements in the list).
     *
     * @param nList
     * @param errorMsg
     * @return
     */
    private ArrayList<String> GetNodeListValues(NodeList nList, String errorMsg)
    {
        ArrayList<String> values = new ArrayList<String>();
        if(nList != null)
        {
            for(int i=0; i < nList.getLength(); i++)
            {
                Element e = (Element)nList.item(i);
                NodeList list = e.getChildNodes();
                Node n = list.item(0);
                if(n != null)
                {
                    n.normalize();
                    values.add(n.getNodeValue().trim());
                }
            }
        }
        if((nList != null) && (values.size() == 0))
        {
            error = true;
            this.errorMsg.add(errorMsg);
        }
        return values;
    }

    /**
     * Used to get the values of the NodeList that's passed in. Used for both single value and multiple value cases where there is no lower
     * hierarchy of elements (i.e. the passed in NodeList must be the lowest elements in the list).
     *
     * @param nList
     * @param errorMsg
     * @return
     */
    private ArrayList<String> GetNodeListValuesIgnoreIfEmpty(NodeList nList, String errorMsg)
    {
        ArrayList<String> values = new ArrayList<String>();
        if(nList != null && nList.getLength() > 0)
        {
            for(int i=0; i < nList.getLength(); i++)
            {
                Element e = (Element)nList.item(i);
                NodeList list = e.getChildNodes();
                Node n = list.item(0);
                if(n != null)
                {
                    n.normalize();
                    values.add(n.getNodeValue().trim());
                }
            }
        }
        return values;
    }
}
