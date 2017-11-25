package version2.prototype.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;

import version2.prototype.util.GdalUtils;


/* rewrote by YL on March 11, 2017
 */

// Mosaic tiles together using gdalwarp
public abstract class Mozaic {

    //locations for the input files. for this step, will only use inputFolders[0]
    String [] inputFolders ;

    //location for the output file
    protected String outputFolder;
    // the bands need to be exacted.
    protected int [] bands;
    // the bandname patter
    protected String bandpattern;
    protected File inputFolder;
    // the files in the input folder
    protected File [] inputFiles;
    // hold the output files
    protected ArrayList<File> outputFiles;

    protected final Boolean deleteInputDirectory;

    public Mozaic(ProcessData data, Boolean deleteInputDirectory) throws InterruptedException {

        //locations for the input files. for this step, will only use inputFolders[0]
        inputFolders = data.getInputFolders();

        //check if there is at least one input file in the given folder
        inputFolder = new File(inputFolders[0]);
        File[] listOfFiles = inputFolder.listFiles();

        //set the input files
        inputFiles = listOfFiles;

        outputFolder = data.getOutputFolder();
        new File(outputFolder).mkdirs();
        System.out.println(outputFolder);
        bands = getBands();

        bandpattern = getBandNamePattern();

        outputFiles = new ArrayList<File>();

        this.deleteInputDirectory = deleteInputDirectory;
    }

    // run method for the scheduler
    public void run() throws Exception{

        //create outputDirectory
        if (!(new File(outputFolder)).exists())
        {   FileUtils.forceMkdir(new File(outputFolder)); }

        for (File mInput : inputFiles) {
            File f = new File(outputFolder, mInput.getName());
            if(f.exists()) {
                f.delete();
            }
        }

        String [] bandFiles = bandNames(inputFiles);

        mosaicTiles(bandFiles, bands);

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

    //the bands for each file
    abstract protected int [] getBands();

    // the band name pattern
    // something like: /MOD_Grid_BRDF:Nadir_Reflectance_Band(1|2|3|4|5|6|7)/;
    abstract protected String getBandNamePattern();

    /* filename: files in the folder
     * sdsName: The name of the band, such as MOD_Grid_BRDF:Nadir_Reflectance_Band
     * band:  the input band
     * return:  an array with each element for the files to be mosaiced
     */
    private String[] bandNames(File[] filenames)
    {
        Process p;

        String [] bandNames = new String[bands.length] ;

        try {
            for (int i = filenames.length - 1; i >= 0; i--)
            {
                // call the gdalinfo to list the HDF info including the sdsNames
                String command = "./lib/gdal/gdalinfo " + filenames[i].getAbsolutePath();
                p = Runtime.getRuntime().exec(command);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = "";
                int j = 0;

                while ((line = reader.readLine())!= null) {
                    Pattern pattern = Pattern.compile(bandpattern);
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find())
                    {
                        if (bandNames[j] == null) {
                            bandNames[j] = " ";
                        }
                        // add bands to the string
                        bandNames[j++] += line.substring(line.indexOf('=') + 1) + " ";
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return bandNames;
    }

    private void mosaicTiles(String [] bandNames, int bands[])
    {
        Process p;;
        String outputFile;
        try {
            for (int i = bandNames.length - 1; i >= 0; i--)
            {
                outputFile = outputFolder + File.separator + "band" + String.valueOf(bands[i]) + ".tif";
                // call the gdalinfo to list the HDF info including the sdsNames
                String command = "./lib/gdal/gdalwarp --config GDAL_CACHEMAX 1000 -wm 1000 "
                        + bandNames[i] + " " +outputFile;
                p = Runtime.getRuntime().exec(command);
                p.waitFor();
            }

        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }



}