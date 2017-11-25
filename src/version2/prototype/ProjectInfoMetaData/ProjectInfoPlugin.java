package version2.prototype.ProjectInfoMetaData;

import java.util.ArrayList;

import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * @author michael.devos
 *
 */
@Immutable public class ProjectInfoPlugin {
    private final String Name;
    private final ArrayList<String> Indices;
    private final String QC;
    private final ArrayList<String> ModisTiles;

    /**
     * Create an immutable ProjectInfoPlugin object that contains the plugin name, list of index names to calculate, and the QC level to use. This information is generally gotten from a
     * parsed project metadata file in a "Plugin" node child of a "Plugins" node.
     *
     * @param name  - plugin name
     * @param indices  - list of indices to calculate by their names
     * @param qc  - QC level
     */
    public ProjectInfoPlugin(String name, ArrayList<String> indices, String qc, ArrayList<String> modisTiles)
    {
        Name = name;
        ArrayList<String> temp = new ArrayList<String>();
        if(indices != null){
            for(String str : indices) {
                temp.add(str.trim());
            }
        }
        Indices = temp;
        QC = qc;
        temp = new ArrayList<String>();
        if(modisTiles != null) {
            for(String str : modisTiles) {
                temp.add(str.trim());
            }
        }
        ModisTiles = temp;
    }

    /**
     * Gets the name of the plugin being described.
     * @return the plugin name
     */
    public String GetName()
    {
        return Name;
    }

    /**
     * Gets the list of the names of the indices to process for the described plugin.
     * @return list of the names of the indices to calculate
     */
    public ArrayList<String> GetIndices()
    {
        return Indices;
    }

    /**
     * Value of the QC flag to use during processing for the described plugin.
     * @return QC flag value to use
     */
    public String GetQC()
    {
        return QC;
    }

    public ArrayList<String> GetModisTiles()
    {
        return ModisTiles;
    }
}
