package version2.prototype.EastWebUI.PluginIndicesUI;

import java.util.ArrayList;
import java.util.EventObject;

@SuppressWarnings("serial")
public class IndicesEventObject extends EventObject {
    private String plugin;
    private ArrayList<String> globalModisTiles;

    /**
     * constructor
     * @param source
     * @param plugin
     */
    public IndicesEventObject(Object source, String plugin, ArrayList<String> globalModisTiles) {
        super(source);
        this.plugin = plugin;
        this.globalModisTiles = new ArrayList<String>();

        for(String temp : globalModisTiles)
        {
            this.globalModisTiles.add(temp);
        }
    }

    /** return whether the sun rose or set */
    public String getPlugin() {
        return plugin;
    }

    public ArrayList<String> getTiles()
    {
        return globalModisTiles;
    }
}
