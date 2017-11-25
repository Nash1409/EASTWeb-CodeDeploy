package version2.prototype.processor;

import java.io.File;
import org.apache.commons.io.FileUtils;

import version2.prototype.Projection;
import version2.prototype.util.GdalUtils;

/* Author: Y. L.
 *
 * Reproject the file from original projection to the specified projection
 */

public abstract class Reproject {
    //locations for the input files. for this step, will only have one folder
    private String [] inputFolders;
    //location for the output file
    private String outputFolder;
    private File inputFolder;
    // the files in the input folder
    private File [] inputFiles;
    private String shapefile;
    private Projection projection;
    private Integer noDataValue;
    protected String wktStr;
    protected boolean NoProj;  // no reprojection =  true
    protected final Boolean deleteInputDirectory;

    public Reproject(ProcessData data, Boolean deleteInputDirectory) {
        inputFolders = data.getInputFolders();
        outputFolder = data.getOutputFolder();
        shapefile = data.getShapefile();
        projection = data.getProjection();
        noDataValue = data.getNoDataValue();

        //check if there is at least one input file in the given folder
        inputFolder = new File(inputFolders[0]);
        File[] listOfFiles = inputFolder.listFiles();
        assert (listOfFiles.length >1);
        //set the input files
        inputFiles = listOfFiles;
        wktStr = null;

        // FIXME:  allow the users to choose whether to do reprojection
        NoProj =  true;

        this.deleteInputDirectory = deleteInputDirectory;
    }

    // run method for the scheduler
    public void run() throws Exception{

        //create outputDirectory
        File outputDir = new File(outputFolder);
        if (!outputDir.exists())
        {   FileUtils.forceMkdir(outputDir); }


        if (NoProj)    // no projection
        {
            // copy files in the input folder to the output folder
            for (File f: inputFiles) {
                File oF = new File(outputFolder, f.getName());
                if(oF.exists()) {
                    oF.delete();
                }
                FileUtils.copyFileToDirectory(f, outputDir);
            }
        }
        else {
            for (File mInput : inputFiles) {
                File f = new File(outputFolder, mInput.getName());
                if(f.exists()) {
                    f.delete();
                }
            }
            reprojectFiles();    // reprojection
        }

        // remove the input folder
        if(deleteInputDirectory)
        {
            File deleteDir = inputFolder;
            if(deleteDir != null && deleteDir.exists())
            {
                if(deleteDir.isFile()) {
                    deleteDir = deleteDir.getParentFile();
                }
                if(deleteDir != null && deleteDir.exists()) {
                    FileUtils.deleteDirectory(deleteDir);
                }
            }
        }
    }

    // reproject all the input Files and save them to the outputFolder
    private void reprojectFiles()  {
        for (File f : inputFiles) {
            String fileName = f.getName();
            File outputFile = new File (outputFolder, fileName);
            // reproject
            // GdalUtils.project(f, shapefile, projection, outputFile);
            GdalUtils.project(f, shapefile, projection, outputFile, noDataValue);
        }

    }

}

