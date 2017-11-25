package version2.prototype.summary.zonal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.Layer;
import org.gdal.ogr.ogr;
import org.xml.sax.SAXException;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.ErrorLog;
import version2.prototype.Process;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;
import version2.prototype.summary.temporal.TemporalSummaryRasterFileStore;
import version2.prototype.util.DatabaseCache;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.GdalUtils;
import version2.prototype.util.GeneralUIEventObject;
import version2.prototype.util.IndicesFileMetaData;
import version2.prototype.util.Schemas;

/**
 * Calculates the zonal summary for a given raster file using the defined SummariesCollection object.
 *
 * @author michael.devos
 *
 */
public class ZonalSummaryCalculator {
    private final DatabaseConnection con;
    private final Process process;
    private final IndicesFileMetaData inputFile;
    private final String shapeFilePath;
    private final File mTableFile;
    private final String areaCodeField;
    private final String areaNameField;
    private final SummariesCollection summariesCollection;
    private final String globalSchema;
    private final String pluginName;
    private final int daysPerInputData;
    private final ProjectInfoSummary summary;
    private final Integer noDataValue;
    private final TemporalSummaryRasterFileStore fileStore;
    private final DatabaseCache outputCache;
    private final String fullPath;
    private Map<Integer, Boolean> zoneReceivedValidData;
    private static Integer invalidZoneCountTot = 0;
    private static Integer oldInvalidZoneCountTot = 0;

    /**
     * Creates a ZonalSummaryCalculator.
     * @param con
     * @param process
     * @param globalSchema
     * @param workingDir
     * @param projectName  - current project's name
     * @param pluginName
     * @param daysPerInputData
     * @param inputFile
     * @param outTableFile  - where to write output to
     * @param summariesCollection  - collection of summary calculations to run on raster data
     * @param summary
     * @param fileStore
     * @param outputCache
     */
    public ZonalSummaryCalculator(DatabaseConnection con, Process process, String globalSchema, String fullPath, String pluginName, int daysPerInputData, IndicesFileMetaData inputFile,
            File outTableFile, SummariesCollection summariesCollection, ProjectInfoSummary summary, Integer noDataValue, TemporalSummaryRasterFileStore fileStore, DatabaseCache outputCache)
    {
        this.con = con;
        this.process = process;
        this.inputFile = inputFile;
        mTableFile = outTableFile;
        shapeFilePath = summary.GetZonalSummary().GetShapeFile();
        areaCodeField = summary.GetZonalSummary().GetAreaCodeField();
        areaNameField = summary.GetZonalSummary().GetAreaNameField();
        this.summariesCollection = summariesCollection;
        this.globalSchema = globalSchema;
        this.pluginName = pluginName;
        this.daysPerInputData = daysPerInputData;
        this.summary = summary;
        this.noDataValue = noDataValue;
        this.fileStore = fileStore;
        this.outputCache = outputCache;
        this.fullPath = fullPath;
        zoneReceivedValidData = new HashMap<Integer, Boolean>();
    }

