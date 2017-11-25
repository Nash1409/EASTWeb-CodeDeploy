package version2.prototype.EastWebUI.PluginIndicesUI;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

@SuppressWarnings("serial")
public class IndicesEvent implements Serializable {
    @SuppressWarnings("rawtypes")
    private transient Vector listeners;

    /** Register a listener for SunEvents */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    synchronized public void addListener(IndicesListener l) {
        if (listeners == null) {
            listeners = new Vector();
        }
        listeners.addElement(l);
    }

    /** Remove a listener for SunEvents */
    @SuppressWarnings("rawtypes")
    synchronized public void removeListener(IndicesListener l) {
        if (listeners == null) {
            listeners = new Vector();
        }
        listeners.removeElement(l);
    }

    /** Fire a SunEvent to all registered listeners */
    @SuppressWarnings("rawtypes")
    protected void fire(String p, ArrayList<String> globalModisTiles) {
        // if we have no listeners, do nothing...
        if (listeners != null && !listeners.isEmpty()) {
            // create the event object to send
            IndicesEventObject event = new IndicesEventObject(this, p, globalModisTiles);

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
                IndicesListener l = (IndicesListener) e.nextElement();
                l.AddPlugin(event);
            }
        }
    }
}
