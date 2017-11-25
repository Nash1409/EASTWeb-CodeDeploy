/**
 *
 */
package version2.prototype.PluginMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author michael.devos
 *
 */
public class ProcessorMetaData extends ProcessMetaData {

    private NodeList nList;

    public final Map<Integer, String> processStep;
    public final Integer numOfOutput;

    public ProcessorMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles, NodeList n){
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        nList = n;
        processStep = new HashMap<Integer, String>();
        Element processorNode = (Element) nList.item(0);

        NodeList processSteps = processorNode.getElementsByTagName("ProcessStep");

        for(int i=0; i < processSteps.getLength(); i++)
        {
            processStep.put(i+1, processSteps.item(i).getTextContent());
        }

        numOfOutput = Integer.parseInt(processorNode.getElementsByTagName("NumberOfOutput").item(0).getTextContent());
    }

    /**
     * Provides a means to create a custom ProcessorMetaData object mainly for testing purposes.
     * @param Title
     * @param QualityControlMetaData
     * @param DaysPerInputData
     * @param Resolution
     * @param CompositesContinueIntoNextYear
     * @param ExtraDownloadFiles
     * @param processSteps
     * @param numOfOutput
     */
    public ProcessorMetaData(String Title, ArrayList<String> QualityControlMetaData, Integer DaysPerInputData, Integer Resolution, Boolean CompositesContinueIntoNextYear, Boolean ExtraIndices, ArrayList<String> ExtraDownloadFiles,
            Map<Integer, String> processSteps, Integer numOfOutput)
    {
        super(Title, QualityControlMetaData, DaysPerInputData, Resolution, CompositesContinueIntoNextYear, ExtraIndices, ExtraDownloadFiles);
        processStep = processSteps;
        this.numOfOutput = numOfOutput;
    }
}
