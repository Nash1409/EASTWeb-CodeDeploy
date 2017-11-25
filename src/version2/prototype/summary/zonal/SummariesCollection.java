package version2.prototype.summary.zonal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import version2.prototype.summary.zonal.summaries.Count;
import version2.prototype.summary.zonal.summaries.Max;
import version2.prototype.summary.zonal.summaries.Mean;
import version2.prototype.summary.zonal.summaries.Min;
import version2.prototype.summary.zonal.summaries.SqrSum;
import version2.prototype.summary.zonal.summaries.StdDev;
import version2.prototype.summary.zonal.summaries.Sum;
import version2.prototype.summary.zonal.summaries.SummaryCalculation;

/**
 * Represents a collection of registered summaries as SummaryCalculations facilitating interaction between summaries to share computations.
 *
 * @author michael.devos
 *
 */
public class SummariesCollection {
    /**
     * Creates a SummariesCollection object.
     *
     * @param summaryNames  - case-sensitive names of the classes of SummaryCalculation objects to create
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public SummariesCollection(ArrayList<String> summaryNames) throws ClassNotFoundException, NoSuchMethodException, SecurityException,
    InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        summaries = new ArrayList<SummaryCalculation>();
        registry = new ArrayList<SummaryNameInstancePair>();
        SummaryCalculation temp = null;
        String canonicalPath = this.getClass().getCanonicalName().substring(0, this.getClass().getCanonicalName().lastIndexOf(".") + 1) + "summaries.";
        boolean alreadyRegistered = false;

        if(summaryNames.size() > 0){
            for(String name : summaryNames){
                if(!name.equals("")){
                    Class<?> summary =  Class.forName(canonicalPath + name);
                    Constructor<?> summaryCons = summary.getConstructor(SummariesCollection.class);

                    if(registry.size() == 0){
                        temp = (SummaryCalculation)summaryCons.newInstance(this);
                        summaries.add(temp);
                        registry.add(new SummaryNameInstancePair(temp.getCanonicalName(), temp));
                    }
                    else{
                        temp = null;
                        alreadyRegistered = false;
                        for(SummaryNameInstancePair pair : registry){
                            if(pair.getCanonicalName().equals(canonicalPath + name)) {
                                alreadyRegistered = true;
                                temp = pair.getInstance();
                            }
                        }
                        if(alreadyRegistered){
                            summaries.add(temp);
                        }
                        else {
                            temp = (SummaryCalculation)summaryCons.newInstance(this);
                            summaries.add(temp);
                            registry.add(new SummaryNameInstancePair(temp.getCanonicalName(), temp));
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieve a reference to the SummaryCalculation object (if there is one) with the given canonical name in the stored collection.
     *
     * @param canonicalName  - SummaryCalculation object's registered canonical name as gotten with SummaryCalculation.getCanonicalName()
     * @return reference to the registered SummaryCalculation
     */
    public SummaryCalculation lookup(String canonicalName){
        boolean found = false;
        int i = 0;
        SummaryCalculation instance = null;
        SummaryNameInstancePair pair = null;

        while(!found && (i < registry.size())){
            pair = registry.get(i);
            if(pair.getCanonicalName().equalsIgnoreCase(canonicalName)){
                instance = pair.getInstance();
                found = true;
            }
            i = i + 1;
        }

        return instance;
    }

    /**
     * Adds a new value into the collection by adding the value, at the specified index, into all distinct leaflet SummaryCalculation objects registered to the collection.
     *
     * @param index  - index of given value
     * @param value  - double value to insert
     */
    public void add(int index, double value){
        ArrayList<SummaryCalculation> leavesFromSummaries = new ArrayList<SummaryCalculation>();
        ArrayList<SummaryCalculation> temp = null;
        for(SummaryCalculation summary : summaries){
            temp = summary.getDistinctLeaflets();
            if(temp != null){
                for(SummaryCalculation s : temp){
                    if(!leavesFromSummaries.contains(s)) {
                        leavesFromSummaries.add(s);
                    }
                }
            }
        }

        for(SummaryCalculation summary : leavesFromSummaries){
            summary.add(index, value);
        }
    }

    /**
     * Registers a SummaryNameInstancePair into the collection.
     *
     * @param pair  - SummaryNameInstancePair to register
     * @return reference to SummaryCalculation that is registered with the given name; if none then this will return the same reference as in the given
     * SummaryNameInstancePair
     */
    public SummaryCalculation register(SummaryNameInstancePair pair){
        SummaryCalculation temp = lookup(pair.getCanonicalName());
        if(temp == null){
            registry.add(pair);
            temp = pair.getInstance();
        }
        return temp;
    }

    /**
     * Gets the results of all registered summaries.
     *
     * @return list of all registered summary results
     */
    public ArrayList<SummaryNameResultPair> getResults(){
        ArrayList<SummaryNameResultPair> results = new ArrayList<SummaryNameResultPair>();
        for(SummaryCalculation summary : summaries){
            results.add(new SummaryNameResultPair(summary.getClass().getSimpleName(), summary.getResult()));
        }
        return results;
    }

    /**
     * Provides a list of the names of the framework supplied SummaryCalculation implementations.
     *
     * @return String list of the provided SummaryCalculation implementation's canonical names
     */
    public static ArrayList<String> providedSummaryCalculations()
    {
        ArrayList<String> list = new ArrayList<String>(7);
        list.add(new Count(null).getCanonicalName());
        list.add(new Max(null).getCanonicalName());
        list.add(new Mean(null).getCanonicalName());
        list.add(new Min(null).getCanonicalName());
        list.add(new SqrSum(null).getCanonicalName());
        list.add(new StdDev(null).getCanonicalName());
        list.add(new Sum(null).getCanonicalName());
        return list;
    }

    private ArrayList<SummaryNameInstancePair> registry;
    private ArrayList<SummaryCalculation> summaries;
}
