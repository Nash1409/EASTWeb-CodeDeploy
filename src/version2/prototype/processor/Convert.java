package version2.prototype.processor;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/* @author:  Yi Liu
 *
 * changed Covert from Interface to abstract class
 * added constructor and run method
 */
/*
 * Convert needs to be extended only when the downloaded data format cannot be read by GDAL directly
 */

public abstract class Convert {
    //locations for the input files. for this step, will only use inputFolders[0]
    protected String [] inputFolders;
    protected File inputFolder;
    //location for the output file
    protected String outputFolder;
    // the file in the input folder
    protected File [] inputFiles;
    protected final Boolean deleteInputDirectory;

    public Convert(ProcessData data, Boolean deleteInputDirectory) {
        inputFolders = data.getInputFolders();
        outputFolder = data.getOutputFolder();

        //check if there is more than one input file in the given folder
        inputFolder = new File(inputFolders[0]);
        File[] listOfFiles = inputFolder.listFiles();
        assert (listOfFiles.length >= 1);
        //set the input files
        inputFiles = listOfFiles;
        this.deleteInputDirectory = deleteInputDirectory;
    }

    // run method for the scheduler
    public void run() throws Exception, IOException{
        //create outputDirectory
        File outputDir = new File(outputFolder);
        if (!outputDir.exists())
        {   FileUtils.forceMkdir(outputDir); }

        for (File mInput : inputFiles) {
            File f = new File(outputFolder, mInput.getName());
            if(f.exists()) {
                f.delete();
            }
        }

        convertFiles();

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

    /*Override this:
     * postcondition:
     *    Convert one downloaded file in raw format into a result file with the format that GDAL can process (TIFF)
     *    the file is written into folder specified in variable outputFolder
     *
     * Steps for the implementation:
     *   (1) Read each file (inputFile)
     *   (2) Convert it into the Tiff format
     *   (3) Write the result from (2) to outputFolder
     *   (4) repeat step (1) - (3)
     */
    abstract protected void convertFiles() throws Exception;

}
