package version2.prototype.summary.temporal;

import java.io.File;

import version2.prototype.DataDate;

/**
 * Pairing object of a File object and a DataDate object.
 *
 * @author michael.devos
 *
 */
public class FileDatePair {
    public File file;
    public DataDate date;

    /**
     * Creates a FileDatePair object for given File and DataDate objects.
     *
     * @param f  - raster file for pairing
     * @param d  - associated DataDate of paired raster file
     */
    public FileDatePair(File f, DataDate d)
    {
        file = f;
        date = d;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof FileDatePair)) {
            return false;
        }

        FileDatePair o = (FileDatePair) obj;
        if(date.compareTo(o.date) != 0) {
            return false;
        }
        if(file.compareTo(o.file) != 0) {
            return false;
        }

        return true;
    }
}
