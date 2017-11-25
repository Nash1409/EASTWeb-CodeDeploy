package version2.prototype.summary.zonal;

import version2.prototype.summary.zonal.summaries.SummaryCalculation;


/**
 * Container object holding the pairing of a summary with a name and an instance.
 *
 * @author michael.devos
 *
 */
public class SummaryNameInstancePair {
    /**
     * Creates a SummaryNameInstancePair.
     *
     * @param canonicalName  - canonical name of the given SummaryCalculation object
     * @param instance  - SummaryCalculation object to pair to a name
     */
    public SummaryNameInstancePair(String canonicalName, SummaryCalculation instance){
        this.canonicalName = canonicalName;
        this.instance = instance;
    }

    /**
     * Gets the summary's given canonical name.
     *
     * @return the summary's canonical name
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * Gets the summary's given instance reference.
     *
     * @return the summary's instance reference
     */
    public SummaryCalculation getInstance() {
        return instance;
    }

    private String canonicalName;
    private SummaryCalculation instance;
}
