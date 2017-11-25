package version2.prototype;

import java.io.*;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jdk.nashorn.internal.ir.annotations.Immutable;
import version2.prototype.EastWebUI.DocumentBuilderInstance;
import version2.prototype.util.FileSystem;

//import version2.prototype.util.LazyCachedReference;

/**
 *
 * @author michael.devos
 *
 */
@Immutable public class Config {
    private static final String CONFIG_FILENAME = ".//config//config.xml";
    private static final String ERROR_LOG_DIR_KEY = "ErrorLogDir";
    // Download section
    private static final String DOWNLOAD_DIR_KEY = "DownloadDir";
    // Database section
    private static final String DATA_BASE_KEY = "Database";
    private static final String GLOBAL_SCHEMA_KEY = "GlobalSchema";
    private static final String HOST_NAME_KEY = "HostName";
    private static final String PORT_KEY = "Port";
    private static final String DATABASE_NAME_KEY = "DatabaseName";
    private static final String DATABASE_USERNAME_KEY = "UserName";
    private static final String DATABASE_PASSWORD_KEY = "PassWord";
    private static final String MAX_NUM_OF_CONNECTIONS_PER_INSTANCE_KEY = "MaxNumOfConnectionsPerInstance";
    // Output section
    private static final String OUTPUT_KEY = "Output";
    private static final String TEMPORAL_SUMMARY_COMPOSITION_STRATEGY_KEY = "TemporalSummaryCompositionStrategy";
    private static final String SUMMARY_CALCULATION_KEY = "SummaryCalculation";
    // Instance
    private static Config instance = null;
    private static Boolean instanceLock = new Boolean(true);        // Value doesn't matter
    //    private static final LazyCachedReference<Config, ConfigReadException> instance =
    //            new LazyCachedReference<Config, ConfigReadException>() {
    //        @Override
    //        protected Config makeInstance() throws ConfigReadException {
    //            try {
    //                return new Config();
    //            } catch (ConfigReadException e) {
    //                throw e;
    //            } catch (Exception e) {
    //                throw new ConfigReadException(e);
    //            }
    //        }
    //    };

    private final String errorLogDir;
    private final String downloadDir;
    private final String globalSchema;
    private final String databaseHost;
    private final Integer port;
    private final String databaseName;
    private final String databaseUsername;
    private final String databasePassword;
    private final Integer maxNumOfConnectionsPerInstance;
    private final ArrayList<String> summaryTempCompStrategies;
    private final ArrayList<String> summaryCalculations;

