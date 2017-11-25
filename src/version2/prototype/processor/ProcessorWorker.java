package version2.prototype.processor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import com.amazonaws.samples.ec2.RDS;

import version2.prototype.Config;
import version2.prototype.DataDate;
import version2.prototype.ErrorLog;
import version2.prototype.Process;
import version2.prototype.ProcessWorker;
import version2.prototype.ProcessWorkerReturn;
import version2.prototype.PluginMetaData.PluginMetaDataCollection.PluginMetaData;
import version2.prototype.PluginMetaData.ProcessorMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.Scheduler.ProcessName;
import version2.prototype.util.DataFileMetaData;
import version2.prototype.util.DatabaseCache;
import version2.prototype.util.DatabaseConnection;
import version2.prototype.util.DatabaseConnector;
import version2.prototype.util.DownloadFileMetaData;
import version2.prototype.util.FileSystem;
import version2.prototype.util.Schemas;


/**
 * An implementation of a ProcessWorker to handle the work for the Processor framework and to be used by a Process extending class.
 *
 * @author Yi Liu
 *
 */
public class ProcessorWorker extends ProcessWorker {

    /**
     * An implementation of ProcessWorker that handles the major processing of a list of raster files after being downloaded and handled by the Download framework.
     * Output is used by the Indices framework. Meant to be ran on its own thread.
     *
     * @param configInstance  - Config reference to use
     * @param process  - the parent Process object to this threaded worker.
     * @param projectInfoFile  - information about the project gotten from the project's info xml.
     * @param pluginInfo  - information about the plugin being used for the acquired data files.
     * @param pluginMetaData  - information relevant to this ProcessWorker about the plugin being used gotten from the plugin's info xml.
     * @param cachedFiles  - the list of files to process in this ProcessWorker.
     * @param outputCache
     */
    public ProcessorWorker(Config configInstance, Process process, ProjectInfoFile projectInfoFile, ProjectInfoPlugin pluginInfo,
            PluginMetaData pluginMetaData, ArrayList<DataFileMetaData> cachedFiles, DatabaseCache outputCache)
    {
        super(configInstance, "ProcessorWorker", process, projectInfoFile, pluginInfo, pluginMetaData, cachedFiles, outputCache);
    }

