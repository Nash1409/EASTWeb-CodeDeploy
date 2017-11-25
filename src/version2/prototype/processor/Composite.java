package version2.prototype.processor;

import java.io.File;

import org.apache.commons.io.FileUtils;

/* Modified by Y. L.  on June 2nd
 * changed Composite from Interface to abstract class
 * added constructor and run method
 */

// compose hourly data into a daily data
public abstract class Composite {

    //locations for the input files. for this step, will only use inputFolders[0]
    protected String [] inputFolders;
    //location for the output file
    protected String outputFolder;

    protected File inputFolder;
    // the files in the input folder for composition
    protected File [] inputFiles;
    protected final Boolean deleteInputDirectory;

    public Composite(ProcessData data, Boolean deleteInputDirectory) {
        inputFolders = data.getInputFolders();
        outputFolder = data.getOutputFolder();

        //check if there are more than one input file in the given folder
        inputFolder = new File(inputFolders[0]);
        File[] listOfFiles = inputFolder.listFiles();
        assert (listOfFiles.length > 1);
        //set the input files
        inputFiles = listOfFiles;
        this.deleteInputDirectory = deleteInputDirectory;
    }

    // run method for scheduler
    public void run() throws Exception{

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

        composeFiles();

        // remove the input folder
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

    /*Override this:
     * postcondition:
     *        compose the files in the array inputFiles into a file that contains
     *        a composition of a number of input files.
     *        For example, as for a hourly NLDAS data, a result file is composed
     *        from 24 input files.
     *
     * Steps for the implementation:
     *   (1) check if there are enough number of files in the inputFiles array.(e.g. 24 for hourly NLDAS)
     *   (2) compose the files into a result file and save the result file to outputFolder
     */
    abstract protected void composeFiles();

}