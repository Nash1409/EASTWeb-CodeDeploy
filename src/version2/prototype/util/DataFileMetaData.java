package version2.prototype.util;


/**
 * Represents the metadata stored for a cached data file within the database. To read the data within it must be gotten from one of the given Read* methods that return specific *FileMetaData objects.
 *
 * @author michael.devos
 *
 */
public class DataFileMetaData {
    private final String dataName;
    private final String filePath;
    private final int dateGroupID;
    private final int year;
    private final int day;
    private final String indexNm;
    //    private final ArrayList<DataFileMetaData> extraDownloads;

    /**
     * Creates a DataFileMetaData object initialized with the given metadata and defaults the environmental index to null.
     *
     * @param dataName  - name of the data this file represents (data name = plugin name)
     * @param filePath  - full path to the data file
     * @param dateGroupID  - the combination key ID of year and day
     * @param year  - the Gregorian year the data file is relevant to
     * @param day  - the Gregorian day of the year the data file is relevant to
     */
    public DataFileMetaData(String dataName, String filePath, int dateGroupID, int year, int day)
    {
        this.dataName = dataName;
        this.filePath = filePath;
        this.dateGroupID = dateGroupID;
        this.year = year;
        this.day = day;
        indexNm = null;
    }

    /**
     * Creates a DataFileMetaData object initialized with the given metadata and defaults the QC file path to null.
     *
     * @param filePath  - full path to the data file
     * @param dateGroupID  - the combination key ID of year and day
     * @param year  - the Gregorian year the data file is relevant to
     * @param day  - the Gregorian day of the year the data file is relevant to
     * @param indexNm  - the environmental index associated to the data file
     */
    public DataFileMetaData(String filePath, int dateGroupID, int year, int day, String indexNm)
    {
        dataName = "Data";
        this.filePath = filePath;
        this.dateGroupID = dateGroupID;
        this.year = year;
        this.day = day;
        this.indexNm = indexNm;
    }

    /**
     * Copy constructor
     *
     * @param dData  - DownloadFileMetaData object to create a copy DataFileMetaData object of
     */
    public DataFileMetaData(DownloadFileMetaData dData)
    {
        dataName = dData.dataName;
        filePath = dData.dataFilePath;
        dateGroupID = dData.dateGroupID;
        year = dData.year;
        day = dData.day;
        indexNm = null;
    }

    /**
     * Copy constructor
     *
     * @param pData  - ProcessorFileMetaData object to create a copy DataFileMetaData object of
     */
    public DataFileMetaData(ProcessorFileMetaData pData)
    {
        dataName = null;
        filePath = pData.dataFilePath;
        dateGroupID = pData.dateGroupID;
        year = pData.year;
        day = pData.day;
        indexNm = null;
        //        extraDownloads = null;
    }

    /**
     * Copy constructor
     *
     * @param iData  - IndicesFileMetaData object to create a copy DataFileMetaData object of
     */
    public DataFileMetaData(IndicesFileMetaData iData)
    {
        dataName = null;
        filePath = iData.dataFilePath;
        dateGroupID = iData.dateGroupID;
        year = iData.year;
        day = iData.day;
        indexNm = iData.indexNm;
        //        extraDownloads = null;
    }

    /**
     * Retrieves only the data relevant to Processor from this DataFileMetaData as a DownloadFileMetaData object.
     *
     * @return a tailored DownloadFileMetaData object
     */
    public DownloadFileMetaData ReadMetaDataForProcessor()
    {
        //        if (extraDownloads != null)
        //        {
        //            ArrayList<DownloadFileMetaData> extras = new ArrayList<DownloadFileMetaData>(extraDownloads.size());
        //            for(DataFileMetaData extraData : extraDownloads)
        //            {
        //                extras.add(extraData.ReadMetaDataForProcessor());
        //            }
        //            return new DownloadFileMetaData(dataName, filePath, year, day, extras);
        //        } else {
        //            return new DownloadFileMetaData(dataName, filePath, year, day, null);
        return new DownloadFileMetaData(dataName, filePath, dateGroupID, year, day);
    }

    /**
     * Retrieves only the data relevant to Indices from this DataFileMetaData as a ProcessorFileMetaData object.
     *
     * @return a tailored ProcessorFileMetaData object
     */
    public ProcessorFileMetaData ReadMetaDataForIndices()
    {
        return new ProcessorFileMetaData(filePath, dateGroupID, year, day);
    }

    /**
     * Retrieves only the data relevant to Summary from this DataFileMetaData as a IndicesFileMetaData object.
     *
     * @return a tailored IndicesFileMetaData object
     */
    public IndicesFileMetaData ReadMetaDataForSummary()
    {
        return new IndicesFileMetaData(filePath, dateGroupID, year, day, indexNm);
    }
}
