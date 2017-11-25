package version2.prototype.util;

/**
 *
 * @author michael.devos
 *
 */
public class DownloadFileMetaData {
    public final String dataName;
    public final String dataFilePath;
    public final int dateGroupID;
    public final int year;
    public final int day;

    /**
     * Creates a DownloadFileMetaData object initialized to the given values.
     *
     * @param dataName  - name of the data file this object represents
     * @param dataFilePath  - full path to the data file
     * @param dateGroupID  - the combination key ID of year and day
     * @param year  - the Gregorian year the data file is relevant to
     * @param day  - the Gregorian day of the year the data file is relevant to
     */
    public DownloadFileMetaData(String dataName, String dataFilePath, int dateGroupID, int year, int day) {
        this.dataName = dataName;
        this.dataFilePath = dataFilePath;
        this.dateGroupID = dateGroupID;
        this.year = year;
        this.day = day;
    }
}
