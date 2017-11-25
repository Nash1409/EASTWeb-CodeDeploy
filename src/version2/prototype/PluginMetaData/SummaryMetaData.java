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
public class SummaryMetaData extends ProcessMetaData {
    public final String mergeStrategyClass;
    public final String interpolateStrategyClass;

    private NodeList nList;

    public SummaryMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, NodeList n){
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        nList = n;
        Node summaryNode = nList.item(0);

        //            NodeList temporal = ((Element) nList).getElementsByTagName("Temporal");
        Node temporalNode = ((Element) summaryNode).getElementsByTagName("Temporal").item(0);

        // Node: DaysPerInputData
        //            Node DaysPerInputData = ((Element) temporal).getElementsByTagName("DaysPerInputData").item(0);
        //            NodeList tempList = DaysPerInputData.getChildNodes();
        //            Node valueNode = tempList.item(0);
        //            daysPerInputData = Integer.parseInt(valueNode.getNodeValue().trim());
        //            daysPerInputData = Integer.parseInt(((Element) temporalNode).getElementsByTagName("DaysPerInputData").item(0).getTextContent());

        // Node: MergeStrategyClass
        mergeStrategyClass = (((Element) temporalNode).getElementsByTagName("MergeStrategyClass").item(0).getTextContent());

        // Node: InterpolateStrategyClass
        interpolateStrategyClass = (((Element) temporalNode).getElementsByTagName("InterpolateStrategyClass").item(0).getTextContent());
    }

    /**
     * Provides a means to create a custom SummaryMetaData object mainly for testing purposes.
     * @param Title
     * @param QualityControlMetaData
     * @param DaysPerInputData
     * @param Resolution
     * @param CompositesContinueIntoNextYear
     * @param ExtraDownloadFiles
     * @param mergeStrategyClass
     * @param interpolateStrategyClass
     */
    public SummaryMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles,
            String mergeStrategyClass, String interpolateStrategyClass)
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        this.mergeStrategyClass = mergeStrategyClass;
        this.interpolateStrategyClass = interpolateStrategyClass;
    }
}