    @Override
    public ProcessWorkerReturn process()
    {
        DatabaseConnection con = DatabaseConnector.getConnection(configInstance);
        if(con == null) {
            return null;
        }

        String pluginName = pluginMetaData.Title;
        String outputFolder  =
                FileSystem.GetProcessOutputDirectoryPath(projectInfoFile.GetFullPath(), pluginName, ProcessName.PROCESSOR) ;

        ProcessorMetaData pMetaData = pluginMetaData.Processor;
        Map<Integer, String> processStep = pMetaData.processStep;

        // get each plugin's PrepareProcessTask
        Class<?> classPrepareTask = null;
        Constructor<?> cnstPrepareTask = null;
        try {
            classPrepareTask = Class.forName("version2.prototype.processor." + pluginName + "." + pluginName +"PrepareProcessTask" );

            cnstPrepareTask =
                    classPrepareTask.getConstructor(ProjectInfoFile.class, ProjectInfoPlugin.class,
                            PluginMetaData.class, DataDate.class);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
            ErrorLog.add(process, "Problem with reflection in PrepareTask.", e);
        } catch (Exception e) {
            ErrorLog.add(process, "Problem with reflection in PrepareTask.", e);
        }

        // Use a map to group CachedFiles based on the dates
        HashMap<DataDate, ArrayList<DownloadFileMetaData>> map =
                new HashMap<DataDate, ArrayList<DownloadFileMetaData>>();
        // extract cachedFiles for download folder
        for (DataFileMetaData dmd : cachedFiles)
        {
            // read each cachedFile
            DownloadFileMetaData downloaded = dmd.ReadMetaDataForProcessor();

            // get the date of the downloaded file
            DataDate thisDate = new DataDate(downloaded.day, downloaded.year);

            // add the downloaded file into the downloaded ArrayList
            ArrayList<DownloadFileMetaData> files = map.get(thisDate);
            if (files == null)
            {
                files = new ArrayList<DownloadFileMetaData>();
            }
            // add the downloaded file into the arraylist associated with this date
            files.add(downloaded);
            // modify the map
            map.put(thisDate, files);
        }

        // for each date, process the steps associated with the plugin
        for (Map.Entry<DataDate, ArrayList<DownloadFileMetaData>> entry : map.entrySet())
        {
            if(Thread.currentThread().isInterrupted()) {
                return null;
            }

            DataDate thisDay = entry.getKey();

            //create necessary folders for the date
            PrepareProcessTask prepareTask = null;
            try {
                prepareTask = (PrepareProcessTask) cnstPrepareTask.newInstance(projectInfoFile, pluginInfo, pluginMetaData, thisDay);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                ErrorLog.add(process, "Problem with instantiation of PrepareProcessTask.", e);
            } catch (Exception e) {
                ErrorLog.add(process, "Problem with instantiation of PrepareProcessTask.", e);
            }

            String laststepOutputFolder = null;

            // process the files for that date
            String outputPath = String.format("%s"+ "%04d" + File.separator+"%03d",
                    outputFolder, thisDay.getYear(), thisDay.getDayOfYear());
            for (Entry<Integer, String> step : processStep.entrySet())
            {
                Integer key = step.getKey();

                Class<?> classProcess = null;
                try {
                    classProcess = Class.forName("version2.prototype.processor." + pluginName + "." + step.getValue());
                    System.out.println(step.getValue());
                } catch (ClassNotFoundException e) {
                    ErrorLog.add(process, "Problem with reflection of classProcess '" + "version2.prototype.processor." + pluginName + "." + step.getValue() + "'.", e);
                } catch (Exception e) {
                    ErrorLog.add(process, "Problem with reflection of classProcess '" + "version2.prototype.processor." + pluginName + "." + step.getValue() + "'.", e);
                }

                Constructor<?> cnstProcess = null;
                try {
                    cnstProcess = classProcess.getConstructor(ProcessData.class, Boolean.class);
                } catch (NoSuchMethodException | SecurityException e) {
                    ErrorLog.add(process, "Problem with reflection of classProcess.", e);
                } catch (Exception e) {
                    ErrorLog.add(process, "Problem with reflection of classProcess.", e);
                }

                //copy the downloaded files to the input folders
                if (key == 1)
                {
                    String [] inputFolders = prepareTask.getInputFolders(key);

                    File dataInputFolder = new File(inputFolders[0]);
                    File qcInputFolder = null;
                    if (inputFolders.length > 1)    // it has a qc folder
                    {
                        qcInputFolder =  new File(inputFolders[1]);
                    }

                    for (DownloadFileMetaData dFile : entry.getValue())
                    {
                        System.out.println("processorWorker: " + dFile.dataFilePath);
                        if (dFile.dataName.equalsIgnoreCase("data"))
                        {
                            try {
                                FileUtils.copyFileToDirectory(new File(dFile.dataFilePath), dataInputFolder);
                            } catch (IOException e) {
                                ErrorLog.add(process, "Problem with copying of downloaded data.", e);
                            } catch (Exception e) {
                                ErrorLog.add(process, "Problem with copying of downloaded data.", e);
                            }
                        }

                        if (dFile.dataName.equalsIgnoreCase("QC"))
                        {
                            try {
                                FileUtils.copyFileToDirectory(new File(dFile.dataFilePath), qcInputFolder);
                            } catch (IOException e) {
                                ErrorLog.add(process, "Problem with copying of downloaded QC.", e);
                            } catch (Exception e) {
                                ErrorLog.add(process, "Problem with copying of downloaded QC.", e);
                            }
                        }

                    }
                }

                laststepOutputFolder = prepareTask.getOutputFolder(key);

                Object process = null;
                try {
                    ProcessData pData = new ProcessData(
                            prepareTask.getInputFolders(key),
                            prepareTask.getOutputFolder(key),
                            prepareTask.getDataDate(),
                            prepareTask.getQC(),
                            prepareTask.getShapeFile(),
                            prepareTask.getMaskFile(),
                            prepareTask.getDataBands(),
                            prepareTask.getQCBands(),
                            prepareTask.getProjection(),
                            prepareTask.getMaskResolution(),
                            prepareTask.getDataResolution(),
                            prepareTask.getClipOrNot(),
                            prepareTask.getFreezingDate(),
                            prepareTask.getHeatingDate(),
                            prepareTask.getFreezingDegree(),
                            prepareTask.getHeatingDegree(),
                            pluginMetaData.NoDataValue
                            );
                    process = cnstProcess.newInstance(pData, this.process.GetClearIntermediateFilesFlag());
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    ErrorLog.add(this.process, "Problem with cnstProcess instantion.", (e.getCause() != null ? e.getCause() : e));
                } catch (Exception e) {
                    ErrorLog.add(this.process, "Problem with cnstProcess instantion.", (e.getCause() != null ? e.getCause() : e));
                }

                Method methodProcess;
                try {
                    methodProcess = process.getClass().getMethod("run");
                    methodProcess.invoke(process);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    ErrorLog.add(this.process, "Problem with calling run on Processor step '" + step.getValue() + "' for day " + thisDay + ".", e);
                } catch (Exception e) {
                    ErrorLog.add(this.process, "Problem with calling run on Processor step '" + step.getValue() + "' for day " + thisDay + ".", e);
                }
            }

            // check if the laststepOutputFolder is the  final outputFolder for the processor
            // if not match, copy the files from the last step to the final outputfolder for  processor
            if (!outputPath.equals(laststepOutputFolder))
            {
                File outputDir = new File(outputPath);
                if(!(outputDir.exists()))
                {
                    outputDir.mkdirs();
                }

                // copy the output files to the system output directory
                if (laststepOutputFolder != null)
                {
                    for (File f: new File(laststepOutputFolder).listFiles()) {
                        try {
                            FileUtils.copyFileToDirectory(f, outputDir);
                        } catch (IOException e) {
                            ErrorLog.add(process, "Copying data to different directory.", e);
                        } catch (Exception e) {
                            ErrorLog.add(process, "Copying data to different directory.", e);
                        }
                    }
                }
            }

            // remove the entire temp folder
            // find "temp" in the laststepOutputFolder
            /*  WRITE BACK:  add back when fix the MODISLST band5 issue
            if ( tempFolderStr!= null)
            {
                String tempFolder = tempFolderStr.substring(0, tempFolderStr.lastIndexOf("Temp"))+"Temp";
                FileUtils.deleteDirectory(new File(tempFolder));
            }
             */

            // compile the output files in the outputPath to an arraylist of DataFileMetaData and save to the database
            ArrayList<DataFileMetaData> toCache = new ArrayList<DataFileMetaData>();

            // get the list of files in the output folder
            File dir = new File(outputPath);
            File[] dirList = dir.listFiles();

            // add each file into DataFileMetaData

            Statement stmt = null;
            Connection con1 = RDS.getCentralDBconnection();
            Statement stmt1 = null;
            try {
                stmt = con.createStatement();
                stmt1 = con1.createStatement();
                for (File oFile : dirList) {
                    if(oFile.length() > 0) {
                        //Schemas.getDateGroupID(configInstance.getGlobalSchema(), thisDay.getLocalDate(), stmt1);
                        toCache.add(new DataFileMetaData("data", oFile.getAbsolutePath(), Schemas.getDateGroupID(configInstance.getGlobalSchema(), thisDay.getLocalDate(), stmt1), thisDay.getYear(),
                                thisDay.getDayOfYear()));
                    }
                }
            } catch (SQLException e) {
                ErrorLog.add(process, "Problem while creating list for output cache.", e);
            } catch (Exception e) {
                ErrorLog.add(process, "Problem while creating list for output cache.", e);
            }

            // cache to the database
            try{
                outputCache.CacheFiles(stmt, toCache);
                stmt.close();
                con.close();
                stmt1.close();
                con1.close();
            } catch(SQLException | ParseException | ClassNotFoundException | ParserConfigurationException | SAXException | IOException e) {
                ErrorLog.add(process, "Problem encountered while caching data for ProcessorWorker.", e);
            } catch (Exception e) {
                ErrorLog.add(process, "Problem encountered while caching data for ProcessorWorker.", e);
            }
        }

        return null;
    }

    @Override
    public boolean verifyResults() {
        // TODO Auto-generated method stub
        return true;
    }

}
