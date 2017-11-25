/**
 *
 */
package version2.prototype.PluginMetaData;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author michael.devos
 *
 */
public class IndicesMetaData extends ProcessMetaData {
    public final ArrayList<String> indicesNames;

    private NodeList nList;

    public IndicesMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, NodeList n) {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        nList = n;

        indicesNames = new ArrayList<String>();
        int nodesIndices = ((Element) nList.item(0)).getElementsByTagName("ClassName").getLength();
        for(int i = 0; i < nodesIndices; i++) {
            indicesNames.add( ((Element) nList.item(0)).getElementsByTagName("ClassName").item(i).getTextContent());
        }
    }

    /**
     * Provides a means to create a custom IndicesMetaData object mainly for testing purposes.
     * @param Title
     * @param QualityControlMetaData
     * @param DaysPerInputData
     * @param Resolution
     * @param CompositesContinueIntoNextYear
     * @param ExtraDownloadFiles
     * @param indicesNames
     */
    public IndicesMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, ArrayList<String> indicesNames)
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        this.indicesNames = indicesNames;
        nList = null;
    }
}
