package version2.prototype.util;

import java.io.IOException;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.ogr.DataSource;
import org.gdal.ogr.ogr;

import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.image.image.ImageDatasetFactory;
import version2.prototype.Config;
import version2.prototype.ConfigReadException;
import version2.prototype.ErrorLog;
import version2.prototype.Projection;
import version2.prototype.Projection.ResamplingType;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GdalUtils {
    private GdalUtils() {
    }

    //    public static final int NO_VALUE = -99999;
    //    public static final float NO_DATA = Float.intBitsToFloat(0xff7fffff);       // float(-3.4028234663852886E38)
    /**
     * All GDAL operations should be done while holding a lock on this object.
     * GDAL is "not completely thread-safe", so this may be critical.
     */
    public static final Object lockObject = new Object();

    private static boolean sRegistered = false;

    public static void register() {
        if (!sRegistered) {
            synchronized (lockObject) {
                if (!sRegistered) {
                    ogr.RegisterAll();
                    gdal.AllRegister();
                    ogr.UseExceptions();
                    sRegistered = true;
                }
            }
        }
    }

    /**
     * Checks for exceptions reported to the GDAL error reporting system and
     * maps them to Java exceptions or errors.
     *
     * @throws IOException
     *             CPLE_AppDefined, CPLE_FileIO, CPLE_OpenFailed,
     *             CPLE_NoWriteAccess, CPLE_UserInterrupt
     * @throws IllegalArgumentException
     *             CPLE_IllegalArg
     * @throws UnsupportedOperationException
     *             CPLE_NotSupported
     */
    public static void errorCheck() throws IOException,
    IllegalArgumentException, UnsupportedOperationException {
        synchronized (lockObject) {
            int type = gdal.GetLastErrorType();
            if (type != gdalconstConstants.CE_None) {
                int number = gdal.GetLastErrorNo();
                String message = gdal.GetLastErrorMsg();
                gdal.ErrorReset();

                if (number == gdalconstConstants.CPLE_AppDefined
                        || number == gdalconstConstants.CPLE_FileIO
                        || number == gdalconstConstants.CPLE_OpenFailed
                        || number == gdalconstConstants.CPLE_NoWriteAccess
                        || number == gdalconstConstants.CPLE_UserInterrupt) {
                    throw new IOException(message);
                } else if (number == gdalconstConstants.CPLE_OutOfMemory) {
                    throw new OutOfMemoryError(message);
                } else if (number == gdalconstConstants.CPLE_IllegalArg) {
                    throw new IllegalArgumentException(message);
                } else if (number == gdalconstConstants.CPLE_NotSupported) {
                    throw new UnsupportedOperationException(message);
                } else if (number == gdalconstConstants.CPLE_AssertionFailed) {
                    throw new AssertionError(message);
                }
            }
        }
    }

    /**
     * Do the projection for input file, and write the processed data into
     * output file
     *
     * @param wkt
     *            wkt string contains the projection information for the output
     *            file
     * @param input
     *            input file for reprojection
     * @param masterShapeFile
     *            input mater shapefile
     * @param project
     *            store the shape file and project information
     * @param output
     *            output file.
     * @param resampleAlg
     *            the type of resampling to use, among gdalconst.GRA_
     * @throws ConfigReadException
     *             *
     **/
    public static void project(File input, String masterShapeFile, Projection projection, File output, Integer noDataValue) {
        assert (masterShapeFile != null);
        GdalUtils.register();

        synchronized (GdalUtils.lockObject) {
            // Load input file and features
            Dataset inputDS = gdal.Open(input.getPath());
            if(inputDS != null)
            {
                // System.out.println(inputDS.GetProjectionRef().toString());
                // SpatialReference inputRef = new SpatialReference();

                /* Original code : takes an array of shape files
                List<DataSource> features = new ArrayList<DataSource>();
                for (String filename : project.getShapeFiles()) {
                    features.add(ogr.Open(new File(DirectoryLayout
                            .getSettingsDirectory(project), filename).getPath()));
                }
                 */

                List<DataSource> features = new ArrayList<DataSource>();
                features.add(ogr.Open(new File(masterShapeFile).getPath()));


                // Find union of extents
                double[] extent = features.get(0).GetLayer(0).GetExtent(); // Ordered:
                // left,
                // right,
                // bottom,
                // top
                // System.out.println(Arrays.toString(extent));
                double left = extent[0];
                double right = extent[1];
                double bottom = extent[2];
                double top = extent[3];
                for (int i = 1; i < features.size(); i++) {
                    extent = features.get(i).GetLayer(0).GetExtent();
                    if (extent[0] < left) {
                        left = extent[0];
                    } else if (extent[1] > right) {
                        right = extent[1];
                    } else if (extent[2] < bottom) {
                        bottom = extent[2];
                    } else if (extent[3] > top) {
                        top = extent[3];
                    }
                }

                // Project to union of extents
                Dataset outputDS =
                        gdal.GetDriverByName("GTiff").Create(
                                output.getPath(),
                                (int) Math.ceil((right - left)
                                        / (projection.getPixelSize())),
                                (int) Math.ceil((top - bottom)
                                        / (projection.getPixelSize())),
                                1, gdalconstConstants.GDT_Float32);

                // TODO: get projection from project info, and get transform from
                // shape file
                // SpatialReference outputRef = new SpatialReference();
                // outputRef.ImportFromWkt(wkt);
                outputDS.GetRasterBand(1).SetNoDataValue(noDataValue);
                String outputProjection =
                        features.get(0).GetLayer(0).GetSpatialRef().ExportToWkt();
                outputDS.SetProjection(outputProjection);
                outputDS.SetGeoTransform(new double[] { left,
                        (projection.getPixelSize()), 0, top, 0,
                        - (double)(projection.getPixelSize()) });

                // get resample argument
                int resampleAlg = -1;
                ResamplingType resample = projection.getResamplingType();
                switch (resample) {
                case NEAREST_NEIGHBOR:
                    resampleAlg = gdalconstConstants.GRA_NearestNeighbour;
                    break;              // added by YL
                case BILINEAR:
                    resampleAlg = gdalconstConstants.GRA_Bilinear;
                    break;              // added by YL
                case CUBIC_CONVOLUTION:
                    resampleAlg = gdalconstConstants.GRA_CubicSpline;
                }
                gdal.ReprojectImage(inputDS, outputDS, null, null, resampleAlg);
                outputDS.GetRasterBand(1).ComputeStatistics(false);
                outputDS.delete();
                inputDS.delete();
            }
        }
    }

    /* @param: inFile - the input NetCDF file
     * @param: outFile - the output Tiff file
     * @param: getTrans - geotransformation
     * @param: projectionStr - the coordinate system
     */
    public static void NetCDF2Tiff(String inFile, String outFile,
            double[] geoTrans, String projectionStr, double nodataV) throws Exception
    {

        // open the netcdf file into a buffered image
        GridDataset gd = GridDataset.open(inFile);
        ImageDatasetFactory imageFactory = new ImageDatasetFactory();
        imageFactory.openDataset(gd.getGrids().get(0));
        BufferedImage image = imageFactory.getNextImage(true);

        GdalUtils.register();
        synchronized (GdalUtils.lockObject) {

            Raster raster = image.getData();
            int xSize=raster.getWidth();
            int ySize=raster.getHeight();

            //write to a tiff file
            Dataset outputDS = gdal.GetDriverByName("GTiff").Create(
                    outFile,
                    xSize, ySize,
                    1,
                    gdalconstConstants.GDT_Float32
                    );

            double[] array = new double[xSize];
            for (int row=0; row<ySize; row++)
            {
                for (int col=0; col<xSize; col++)
                {
                    array[col] = raster.getSampleDouble(col, row, 0);
                }
                outputDS.GetRasterBand(1).WriteRaster(0, row, xSize, 1, array);
            }

            outputDS.GetRasterBand(1).SetNoDataValue(nodataV);
            //set geotransformation
            outputDS.SetGeoTransform(geoTrans);
            // set coordinate system
            outputDS.SetProjection(projectionStr);
            outputDS.GetRasterBand(1).ComputeStatistics(false);
            outputDS.delete();

            image.flush();
            gd.close();
        }

    }
}
