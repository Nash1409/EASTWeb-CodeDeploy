package version2.prototype.indices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.amazonaws.samples.DeployCode.S3;
import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.Process;
import version2.prototype.ProcessWorker;
import version2.prototype.ProcessWorkerReturn;
import version2.prototype.PluginMetaData.IndicesMetaData;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.indices.NldasForcing.NldasForcingExtraIndices;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseCache;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.FileSystem;
import version2.prototype.util.ProcessorFileMetaData;
import version2.prototype.util.Schemas;


/**
 * An implementation of a ProcessWorker to handle the work for the Indices framework and to be used by a Process extending class.
 *
 * @author michael.devos
 * @author Yi Liu
 *
 */
public class IndicesWorker extends ProcessWorker{

    /**
     * An implementation of ProcessWorker that handles the indexing of a list of raster files after being handled by the Processor framework. Output used by the
     * Summary framework. Meant to be ran on its own thread.
     *
     * @param configInstance  - Config reference to use
     * @param process  - the parent Process object to this threaded worker.
     * @param projectInfoFile  - information about the project gotten from the project's info xml.
     * @param pluginInfo  - information about the plugin being used for the acquired data files.
     * @param pluginMetaData  - information relevant to this ProcessWorker about the plugin being used gotten from the plugin's info xml.
     * @param cachedFiles  - the list of files to process in this ProcessWorker.
     * @param outputCache
     */
    public IndicesWorker(Config configInstance, Process process, ProjectInfoFile projectInfoFile, ProjectInfoPlugin pluginInfo,
            PluginMetaData pluginMetaData, ArrayList<DataFileMetaData> cachedFiles, DatabaseCache outputCache)
    {
        super(configInstance, "IndicesWorker", process, projectInfoFile, pluginInfo, pluginMetaData, cachedFiles, outputCache);
    }

