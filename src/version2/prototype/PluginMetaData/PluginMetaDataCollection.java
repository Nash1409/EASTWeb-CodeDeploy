package version2.prototype.PluginMetaData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Handles reading in the plugin metadata from the xml files found in the package PluginMetaData.
 *
 * @author michael.devos
 * @author sufiabdul
 *
 */
public class PluginMetaDataCollection {
    private static PluginMetaDataCollection instance;
    /**
     * PluginMetaData objects are mapped based on the xml file name from which they were parsed from.
     * The string used is that of the file name but with the prepended "Plugin_" stripped out.
     * For the typical case, this would make the plugin's 'Title' element equal to this value.
     */
    public Map<String,PluginMetaData> pluginMetaDataMap;
    /**
     * List of plugin titles from loaded plugin metadata files.
     */
    public ArrayList<String> pluginList;

    /**
     * Gets the stored PluginMetaDataCollection instance or creates a new one if none exists with all the plugin metadata files read in.
     *
     * @return a PluginMetaDataCollection instance
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws DOMException
     * @throws PatternSyntaxException
     */
    public static PluginMetaDataCollection getInstance() throws PatternSyntaxException, DOMException, ParserConfigurationException, SAXException, IOException
    {
        if(instance == null) {
            File fileDir = new File(System.getProperty("user.dir") + "\\plugins\\");
            instance = new PluginMetaDataCollection(getXMLFiles(fileDir));
        }
        return instance;
    }

    /**
     * Gets the stored PluginMetaDataCollection instance or creates a new one if none exists with only the data read from the specified plugin metadata file.
     *
     * @param xmlFilePath  - the xml file path to read in
     * @return a PluginMetaDataCollection instance
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws DOMException
     * @throws PatternSyntaxException
     */
    public static PluginMetaDataCollection getInstance(String xmlFilePath) throws ParserConfigurationException, SAXException, IOException, DOMException, PatternSyntaxException
    {
        File[] xmlFileArr = new File[1];
        xmlFileArr[0] = new File(xmlFilePath);
        return new PluginMetaDataCollection(xmlFileArr);
    }

    /**
     * Gets the stored PluginMetaDataCollection instance or creates a new one if none exists with only the data read from the specified plugin metadata file.
     *
     * @param xmlFile  - the xml file to read in
     * @return a PluginMetaDataCollection instance
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws DOMException
     * @throws PatternSyntaxException
     */
    public static PluginMetaDataCollection getInstance(File xmlFile) throws ParserConfigurationException, SAXException, IOException, DOMException, PatternSyntaxException
    {
        File[] xmlFileArr = new File[1];
        xmlFileArr[0] = xmlFile;
        return new PluginMetaDataCollection(xmlFileArr);
    }

    /**
     * Gets the stored PluginMetaDataCollection instance or creates a new one if none exists with only the data read from the specified plugin metadata file.
     *
     * @param xmlFiles  - array of xml files to read in
     * @return a PluginMetaDataCollection instance
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws DOMException
     * @throws PatternSyntaxException
     */
    public static PluginMetaDataCollection getInstance(File[] xmlFiles) throws ParserConfigurationException, SAXException, IOException, DOMException, PatternSyntaxException
    {
        return new PluginMetaDataCollection(xmlFiles);
    }