    private Config(String xmlPath)
    {
        DocumentBuilder dBuilder = null;
        Document doc = null;
        Element database = null;
        Node outputNode = null;
        NodeList summaryList = null;
        NodeList tempCompStrategyList = null;

        String errorLogDirTemp = null;
        String downloadDirTemp = null;
        String globalSchemaTemp = null;
        String databaseHostTemp = null;
        Integer portTemp = null;
        String databaseNameTemp = null;
        String databaseUsernameTemp = null;
        String databasePasswordTemp = null;
        Integer maxNumOfConnectionsPerInstanceTemp = null;
        ArrayList<String> tempSummaryCompStrategiesTemp = null;
        ArrayList<String> summaryCalculationsTemp = null;

        File fXmlFile = new File(xmlPath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            errorLogDirTemp = ((Element) doc.getElementsByTagName(ERROR_LOG_DIR_KEY).item(0)).getTextContent();

            // Node: Download
            downloadDirTemp = ((Element) doc.getElementsByTagName(DOWNLOAD_DIR_KEY).item(0)).getTextContent();

            // Node: Database
            database = (Element) doc.getElementsByTagName(DATA_BASE_KEY).item(0);
            globalSchemaTemp = database.getElementsByTagName(GLOBAL_SCHEMA_KEY).item(0).getTextContent();
            databaseHostTemp = "jdbc:postgresql://"+database.getElementsByTagName(HOST_NAME_KEY).item(0).getTextContent();
            portTemp = Integer.parseInt(database.getElementsByTagName(PORT_KEY).item(0).getTextContent());
            databaseNameTemp = database.getElementsByTagName(DATABASE_NAME_KEY).item(0).getTextContent();
            databaseUsernameTemp = database.getElementsByTagName(DATABASE_USERNAME_KEY).item(0).getTextContent();
            databasePasswordTemp = database.getElementsByTagName(DATABASE_PASSWORD_KEY).item(0).getTextContent();
            maxNumOfConnectionsPerInstanceTemp = Integer.parseInt(database.getElementsByTagName(MAX_NUM_OF_CONNECTIONS_PER_INSTANCE_KEY).item(0).getTextContent());

            // Node: Output
            outputNode = doc.getElementsByTagName(OUTPUT_KEY).item(0);

            // Node(s): TemporalSummaryCompositionStrategy
            tempSummaryCompStrategiesTemp = new ArrayList<String>(1);
            tempCompStrategyList = ((Element) outputNode).getElementsByTagName(TEMPORAL_SUMMARY_COMPOSITION_STRATEGY_KEY);
            for(int i=0; i < tempCompStrategyList.getLength(); i++) {
                tempSummaryCompStrategiesTemp.add(tempCompStrategyList.item(i).getTextContent());
            }

            // Node(s): SummaryCalculation
            summaryCalculationsTemp = new ArrayList<String>(1);
            summaryList = ((Element) outputNode).getElementsByTagName(SUMMARY_CALCULATION_KEY);
            //summaryCalculationsTemp.add("Count");
            for(int i=0; i < summaryList.getLength(); i++) {
                summaryCalculationsTemp.add(summaryList.item(i).getTextContent());
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            ErrorLog.add("Problem getting Config instance", e);
        } catch (Exception e) {
            ErrorLog.add("Problem getting Config instance", e);
        }

        errorLogDir = errorLogDirTemp;
        downloadDir = downloadDirTemp;
        globalSchema = globalSchemaTemp;
        databaseHost = databaseHostTemp;
        port = portTemp;
        databaseName = databaseNameTemp;
        databaseUsername = databaseUsernameTemp;
        databasePassword = databasePasswordTemp;
        summaryTempCompStrategies = tempSummaryCompStrategiesTemp;
        summaryCalculations = summaryCalculationsTemp;
        maxNumOfConnectionsPerInstance = maxNumOfConnectionsPerInstanceTemp;
    }

    @SuppressWarnings("unchecked")
    private Config(String errorLogDir, String downloadDir, String globalSchema, String databaseHost, Integer port, String databaseName, String databaseUsername, String databasePassword,
            Integer maxNumOfConnectionsPerInstance, ArrayList<String> summaryTempCompStrategies, ArrayList<String> summaryCalculations)
    {
        this.errorLogDir = errorLogDir;
        this.downloadDir = downloadDir;
        this.globalSchema = globalSchema;
        this.databaseHost = "jdbc:postgresql://"+databaseHost;
        this.port = port;
        this.databaseName = databaseName;
        this.databaseUsername = databaseUsername;
        this.databasePassword = databasePassword;
        this.maxNumOfConnectionsPerInstance = maxNumOfConnectionsPerInstance;
        this.summaryTempCompStrategies = (ArrayList<String>) summaryTempCompStrategies.clone();
        this.summaryCalculations = (ArrayList<String>) summaryCalculations.clone();
    }

    /**
     * Gets the singleton instance of Config reading and parsing the Config.xml file found in EASTWeb's working directory.
     *
     * @return the Config instance
     */
    public static Config getInstance()
    {
        if(instance == null) {
            synchronized(instanceLock) {
                instance = new Config(CONFIG_FILENAME);
            }
        }
        return instance;
    }

    /**
     * Returns a Config object created from parsing the xml file found at the given path. Works the same as calling getInstance() but allows specifying of which file to use. Doesn't store or reuse
     * the created instance. Meant for testing purposes.
     *
     * @param xmlPath  - file path to the xml to read and parse
     * @return a new Config object
     */
    public static Config getAnInstance(String xmlPath)
    {
        return new Config(xmlPath);
    }

    /**
     * Creates a custom Config object with the given parameters. Doesn't store or reuse the created instance. Meant for testing purposes.
     * @param errorLogDir
     * @param downloadDir
     * @param globalSchema
     * @param databaseHost
     * @param port
     * @param databaseName
     * @param databaseUsername
     * @param databasePassword
     * @param maxNumOfConnectionsPerInstance
     * @param summaryTempCompStrategies
     * @param summaryCalculations
     * @return a new Config object
     */
    public static Config getAnInstance(String errorLogDir, String downloadDir, String globalSchema, String databaseHost, Integer port, String databaseName, String databaseUsername,
            String databasePassword, Integer maxNumOfConnectionsPerInstance, ArrayList<String> summaryTempCompStrategies, ArrayList<String> summaryCalculations) {
        return new Config(errorLogDir, downloadDir, globalSchema, databaseHost, port, databaseName, databaseUsername, databasePassword, maxNumOfConnectionsPerInstance,
                summaryTempCompStrategies, summaryCalculations);
    }

    public String getErrorLogDir() {
        return FileSystem.CheckDirPath(errorLogDir);
    }

    public String getDownloadDir() {
        return FileSystem.CheckDirPath(downloadDir);
    }

    public String getGlobalSchema() {
        return FileSystem.StandardizeName(globalSchema);
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public Integer getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public Integer getMaxNumOfConnectionsPerInstance() {
        return maxNumOfConnectionsPerInstance;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getSummaryTempCompStrategies() {
        return (ArrayList<String>) summaryTempCompStrategies.clone();
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getSummaryCalculations() {
        return (ArrayList<String>) summaryCalculations.clone();
    }

    public boolean WriteConfigFile(Document doc)
    {
        File theDir = new File(System.getProperty("user.dir") + "\\config\\" + "config.xml" );
        //File theDir = new File("C:\\Users\\sufi\\OneDrive\\config.xml" );

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

    public boolean Writec3p0File(String xmlSource)
    {
        try {
            // Parse the given input
            DocumentBuilderFactory factory = DocumentBuilderInstance.Instance().GetDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlSource)));

            // Write the parsed document to an xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result =  new StreamResult(new File(System.getProperty("user.dir") + "\\config\\" + "c3p0-config.xml" ));
            //StreamResult result =  new StreamResult(new File("C:\\Users\\sufi\\OneDrive\\c3p0-config.xml" ));
            transformer.transform(source, result);

            return true;
        } catch (IOException | ParserConfigurationException | SAXException | TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }

    }
}