    /**
     * Run ZonalSummaryCalculator.
     * @throws Exception
     */
    public void calculate() throws Exception {
        LayerFileData layerData = null;

        GdalUtils.register();
        synchronized (GdalUtils.lockObject) {
            Dataset raster = null;
            DataSource layerSource = null;
            Layer layer = null;
            Dataset zoneRaster = null;
            try {
                // Open inputs
                raster = gdal.Open(inputFile.dataFilePath);
                GdalUtils.errorCheck();
                layerSource = ogr.Open(shapeFilePath);
                GdalUtils.errorCheck();
                layer = layerSource.GetLayer(0);

                // Get data from layer file
                layerData = new LayerFileData(inputFile, shapeFilePath, layer, areaCodeField, areaNameField, summariesCollection, raster, noDataValue);
            }
            catch (IllegalArgumentException | UnsupportedOperationException | IOException e)
            {
                ErrorLog.add(process, "Problem with calculating zonal summaries.", e);
            }
            finally
            {
                try {
                    if (raster != null) {
                        raster.delete();
                        GdalUtils.errorCheck();
                    }
                    if (layer != null) {
                        layer.delete(); GdalUtils.errorCheck();
                    }
                    if (layerSource != null) {
                        layerSource.delete(); GdalUtils.errorCheck();
                    }
                    if (zoneRaster != null) {
                        zoneRaster.delete(); GdalUtils.errorCheck();
                    }
                } catch (IllegalArgumentException | UnsupportedOperationException | IOException e) {
                    ErrorLog.add(process, "Problem with deleting Gdal related resources.", e);
                }
            }
        }

        if(layerData != null)
        {
            // Write the table
            writeTable(layerData, layerData.getCountMap());

            // Write to database
            uploadResultsToDb(mTableFile, layerData, layerData.getCountMap(), areaCodeField, areaNameField, inputFile.indexNm, summary, fileStore, summariesCollection, inputFile.year, inputFile.day, process);
        } else {
            throw new InstantiationException("Failed to get layer data.");
        }
    }

    private void writeTable(LayerFileData layerData, Map<Integer, Double> countMap) throws IllegalArgumentException, UnsupportedOperationException, IOException {
        // Write the table
        String directory = mTableFile.getCanonicalPath().substring(0, mTableFile.getCanonicalPath().lastIndexOf("\\"));
        new File(directory).mkdirs();
        PrintWriter writer = new PrintWriter(mTableFile);

        ArrayList<SummaryNameResultPair> results = summariesCollection.getResults();

        writer.print("Area Name, Area Code, ");
        for(int i=0; i < results.size(); i++)
        {
            String name = results.get(i).getSimpleName();
            if(i < results.size() - 1) {
                writer.print(name + ", ");
            } else {
                writer.println(name);
            }
        }

        String areaName;
        Map<Integer, String> areas = layerData.getAreas();
        for(Integer areaCode : areas.keySet())
        {
            areaName = areas.get(areaCode);
            if (countMap.get(areaCode) != null && countMap.get(areaCode) != 0) {
                writer.print(areaName + ", " + areaCode + ", ");
                for(int i=0; i < results.size(); i++){
                    if(i < results.size() - 1) {
                        writer.print(results.get(i).getResult().get(areaCode) + ", ");
                    } else {
                        writer.println(results.get(i).getResult().get(areaCode));
                    }
                }
            }
        }

        writer.close();
    }

