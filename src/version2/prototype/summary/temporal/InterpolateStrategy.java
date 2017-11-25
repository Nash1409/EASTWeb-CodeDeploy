package version2.prototype.summary.temporal;

import java.io.File;
import java.util.ArrayList;

/**
 * Interface designed for defining interpolation strategies for temporal summary calculations.
 *
 * @author michael.devos
 *
 */
public interface InterpolateStrategy {
    /**
     * Concrete classes will implement this method to define how to split single, or more, data files which contain multiple days worth of data into multiple files.
     *
     * @param days  - days the raster file should be split into
     * @param rasterFiles  - raster files to interpolate
     * @return  - list of files creates from the given rasterFile
     */
    public abstract ArrayList<File> Interpolate(int days, File... rasterFiles);

    /**
     * Gets the expected count of the number of files resulting after running this interpolation strategy on a single file.
     *
     * @return the expected number of resulting files
     */
    public abstract int GetResultingNumOfFiles();
}
