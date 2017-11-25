/**
 *
 */
package version2.prototype.PluginMetaData;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author michael.devos
 *
 */
public class ExtraInfoData extends ProcessMetaData {
    public final Boolean Tiles;

    private final NodeList nList;

    protected ExtraInfoData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, NodeList n) {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        nList = n;

        Node extraInfo = nList.item(0);

        Tiles = Boolean.parseBoolean(((Element) extraInfo).getElementsByTagName("Tiles").item(0).getTextContent().toUpperCase());
    }
}