    /**
     * Provides a means to create a custom PluginMetaData object mainly for testing purposes.
     *
     * @param Download
     * @param Processor
     * @param Indices
     * @param Summary
     * @param QualityControlMetaData
     * @param Title
     * @param DaysPerInputData
     * @param NoDataValue
     * @param Resolution
     * @param CompositesContinueIntoNextYear
     * @param ExtraDownloadFiles
     * @param ExtraInfo
     * @return a customized PluginMetaData object
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public static PluginMetaData CreatePluginMetaData(String Title, Integer DaysPerInputData, Integer NoDataValue, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, DownloadMetaData Download,
            ProcessorMetaData Processor, IndicesMetaData Indices, SummaryMetaData Summary, ArrayList<String> QualityControlMetaData, ExtraInfoData ExtraInfo, Boolean IsAccumulative) throws ParserConfigurationException,
    SAXException, IOException
    {
        PluginMetaDataCollection collection = new PluginMetaDataCollection();
        return collection.new PluginMetaData(Title, DaysPerInputData, NoDataValue, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, Download, Processor, Indices, Summary, QualityControlMetaData, ExtraInfo,IsAccumulative);
    }

    private PluginMetaDataCollection(File[] xmlFiles) throws ParserConfigurationException, SAXException, IOException, DOMException, PatternSyntaxException
    {
        pluginList = new ArrayList<String>();
        pluginMetaDataMap = createMap(xmlFiles);
    }

    private PluginMetaDataCollection(){
        pluginList = new ArrayList<String>();
    }

    private Map<String, PluginMetaData> createMap(File[] xmlFiles) throws ParserConfigurationException, SAXException, IOException, DOMException, PatternSyntaxException
    {
        Map<String,PluginMetaData> myMap=new HashMap<String,PluginMetaData>();
        String AccumulativeIndex = "";
        Boolean IsAccumulative = false;
        for(File fXmlFile: xmlFiles){
            // Setup Document
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            // Get top level non-process specific elements
            String Title = doc.getElementsByTagName("Title").item(0).getTextContent();
            int DaysPerInputData = Integer.parseInt(doc.getElementsByTagName("DaysPerInputData").item(0).getTextContent());
            Integer NoDataValue = Integer.parseInt(doc.getElementsByTagName("NoDataValue").item(0).getTextContent());
            int Resolution = Integer.parseInt(doc.getElementsByTagName("Resolution").item(0).getTextContent());
            Boolean CompositesContinueIntoNextYear = false;
            if(doc.getElementsByTagName("CompositesContinueIntoNextYear").getLength() > 0) {
                CompositesContinueIntoNextYear = Boolean.parseBoolean(doc.getElementsByTagName("CompositesContinueIntoNextYear").item(0).getTextContent());
            }
            Boolean ExtraIndices = false;
            if(doc.getElementsByTagName("ExtraIndices").getLength() > 0) {
                ExtraIndices = Boolean.parseBoolean(doc.getElementsByTagName("ExtraIndices").item(0).getTextContent());
            }

            ArrayList<String> QualityControlMetaData = new ArrayList<String>();
            NodeList tempQC = doc.getElementsByTagName("QualityControl");
            int nodesQC = ((Element) tempQC.item(0)).getElementsByTagName("Level").getLength();
            for(int i = 0; i < nodesQC; i++) {
                QualityControlMetaData.add( ((Element) tempQC.item(0)).getElementsByTagName("Level").item(i).getTextContent());
            }

            ArrayList<String> ExtraDownloadFiles = new ArrayList<String>();
            NodeList extraDownloadFilesNodeList = doc.getElementsByTagName("ExtraDownloadFiles");
            int extras = ((Element) extraDownloadFilesNodeList.item(0)).getElementsByTagName("Name").getLength();
            for(int i=0; i < extras; i++)
            {
                ExtraDownloadFiles.add( ((Element) extraDownloadFilesNodeList.item(0)).getElementsByTagName("Name").item(i).getTextContent());
            }
            // Get process specific metadata
            DownloadMetaData Download = new DownloadMetaData(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, doc.getElementsByTagName("Download"));
            ProcessorMetaData Processor = new ProcessorMetaData(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, doc.getElementsByTagName("Processor"));
            IndicesMetaData Indices = new IndicesMetaData(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, doc.getElementsByTagName("Indices"));
            SummaryMetaData Summary = new SummaryMetaData(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, doc.getElementsByTagName("Summary"));

            // Extra Info
            ExtraInfoData ExtraInfo = new ExtraInfoData(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, doc.getElementsByTagName("ExtraInfo"));


            if (ExtraIndices == true){
                NodeList in = doc.getElementsByTagName("Indices");
                NodeList Indexname = ((Element)in.item(0)).getElementsByTagName("ClassName");
                int len = Indexname.getLength();
                for(int i = 0; i < len; i++) {
                    if(((Element)Indexname.item(i)).hasAttribute("span")){

                        if(Indexname.item(i).getAttributes().getNamedItem("span").getNodeValue().trim().equals("true") ){
                            IsAccumulative = true;
                            AccumulativeIndex = Indexname.item(i).getTextContent();
                        }
                    }
                }
            }

            // Setup map
            //            String pluginName = FilenameUtils.removeExtension(fXmlFile.getName()).replace("Plugin_","");
            pluginList.add(Title);
            myMap.put(Title, new PluginMetaData(Title, DaysPerInputData, NoDataValue, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles, Download, Processor, Indices, Summary, QualityControlMetaData, ExtraInfo, IsAccumulative));

        }
        return myMap;
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

    public class PluginMetaData {
        public final String Title;
        public final Integer DaysPerInputData;
        public final Integer NoDataValue;
        public final Integer Resolution;
        public final Boolean CompositesContinueIntoNextYear;
        public final Boolean ExtraIndices;
        public final ArrayList<String> ExtraDownloadFiles;
        public final DownloadMetaData Download;
        public final ProcessorMetaData Processor;
        public final IndicesMetaData Indices;
        public final SummaryMetaData Summary;
        public final ArrayList<String> QualityControlMetaData;
        public final ExtraInfoData ExtraInfo;
        public final Boolean IsAccumulative;

        public PluginMetaData(String Title, Integer DaysPerInputData, Integer NoDataValue, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, DownloadMetaData Download, ProcessorMetaData Processor,
                IndicesMetaData Indices, SummaryMetaData Summary, ArrayList<String> QualityControlMetaData, ExtraInfoData ExtraInfo, Boolean IsAccumulative)
        {
            this.Download = Download;
            this.Processor = Processor;
            this.Indices = Indices;
            this.Summary = Summary;
            this.QualityControlMetaData = QualityControlMetaData;
            this.Title = Title;
            this.DaysPerInputData = DaysPerInputData;
            this.NoDataValue = NoDataValue;
            this.Resolution = Resolution;
            this.CompositesContinueIntoNextYear = CompositesContinueIntoNextYear;
            this.ExtraIndices = ExtraIndices;
            this.ExtraDownloadFiles = ExtraDownloadFiles;
            this.ExtraInfo = ExtraInfo;
            this.IsAccumulative = IsAccumulative;
        }
    }

}

