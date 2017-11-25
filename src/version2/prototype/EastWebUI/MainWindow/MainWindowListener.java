package version2.prototype.EastWebUI.MainWindow;

import java.util.EventListener;

public interface MainWindowListener extends EventListener {
    /** Called whenever the sun changes position
     *   in a SunEvent source object
     */
    public void RefreshProjectList(MainWindowEventObject e);
}