    @Override
    public ProcessWorkerReturn process() {
        DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        Connection con1 = RDS.getCentralDBconnection();
        if(con == null) {
            return null;
        }
        if(con1 == null) {
            return null;
        }
        String pluginName = pluginMetaData.Title;
        String outputFolder  =
                FileSystem.GetProcessOutputDirectoryPath(projectInfoFile.GetFullPath(), pluginName, ProcessName.INDICES);
        Statement stmt = null;
        Statement stmt1 = null;
        try {
            stmt = con.createStatement();
            stmt1 = con1.createStatement();
        } catch (SQLException e) {
            ErrorLog.add(process, "Problem creating Statement from db connection.", e);
        }

        if(stmt == null) {
            if(con != null) {
                con.close();
            }
            return null;
        }
        if(stmt1 == null) {
            if(con1 != null) {
                try {
                    con1.close();
                } catch (SQLException e) {e.printStackTrace();}
            }
            return null;
        }

        // Use a map to group CachedFiles based on the dates
        HashMap<DataDate, ArrayList<ProcessorFileMetaData>> map =
                new HashMap<DataDate, ArrayList<ProcessorFileMetaData>>();

        // extract cachedFiles for indices inputs
        for (DataFileMetaData dmd : cachedFiles)
        {
            // read each cachedFile
            ProcessorFileMetaData input = dmd.ReadMetaDataForIndices();

            // get the date of the downloaded file
            DataDate thisDate = new DataDate(input.day, input.year);

            // add the cachedInput file into the file ArrayList
            ArrayList<ProcessorFileMetaData> files = map.get(thisDate);
            if (files == null)
            {
                files = new ArrayList<ProcessorFileMetaData>();
            }
            // add the cachedInput file into the arraylist associated with this date
            files.add(input);
            // modify the map
            map.put(thisDate, files);
        }

        IndicesMetaData iMetaData = pluginMetaData.Indices;
        //        ArrayList<String> indicesNames  = iMetaData.indicesNames;
        ArrayList<String> indicesNames  = pluginInfo.GetIndices();
        boolean exists;
        //boolean IsAccumulative = false;
        for(String index : indicesNames)
        {
            exists = false;
            for(String knownIdx : iMetaData.indicesNames)
            {
                if(index.equals(knownIdx)) {
                    exists = true;
                }
            }
            if(!exists) {
                ErrorLog.add(process, "Problem encountered while caching data for IndicesWorker.", new Exception("Specified index, " + index + " is not part of plugin indices list."));
            }
        }

        ArrayList<DataFileMetaData> output;

        for (Map.Entry<DataDate, ArrayList<ProcessorFileMetaData>> entry : map.entrySet())
        {
            if(Thread.currentThread().isInterrupted())
            {
                try{
                    if(stmt != null) {
                        stmt.close();
                    }
                    if(stmt1 != null) {
                        stmt1.close();
                    }
                } catch(SQLException e) {
                    ErrorLog.add(process, "Problem closing connection.", e);
                }
                if(con != null) {
                    con.close();
                }
                if(con1 != null) {
                    try {
                        con1.close();
                    } catch (SQLException e) {e.printStackTrace();}
                }
                return null;
            }

            output = new ArrayList<DataFileMetaData>();
            DataDate thisDay = entry.getKey();

            File [] inputFiles =  new File [entry.getValue().size()];

            int i = 0;
            // feed the inputs
            for (ProcessorFileMetaData dFile : entry.getValue())
            {
                inputFiles[i++]= new File(dFile.dataFilePath);
            }

            // output file path
            String outputPath = String.format("%s" + "%04d" + File.separator+"%03d",
                    outputFolder, thisDay.getYear(), thisDay.getDayOfYear());

            if(!(new File(outputPath).exists()))
            {
                try {
                    FileUtils.forceMkdir(new File(outputPath));
                } catch (IOException e) {
                    ErrorLog.add(process, "Problem creating output directory.", e);
                }
            }

            for(String indices: indicesNames)
            {
                Class<?> clazzIndices;
                try
                {
                    clazzIndices = Class.forName(String.format("version2.prototype.indices.%s.%s", pluginName, indices));
                    Constructor<?> ctorIndices = clazzIndices.getConstructor(List.class, File.class, Integer.class);

                    String outFile = outputPath + File.separator + indices + ".tif";
                    File f = new File(outFile);
                    Object indexCalculator = ctorIndices.newInstance(Arrays.asList(inputFiles.clone()),f, pluginMetaData.NoDataValue);



                    Method method = indexCalculator.getClass().getMethod("calculate");
                    method.invoke(indexCalculator);

                    output.add(new DataFileMetaData(outFile, Schemas.getDateGroupID(configInstance.getGlobalSchema(), thisDay.getLocalDate(), stmt1), thisDay.getYear(), thisDay.getDayOfYear(), indices));

                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"+pluginMetaData.IsAccumulative);
                    if(pluginMetaData.IsAccumulative){
                        WriteDownloadFilesToS3(f.getName(),f.getPath());
                    }
                } catch(Exception e) {
                    ErrorLog.add(process, "Problem setting up IndexCalculator object for day " + thisDay.toString() + ". Number of input files " + inputFiles.length + ". Output file " + outputPath + File.separator + indices + ".tif.", e);
                }
            }

            try{
                outputCache.CacheFiles(stmt, output);
            } catch(SQLException | ParseException | ClassNotFoundException | ParserConfigurationException | SAXException | IOException e) {
                ErrorLog.add(process, "Problem encountered while caching data for IndicesWorker.", e);
            } catch (Exception e) {
                ErrorLog.add(process, "Problem encountered while caching data for IndicesWorker.", e);
            }
        }

        try{
            if(stmt != null) {
                stmt.close();
            }
            if(stmt1 != null) {
                stmt1.close();
            }

        } catch(SQLException e) {
            ErrorLog.add(process, "Problem closing connection.", e);
        }

        if(con != null) {
            con.close();
        }
        if(con1 != null) {
            try {
                con1.close();
            } catch (SQLException e) {e.printStackTrace();}
        }
        return null;
    }

    @Override
    public boolean verifyResults() {
        String url = "jdbc:postgresql://localhost:" + configInstance.getPort() + "/" + configInstance.getDatabaseName();
        Connection con = null;
        boolean allGood = true;
        try {
            con = DriverManager.getConnection(url, configInstance.getDatabaseUsername(), configInstance.getDatabasePassword());

            if(pluginMetaData.ExtraIndices)
            {
                if(pluginInfo.GetName() == "NldasForcing")
                {
                    NldasForcingExtraIndices.getCumulative(con, projectInfoFile.GetWorkingDir() + "\\projects", configInstance.getGlobalSchema(), Schemas.getSchemaName(projectInfoFile.GetParentProjectName(), pluginInfo.GetName()),
                            projectInfoFile.GetProjectName(), projectInfoFile.GetStartDate(), pluginMetaData.DaysPerInputData, projectInfoFile.GetFreezingDate(), projectInfoFile.GetHeatingDate());
                }
                allGood = false;
            }
            con.close();

        } catch (SQLException|NumberFormatException|ClassNotFoundException|FileNotFoundException e) {
            ErrorLog.add(process, "Problem identifying missing summaries", e);

            if(con != null) {
                try {
                    con.close();
                } catch (SQLException e1) {
                    ErrorLog.add(process, "Problem closing connection", e);
                }
            }
        }

        return allGood;
    }

    public static void WriteDownloadFilesToS3(String FileName, String Filepath) {
        S3 s3 = new S3();
        String folderName = "Projects";
        S3.init();
        //S3.createFolder(folderName);
        S3.WriteDownloadFilesToFolder(folderName, Filepath, FileName);
        System.out.println("Write indices file to S3 for acumulative index: "+FileName);
    }
}