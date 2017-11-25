package version2.prototype.summary.zonal.summaries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import version2.prototype.summary.zonal.SummariesCollection;
import version2.prototype.summary.zonal.SummaryNameInstancePair;

/**
 * Represents a standard deviation summary based on indexed double values.
 *
 * @author michael.devos
 *
 */
public class StdDev extends SummaryCalculation {
    //    private SummaryCalculation sqrSum;
    private SummaryCalculation count;
    private SummaryCalculation mean;

    /**
     * Creates a StdDev object representing a standard deviation summary.
     *
     * @param col  - the SummariesCollection to register itself to
     */
    public StdDev(SummariesCollection col) {
        super(col);
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#put(int, double)
     */
    @Override
    protected void put(int index, double value) {
        //        count.add(index, value);
        //        mean.add(index, value);
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#getResult()
     */
    @Override
    public Map<Integer, Double> getResult() {
        if(resultMap.size() == 0 || resultMap.size() < count.getResult().size()){
            Map<Integer, LinkedList<Double>> valuesMap = count.getValuesMap();
            //            Map<Integer, Double> sqrSumRs = sqrSum.getResult();
            Map<Integer, Double> countRs = count.getResult();
            Map<Integer, Double> meanRs = mean.getResult();
            Double tempSum;
            Double tempResult;
            Double tempValue;
            Double tempMeanRs;
            LinkedList<Double> tempValuesList;
            Iterator<Integer> keys = countRs.keySet().iterator();
            Integer key;

            while(keys.hasNext())
            {
                key = keys.next();
                tempSum = 0.0;
                tempValuesList = valuesMap.get(key);
                if(tempValuesList != null)
                {
                    for(int x_i=0; x_i < tempValuesList.size(); x_i++)
                    {
                        // Summation of the squared differences from the mean
                        tempValue = valuesMap.get(key).get(x_i);
                        tempMeanRs = meanRs.get(key);
                        if(tempValue != null && tempMeanRs != null) {
                            tempSum += Math.pow(tempValue - tempMeanRs, 2);
                        }
                    }
                    // Calculate variance and std dev
                    if(tempValuesList.size() > 0) {
                        tempResult = Math.sqrt(tempSum / tempValuesList.size());
                    } else {
                        tempResult = null;
                    }
                } else {
                    tempResult = 0.0;
                }
                if(new Double(tempResult).equals(Double.NaN)) {
                    resultMap.put(key, null);
                }
                else{
                    resultMap.put(key, tempResult);
                }
            }
        }
        return resultMap;
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#getDistinctLeaflets()
     */
    @Override
    public ArrayList<SummaryCalculation> getDistinctLeaflets() {
        ArrayList<SummaryCalculation> temp = new ArrayList<SummaryCalculation>(3);
        temp.add(count);
        //        temp.add(sqrSum);
        ArrayList<SummaryCalculation> fromMean = mean.getDistinctLeaflets();
        if(fromMean.get(0).getClass().getSimpleName().equalsIgnoreCase("sum")) {
            temp.add(fromMean.get(0));
        } else {
            temp.add(fromMean.get(1));
        }
        return temp;
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#registerDependencies()
     */
    @Override
    protected void registerDependencies() {
        SummariesCollection col = getCollection();
        //        sqrSum = new SqrSum(col);
        //        sqrSum = col.register(new SummaryNameInstancePair(sqrSum.getCanonicalName(), sqrSum));

        count = new Count(col);
        count = col.register(new SummaryNameInstancePair(count.getCanonicalName(), count));

        mean = new Mean(col);
        mean = col.register(new SummaryNameInstancePair(mean.getCanonicalName(), mean));
    }

}
