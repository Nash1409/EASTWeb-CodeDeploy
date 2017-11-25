package version2.prototype.EastWebUI.SummaryUI;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

public class SummaryEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("rawtypes")
    private transient Vector listeners;

    /** Register a listener for SunEvents */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    synchronized public void addListener(SummaryListener l) {
        if (listeners == null) {
            listeners = new Vector();
        }
        listeners.addElement(l);
    }

    /** Remove a listener for SunEvents */
    @SuppressWarnings("rawtypes")
    synchronized public void removeListener(SummaryListener l) {
        if (listeners == null) {
            listeners = new Vector();
        }
        listeners.removeElement(l);
    }

    /** Fire a SunEvent to all registered listeners */
    @SuppressWarnings("rawtypes")
    protected void fire(String p) {
        // if we have no listeners, do nothing...
        if (listeners != null && !listeners.isEmpty()) {
            // create the event object to send
            SummaryEventObject event = new SummaryEventObject(this, p);

            // make a copy of the listener list in case
            //   anyone adds/removes listeners
            Vector targets;
            synchronized (this) {
                targets = (Vector) listeners.clone();
            }

            // walk through the listener list and
            //   call the sunMoved method in each
            Enumeration e = targets.elements();
            while (e.hasMoreElements()) {
                SummaryListener l = (SummaryListener) e.nextElement();
                l.AddSummary(event);
            }
        }
    }
}
