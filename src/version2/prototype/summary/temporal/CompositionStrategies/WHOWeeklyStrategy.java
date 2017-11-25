package version2.prototype.summary.temporal.CompositionStrategies;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;

/**
 * @author michael.devos
 *
 */
public class WHOWeeklyStrategy implements TemporalSummaryCompositionStrategy {

    @Override
    public LocalDate getStartDate(LocalDate sDate) throws Exception {
        //        int firstDay = GregorianCalendar.MONDAY;
        //        int currentDay = sDate.get(GregorianCalendar.DAY_OF_WEEK);
        //        if(currentDay !=  firstDay){
        //            GregorianCalendar newDate = (GregorianCalendar) sDate.clone();
        //
        //            do{
        //                newDate.add(GregorianCalendar.DAY_OF_MONTH, 1);
        //                currentDay = newDate.get(GregorianCalendar.DAY_OF_WEEK);
        //            }while(currentDay != firstDay);
        //
        //            return newDate;
        //        }
        //        return sDate;
        LocalDate outDate = sDate;
        DayOfWeek currentDay = sDate.getDayOfWeek();
        DayOfWeek firstDay = DayOfWeek.MONDAY;
        if(currentDay != firstDay)
        {
            outDate = sDate.with(TemporalAdjusters.previous(DayOfWeek.MONDAY));
        }
        return outDate;
    }

    @Override
    public int getDaysInThisComposite(LocalDate dateInComposite) {
        return 7;
    }

    @Override
    public long getNumberOfCompleteCompositesInRange(LocalDate startDate, LocalDate endDate) {
        DayOfWeek startDay = startDate.getDayOfWeek();
        LocalDate adjStartDay = startDate;

        if(startDay != DayOfWeek.MONDAY)
        {
            int value = startDay.getValue();     // 1 - Monday, 7 - Sunday

            adjStartDay = startDate.plusDays(8 - value);
        }

        return ChronoUnit.WEEKS.between(adjStartDay, endDate);
    }

    @Override
    public int maxNumberOfDaysInComposite() {
        return 7;
    }

}
