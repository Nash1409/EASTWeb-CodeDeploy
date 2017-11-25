package version2.prototype.summary.zonal.summaries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import version2.prototype.summary.zonal.SummariesCollection;

/**
 * Abstract summary definition. Implementing classes are to define a calculation of a summary for double values. Classes won't be allowed to be instantiated multiple
 * times but will share their results with all requiring classes registered within a SummariesCollection object.
 *
 * @author michael.devos
 *
 */
public abstract class SummaryCalculation {
    private final String canonicalName;
    private final SummariesCollection col;
    private Map<Integer, LinkedList<Double>> valuesMap;
    protected Map<Integer, Double> resultMap;

    /**
     * Create a SummaryCalculation. Registers this instance to the given SummariesCollection object along with all its dependencies. Implementing classes have to take
     * care of defining the dependencies and the calculation this instance will do with acquired doubles.
     *
     * @param col
     */
    public SummaryCalculation(SummariesCollection col){
        canonicalName = this.getClass().getCanonicalName();
        this.col = col;
        valuesMap = new HashMap<Integer, LinkedList<Double>>();
        resultMap = new HashMap<Integer, Double>();
        registerDependencies();
    }

    /**
     * Implementing classes will override this method and register required dependencies by creating instances of required SummaryCalculations and registering them.
     * The SummariesCollection object will handle returning the correct reference to the stored SummaryCalculation to use. If a SummaryCalculation has no dependencies
     * then it need not override this method.
     */
    protected void registerDependencies(){    }

    /**
     * Adds the specified value to the calculation map at the given index. Values don't necessarily replace previously added values at the same indices.
     * What happens is determined by the calculation being implemented by subclasses.
     *
     * @param index  - index within the map the value is to be added to. All indices should be initialized to zero.
     * @param value  - value to add to the specified index
     */
    public final void add(int index, double value)
    {
        if(valuesMap.get(index) == null) {
            LinkedList<Double> temp = new LinkedList<Double>();
            temp.add(value);
            valuesMap.put(index, temp);
        }
        else {
            valuesMap.get(index).add(value);
        }
        put(index, value);
    }

    /**
     * Get the resulting calculation.
     *
     * @return returns a HashMap object containing results to this objects calculation to each index
     */
    public abstract Map<Integer, Double> getResult();

    /**
     * A full name to this calculation. Not to be abbreviated. SummaryCalculations are looked up in SummariesCollections using this value.
     *
     * @return a name to identify this calculation by (class name of the implementing SummaryCalculation)
     */
    public String getCanonicalName(){
        return canonicalName;
    }

    /**
     * Returns a copy of the map of the values added to this SummaryCalculation. Values are listed in a LinkedList. WARNING: This is only
     * added to in the leaflet SummaryCalculations (the SummaryCalculations whom are returned by a call to getDistinctLeaflets() by one of
     * the SummaryCalculation objects in the SummariesCollection and/or whom implement put(int, double)).
     *
     * @return HashMap<Integer, LinkedList<Double>> object representing all values add to the calculation and their assigned indices
     */
    public final Map<Integer, LinkedList<Double>> getValuesMap()
    {
        //        return new HashMap<Integer, LinkedList<Double>>(valuesMap);
        return valuesMap;
    }

    /**
     * Gets the list of SummaryCalculation objects that are this object's dependencies. Must return the list of unique dependencies amongst all of its dependencies and
     * itself. Only SummaryCalculation objects that have no dependencies are to be included in this list (hence distinct leaflets, or leaves of the tree structure).
     * These can be found from dependencies by finding SummaryCalculation objects which return a list of only themselves for this method.
     *
     * Only SummaryCalculation objects returned here will have their put(int, double) method called. Others are expected to use these calculations in their getResult() method.
     *
     * @return list of SummaryCalculation objects used by this SummaryCalculation which are dependent on no other SummaryCalculations
     * (calling this method on those returned yields an empty or null list).
     */
    public abstract ArrayList<SummaryCalculation> getDistinctLeaflets();

    /**
     * Implementing classes override this method to handle calculations needing to be done on a per value basis and the valuesMap gotten by the getValuesMap() method is insufficient.
     * SummaryCalculation objects who returns an empty or null list with getDistinctLeaflets() (in other words uses no other SummaryCalculation object within itself) must implement
     * this method. Others, may make an empty method to handle the implementation. Internally the values should be stored in a Map<Integer, Double> type awaiting the call of getResult.
     *
     * Only SummaryCalculation objects returned by getDistinctLeaflets() will have their put(int, double) method called. Others are expected to use these calculations in their getResult() method.
     *
     * @param index  - index within the map the value is to be added to. All indices should be initialized to zero.
     * @param value  - value to add to the specified index
     */
    protected abstract void put(int index, double value);

    /**
     * Gets the SummariesCollection object used in the creation of this SummaryCalculation.
     *
     * @return SummariesCollection object used in instantiation
     */
    protected final SummariesCollection getCollection()
    {
        return col;
    }
}
