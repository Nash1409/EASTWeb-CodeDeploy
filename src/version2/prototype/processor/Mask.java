package version2.prototype.processor;

import java.io.File;
import java.io.IOException;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.Transformer;
import org.gdal.gdal.gdal;

import version2.prototype.util.GdalUtils;

import org.apache.commons.io.FileUtils;

public class Mask {
    //locations for the input files. for this step, will only use inputFolders[0]
    protected String[] inputFolders;
    //location for the output file
    protected String outputFolder;
    protected File inputFolder;
    // the files in the input folder for composition
    protected File [] inputFiles;
    // mask file
    protected File maskFile;
    // mask file resolution
    protected Integer maskRes;
    // data file resolution;
    protected Integer projRes;
    protected final Boolean deleteInputDirectory;
    private Integer noDataValue;

    public Mask(ProcessData data, Boolean deleteInputDirectory)
    {
        inputFolders = data.getInputFolders();
        outputFolder = data.getOutputFolder();

        //check if there are more than one input file in the given folder
        inputFolder = new File(inputFolders[0]);
        File[] listOfFiles = inputFolder.listFiles();
        assert (listOfFiles.length >= 1);
        //set the input files
        inputFiles = listOfFiles;

        if (data.getMaskfile() != null) {
            maskFile = new File(data.getMaskfile()) ;
        } else {
            maskFile = null;
        }

        // the mask resolution with the projection resolution
        maskRes = data.getMaskResolution();
        projRes = data.getProjection().getPixelSize();

        this.deleteInputDirectory = deleteInputDirectory;
        noDataValue = data.getNoDataValue();
    }

    // run method for the scheduler
    public void run() throws Exception
    {
        //create outputDirectory
        File outputDir = new File(outputFolder);
        if (!outputDir.exists())
        {   FileUtils.forceMkdir(outputDir);   }

        if ((maskFile != null) && maskRes.compareTo(projRes) == 0)
        {
            // do masking only when a mask file exist
            // and the mask resolution equals to the the projection resolution
            for (File mInput : inputFiles) {
                File f = new File(outputFolder, mInput.getName());
                if(f.exists()) {
                    f.delete();
                }
            }
            maskFiles();
        }
        else  // skip masking
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

    protected void maskFiles() throws IOException
    {
        synchronized (GdalUtils.lockObject) {
            for (File mInput : inputFiles){
                Dataset mInputDS = gdal.Open(mInput.getPath());
                Dataset mMaskDS = gdal.Open(maskFile.getPath());
                String filename = mInput.getName();
                File mOutput = new File(outputFolder,filename);

                if(mInputDS != null && mMaskDS != null)
                {
                    Dataset mOutputDS = mInputDS.GetDriver().CreateCopy(mOutput.getPath(), mInputDS); // FIXME: create 32bit new raster instead?

                    assert(mInputDS.GetRasterCount() == 1);
                    assert(mMaskDS.GetRasterCount() == 1);

                    int rasterX = 0;
                    int rasterY = 0;
                    int rasterWidth = mInputDS.GetRasterXSize();
                    int rasterHeight = mInputDS.GetRasterYSize();
                    int rasterRight = rasterWidth;
                    int rasterBottom = rasterHeight;
                    Transformer transformer = new Transformer(mMaskDS, mInputDS, null);
                    double[] point = new double[] {-0.5, -0.5, 0}; // Location of corner of first zone raster pixel

                    transformer.TransformPoint(0, point);

                    int maskX = (int) Math.round(point[0]);
                    int maskY = (int) Math.round(point[1]);
                    int maskWidth = mMaskDS.GetRasterXSize();
                    int maskHeight = mMaskDS.GetRasterYSize();
                    int maskRight = maskX + maskWidth;
                    int maskBottom = maskY + maskHeight;

                    int intersectX = Math.max(rasterX, maskX);
                    int intersectY = Math.max(rasterY, maskY);
                    int intersectRight = Math.min(rasterRight, maskRight);
                    int intersectBottom = Math.min(rasterBottom, maskBottom);
                    int intersectWidth = intersectRight - intersectX;
                    int intersectHeight = intersectBottom - intersectY;

                    // FIXME: optimize line 88 - line 102
                    double[] output = new double[intersectWidth];
                    double[] mask = new double[intersectWidth];


                    // FIXME:  optimize it
                    for (int y=0; y<intersectHeight; y++) {
                        mInputDS.GetRasterBand(1).ReadRaster(intersectX, intersectY + y, intersectWidth, 1, output);
                        mMaskDS.GetRasterBand(1).ReadRaster(intersectX - maskX, intersectY - maskY + y, intersectWidth, 1, mask);

                        for (int x=0; x<intersectWidth; x++) {
                            if (mask[x] == 0) {
                                output[x] = noDataValue;
                            }
                        }

                        mOutputDS.GetRasterBand(1).WriteRaster(intersectX, intersectY + y, intersectWidth, 1, output);
                    }

                    mOutputDS.GetRasterBand(1).SetNoDataValue(noDataValue);
                    mOutputDS.GetRasterBand(1).ComputeStatistics(false);
                    mInputDS.delete();
                    mMaskDS.delete();
                    mOutputDS.delete();
                }
            }
        }
    }
}


