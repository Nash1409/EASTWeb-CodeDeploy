package version2.prototype;

import java.util.ArrayList;

import version2.prototype.util.DataFileMetaData;

public class ProcessWorkerReturn {
    public final ArrayList<DataFileMetaData> filesToCache;

    public ProcessWorkerReturn(ArrayList<DataFileMetaData> filesToCache)
    {
        this.filesToCache = filesToCache;
    }
}