    private void uploadResultsToDb(File mTableFile, LayerFileData layerData, Map<Integer, Double> countMap, String areaCodeField, String areaNameField, String indexNm, ProjectInfoSummary summary,
            TemporalSummaryRasterFileStore fileStore, SummariesCollection summariesCollection, int year, int day, Process process) throws IllegalArgumentException, UnsupportedOperationException,
    IOException, ClassNotFoundException, ParserConfigurationException, SAXException, SQLException {

        Statement stmt = con.createStatement();
        Connection con1 = RDS.getCentralDBconnection();
        Statement stmt1 = con1.createStatement();
        ArrayList<SummaryResult> newResults = new ArrayList<SummaryResult>();
        ArrayList<SummaryNameResultPair> results = summariesCollection.getResults();
        Map<Integer, String> zoneNameMap = new HashMap<Integer, String>();

        int indexID = Schemas.getIndexID(globalSchema, indexNm, stmt1);
        int projectSummaryID;
        int dateGroupID;
        Integer areaCodeTemp = null;
        Double value;
        String areaName = null;
        String filePath = mTableFile.getCanonicalPath();
        SummaryNameResultPair pair = null;
        Map<Integer, Double> result = null;
        Map<String, Double> summaryAreaResult = null;
        Map<Integer, String> areas = layerData.getAreas();
        try{
            for(Integer areaCode : areas.keySet())
            {
                areaCodeTemp = areaCode;
                areaName = areas.get(areaCode);
                summaryAreaResult = new HashMap<String, Double>();
                if(zoneNameMap.get(areaCode) == null) {
                    zoneNameMap.put(areaCode, areaName);
                }
                String projectName = null;
                if(fullPath.endsWith("\\") || fullPath.endsWith("/")){
                    projectName = fullPath.substring(0, -1).replace('\\', '/');
                }
                else{
                    projectName = fullPath.replace('\\', '/');
                }
                projectName = projectName.substring(projectName.lastIndexOf('/')+1);
                if (countMap.get(areaCode) != null && countMap.get(areaCode) != 0)
                {
                    // Insert values

                    projectSummaryID = Schemas.getProjectSummaryID(globalSchema, projectName, summary.GetID(), stmt1);
                    dateGroupID = Schemas.getDateGroupID(globalSchema, LocalDate.ofYearDay(year, day), stmt1);
                    //Schemas.getDateGroupID(globalSchema, LocalDate.ofYearDay(year, day), stmt1);
                    //Schemas.getProjectSummaryID(globalSchema, projectName, summary.GetID(), stmt1);
                    for(int i=0; i < results.size(); i++)
                    {
                        pair = results.get(i);
                        result = pair.getResult();
                        value = result.get(areaCode);
                        summaryAreaResult.put(pair.getSimpleName(), value);
                    }
                    newResults.add(new SummaryResult(projectSummaryID, areaName, areaCode, dateGroupID, indexID, filePath, summaryAreaResult));
                }
                // If there were no valid values for this area code then insert a null
                else
                {
                    // Insert null values
                    projectSummaryID = Schemas.getProjectSummaryID(globalSchema, projectName, summary.GetID(), stmt1);
                    dateGroupID = Schemas.getDateGroupID(globalSchema, LocalDate.ofYearDay(year, day), stmt1);
                    //Schemas.getProjectSummaryID(globalSchema, projectName, summary.GetID(), stmt1);
                    //Schemas.getDateGroupID(globalSchema, LocalDate.ofYearDay(year, day), stmt1);
                    for(int i=0; i < results.size(); i++)
                    {
                        pair = results.get(i);
                        summaryAreaResult.put(pair.getSimpleName(), null);
                    }
                    newResults.add(new SummaryResult(projectSummaryID, areaName, areaCode, dateGroupID, indexID, filePath, summaryAreaResult));
                }
            }
        } finally {
            stmt.close();
            stmt1.close();
            //con1.close();
        }
        con1.close();

        TemporalSummaryCompositionStrategy compStrategy = null;
        if(fileStore != null) {
            compStrategy = fileStore.compStrategy;
        }

        Set<Integer> zones = zoneReceivedValidData.keySet();
        int invalidCount = 0;
        for(Integer zone : zones)
        {
            if(!zoneReceivedValidData.get(zone)) {
                ++invalidCount;
                System.out.println("Invalid zone (" + zone + ":" + zoneNameMap.get(zone) +") for dateGroupID: " + inputFile.dateGroupID);
            }
        }
        invalidZoneCountTot += invalidCount;
        if(invalidZoneCountTot > 0 && invalidZoneCountTot != oldInvalidZoneCountTot) {
            System.out.println("invalidZoneCountTot: " + invalidZoneCountTot);
            oldInvalidZoneCountTot = invalidZoneCountTot;
        }
        if(invalidCount > 0) {
            process.NotifyUI(new GeneralUIEventObject(this, invalidCount + " zone" + ((invalidCount > 1) ? "s" : "") + " had no valid data read in for plugin='" + pluginName + "', index='" + indexNm
                    + "', Summary ID="+ summary.GetID() + ", date={year: " + year + ", day of year: " + day + "}"));
        }

        outputCache.UploadResultsToDb(con, newResults, summary.GetID(), indexNm, compStrategy, year, day, process, daysPerInputData);
    }

}
