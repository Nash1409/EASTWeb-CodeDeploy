package version2.prototype.Scheduler;

/**
 * Identifiers for the four multithreaded frameworks.
 *
 * @author michael.devos
 *
 */
public enum ProcessName {
    DOWNLOAD,
    PROCESSOR,
    INDICES,
    SUMMARY;

    @Override
    public String toString() {
        String name = null;

        switch(name())
        {
        case "DOWNLOAD":
            name = "Download";
            break;
        case "INDICES":
            name = "Indices";
            break;
        case "PROCESSOR":
            name = "Processor";
            break;
        default:    // SUMMARY
            name = "Summary";
            break;
        }
        return name;
    }
}
