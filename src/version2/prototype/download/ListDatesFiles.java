package version2.prototype.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import version2.prototype.DataDate;
import version2.prototype.PluginMetaData.DownloadMetaData;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;

/**
 * Abstract map object container containing the list of available downloads for a specific plugin file type.
 * This map utilizes lazy initialization to avoid slowing down project creation due to the list creation time.
 *
 */
public abstract class ListDatesFiles
{
    protected List<DataDate> lDates;
    protected DataDate sDate;
    protected DataDate eDate;
    protected DownloadMetaData mData;
    protected Map<DataDate, ArrayList<String>>  mapDatesFiles;
    protected Boolean mapDatesFilesSet;
    protected ProjectInfoFile mProject;

    /**
     * Creates ListDatesFiles object but delays list creation until CloneListDatesFiles() is first called. The contained list will be of all files on data file server described by the DownloadMetaData and
     * with a date equal to or more recent than the given DataDate.
     *
     * @param startDate
     * @param data
     * @param project
     * @throws IOException
     */
    //    public ListDatesFiles(DataDate startDate, DownloadMetaData data, ProjectInfoFile project) throws IOException
    //    {
    //        sDate = startDate;
    //        mData = data;
    //
    //        /* Check if projectInfoFile.GetStartDate() is later than plugin's Origin Date
    //         * If not, use plugin's Origin Date as the start date
    //         * */
    //        if (sDate.compareTo(new DataDate(mData.originDate)) < 0)
    //        {
    //            sDate = new DataDate(mData.originDate);
    //        }
    //
    //        lDates = null;
    //        mapDatesFiles =  null;
    //        mProject =  project;
    //        mapDatesFiles = null;
    //        mapDatesFilesSet = new Boolean(false);
    //    }

    // Overloaded (villegar)
    public ListDatesFiles(DataDate startDate, DataDate endDate, DownloadMetaData data, ProjectInfoFile project) throws IOException
    {
        sDate = startDate;
        eDate = endDate;
        mData = data;
        //javax.swing.JOptionPane.showMessageDialog(null,"Project: "+project.GetProjectName()+"\nStart date: "+sDate.toString()+"\nEnd date: "+eDate.toString()+"\nOrigin: "+mData.originDate);
        /* Check if projectInfoFile.GetStartDate() is later than plugin's Origin Date
         * If not, use plugin's Origin Date as the start date
         * */
        if (sDate.compareTo(new DataDate(mData.originDate)) < 0)
        {
            sDate = new DataDate(mData.originDate);
            if(eDate.compareTo(sDate) < 0){
                //return;
                eDate = new DataDate(mData.originDate);
            }
        }

        lDates = null;
        mapDatesFiles =  null;
        mProject =  project;
        mapDatesFiles = null;
        mapDatesFilesSet = new Boolean(false);
    }

    /**
     * Gets a map of each day and its associated files. First call to this starts the list creation and subsequent calls after this returns will reuse the created list from the first call.
     * @return map of server file paths to associated file creation dates
     */
    public Map<DataDate, ArrayList<String>> CloneListDatesFiles()
    {
        Map<DataDate, ArrayList<String>> filesMap = new HashMap<DataDate, ArrayList<String>>();
        ArrayList<String> files;

        if(!mapDatesFilesSet)
        {
            synchronized(mapDatesFilesSet)
            {
                if(!mapDatesFilesSet)
                {
                    System.out.println("Creating ListDatesFiles map for '" + mData.Title + "':'" + mData.name + "'.");
                    if ((mData.mode).equalsIgnoreCase("FTP"))
                    {
                        mapDatesFiles = ListDatesFilesFTP();
                    };

                    if ((mData.mode).equalsIgnoreCase("HTTP"))
                    {
                        mapDatesFiles = ListDatesFilesHTTP();
                    };
                    System.out.println("Finished creating ListDatesFiles map for '" + mData.Title + "':'" + mData.name + "'.");
                }
                mapDatesFilesSet = new Boolean(true);
            }
        }

        for(DataDate dd : mapDatesFiles.keySet())
        {
            files = new ArrayList<String>();
            for(String file :  mapDatesFiles.get(dd))
            {
                files.add(new String(file));
            }
            filesMap.put(dd, files);
        }
        return filesMap;
    }

    /* Overridden by each plugin using FTP protocol
     * produce the map of each day and its associated files
     */
    abstract protected Map<DataDate, ArrayList<String>> ListDatesFilesFTP();

    /* Overridden by each plugin using HTTP protocol
     * produce the map of each day and its associated files
     */
    abstract protected Map<DataDate, ArrayList<String>> ListDatesFilesHTTP();

}
