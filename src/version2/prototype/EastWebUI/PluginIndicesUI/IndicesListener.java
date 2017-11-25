package version2.prototype.EastWebUI.PluginIndicesUI;

import java.util.EventListener;

public interface IndicesListener extends EventListener {
    /** Called whenever the sun changes position
     *   in a SunEvent source object
     */
    public void AddPlugin(IndicesEventObject e);
}
