package version2.prototype.summary.zonal;

import java.util.Map;

/**
 * A name result pairing container object for Summary framework.
 *
 * @author michael.devos
 *
 */
public class SummaryNameResultPair {
    /**
     * Creates a SummaryNameResultPair.
     *
     * @param simpleName  - name of summary calculation
     * @param result  - mapping of results for the summary
     */
    public SummaryNameResultPair(String simpleName, Map<Integer, Double> result){
        name = simpleName;
        this.result = result;
    }

    /**
     * Gets the simple name of the summary.
     *
     * @return the summary's simple name
     */
    public String getSimpleName(){ return name; }

    /**
     * Gets the summary's result.
     *
     * @return the summary's result.
     */
    public Map<Integer, Double> getResult(){ return result; }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString(){
        return name + ": " + result;
    }

    /**
     * Overloaded toString method that accepts a delimiter.
     *
     * @param delimiter  - string to separate the summary name from its result when converting it to a string
     * @return
     */
    public String toString(String delimiter){
        return name + delimiter + " " + result;
    }

    private String name;
    private Map<Integer, Double> result;
}
