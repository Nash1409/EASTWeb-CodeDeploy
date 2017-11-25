package version2.prototype.EastWebUI.MainWindow;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

public class MainWindowEvent implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("rawtypes")
    private transient Vector listeners;

    /** Register a listener for SunEvents */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    synchronized public void addListener(MainWindowListener l) {
        if (listeners == null) {
            listeners = new Vector();
        }
        listeners.addElement(l);
    }

    /** Remove a listener for SunEvents */
    @SuppressWarnings("rawtypes")
    synchronized public void removeListener(MainWindowListener l) {
        if (listeners == null) {
            listeners = new Vector();
        }
        listeners.removeElement(l);
    }

    /** Fire a SunEvent to all registered listeners */
    @SuppressWarnings("rawtypes")
    public void fire() {
        // if we have no listeners, do nothing...
        if (listeners != null && !listeners.isEmpty()) {
            // create the event object to send
            MainWindowEventObject event = new MainWindowEventObject(this);

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
                MainWindowListener l = (MainWindowListener) e.nextElement();
                l.RefreshProjectList(event);
            }
        }
    }
}
