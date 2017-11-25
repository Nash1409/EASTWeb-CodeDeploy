package version2.prototype.summary.zonal.summaries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import version2.prototype.summary.zonal.SummariesCollection;
import version2.prototype.summary.zonal.SummaryNameInstancePair;

/**
 * Represents a mean summary based on indexed double values.
 *
 * @author michael.devos
 *
 */
public class Mean extends SummaryCalculation {
    private SummaryCalculation sum;
    private SummaryCalculation count;

    /**
     * Creates a Mean object representing a mean summary.
     *
     * @param col  - the SummariesCollection to register itself to
     */
    public Mean(SummariesCollection col) {
        super(col);
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#put(int, double)
     */
    @Override
    protected void put(int index, double value) {
        //        sum.add(index, value);
        //        count.add(index, value);
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#getResult()
     */
    @Override
    public Map<Integer, Double> getResult() {
        if(resultMap.size() == 0 || resultMap.size() < count.getResult().size()){
            Map<Integer, Double> sumRs = sum.getResult();
            Map<Integer, Double> countRs = count.getResult();

            Double tempSum;
            Double tempCount;
            Double tempR;
            Iterator<Integer> keys = countRs.keySet().iterator();
            Integer key;
            while(keys.hasNext())
            {
                key = keys.next();
                tempSum = sumRs.get(key);
                tempCount = countRs.get(key);
                if(tempSum == null || tempCount == null) {
                    tempR = null;
                } else {
                    tempR = tempSum/tempCount;
                }
                resultMap.put(key, tempR);
            }
        }
        return resultMap;
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#getDistinctLeaflets()
     */
    @Override
    public ArrayList<SummaryCalculation> getDistinctLeaflets() {
        ArrayList<SummaryCalculation> temp = new ArrayList<SummaryCalculation>(2);
        temp.add(count);
        temp.add(sum);
        return temp;
    }

    /* (non-Javadoc)
     * @see version2.prototype.summary.summaries.SummaryCalculation#registerDependencies()
     */
    @Override
    protected void registerDependencies() {
        SummariesCollection col = getCollection();
        sum = new Sum(col);
        sum = col.register(new SummaryNameInstancePair(sum.getCanonicalName(), sum));

        count = new Count(col);
        count = col.register(new SummaryNameInstancePair(count.getCanonicalName(), count));
    }
}
