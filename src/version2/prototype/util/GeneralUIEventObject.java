package version2.prototype.util;

import java.util.EventObject;

/**
 * @author michael.devos
 * @author sufiabdul
 *
 */
@SuppressWarnings("serial")
public class GeneralUIEventObject extends EventObject{
    private final String status;

    /**
     * Creates a GeneralUIEventObject for a progress update solely for sending a log entry (status).
     * @param source  - the object that's creating this object
     * @param status  - message to append to the log
     */
    public GeneralUIEventObject(Object source, String status) {
        super(source);
        this.status = status;
    }

    /** return whether the sun rose or set
     * @return String arbitrary status string appended to the log
     */
    public String getStatus() {
        return status;
    }
}
