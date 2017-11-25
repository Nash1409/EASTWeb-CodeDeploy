/**
 *
 */
package version2.prototype.download;

/**
 * @author michael.devos
 *
 */
public interface DownloaderFactory {
    /**
     * Creates a LocalDownloader instance determined by subclasses.
     *
     * @param gdl  - the GlobalDownloader to associate the LocalDownloader with for when loading files for processing
     * @return a LocalDownloader instance
     */
    public LocalDownloader CreateLocalDownloader(GlobalDownloader gdl);

    /**
     * Creates a GlobaldDownloader instance determined by subclasses if it registers correctly. Otherwise returns null on failure.
     *
     * @param myID  - an unique number to use as the ID for this GlobalDownloader instance (only has to be unique in comparison to currently surviving GlobalDownloader instances).
     * @return a GlobalDownloader instance
     */
    public GlobalDownloader CreateGlobalDownloader(int myID);
}
