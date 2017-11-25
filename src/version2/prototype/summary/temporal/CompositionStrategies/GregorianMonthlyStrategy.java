package version2.prototype.summary.temporal.CompositionStrategies;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;

/**
 * @author michael.devos
 *
 */
public class GregorianMonthlyStrategy implements TemporalSummaryCompositionStrategy {

    @Override
    public LocalDate getStartDate(LocalDate iDate) throws Exception {
        //LocalDate startDate = iDate;

        //        if(!startDate.equals(LocalDate.of(iDate.getYear(), iDate.getMonthValue(), 1)))
        //        {
        //            startDate = (startDate.getMonthValue() < 12 ? LocalDate.of(startDate.getYear(), startDate.getMonthValue() + 1, 1)
        //                    : LocalDate.of(startDate.getYear() + 1, 1, 1));
        //        }
        return LocalDate.of(iDate.getYear(), iDate.getMonthValue(), 1);
    }

    @Override
    public int getDaysInThisComposite(LocalDate dateInComposite) {
        //        return dateInComposite.getActualMaximum(Calendar.DAY_OF_MONTH);
        return dateInComposite.lengthOfMonth();
    }

    @Override
    public long getNumberOfCompleteCompositesInRange(LocalDate iDate, LocalDate endDate) {
        LocalDate startDate = iDate;

        if(!startDate.equals(LocalDate.of(iDate.getYear(), iDate.getMonthValue(), 1)))
        {
            startDate = (startDate.getMonthValue() < 12 ? LocalDate.of(startDate.getYear(), startDate.getMonthValue() + 1, 1)
                    : LocalDate.of(startDate.getYear() + 1, 1, 1));
        }
        return ChronoUnit.MONTHS.between(startDate, endDate);
    }

    @Override
    public int maxNumberOfDaysInComposite() {
        return 31;
    }

}
