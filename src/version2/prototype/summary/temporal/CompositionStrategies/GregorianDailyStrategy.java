package version2.prototype.summary.temporal.CompositionStrategies;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import version2.prototype.summary.temporal.TemporalSummaryCompositionStrategy;

public class GregorianDailyStrategy implements TemporalSummaryCompositionStrategy {

    @Override
    public LocalDate getStartDate(LocalDate iDate) throws Exception {

        LocalDate sDate = LocalDate.ofYearDay(iDate.getYear(), 1);
        while(ChronoUnit.DAYS.between(sDate, iDate) >= 1) {
            sDate = sDate.plusDays(1);
        }
        return sDate;
    }

    @Override
    public int getDaysInThisComposite(LocalDate dateInComposite) {
        return 1;
    }

    @Override
    public int maxNumberOfDaysInComposite() {
        return 1;
    }

    @Override
    public long getNumberOfCompleteCompositesInRange(LocalDate startDate, LocalDate endDate) {

        LocalDate sDate = LocalDate.ofYearDay(startDate.getYear(), 1);
        while(ChronoUnit.DAYS.between(sDate, startDate) >= 1) {
            sDate = sDate.plusDays(1);
        }
        if(ChronoUnit.DAYS.between(sDate, startDate) != 0) {
            sDate = sDate.plusDays(1);
        }

        int count = 0;
        LocalDate nextYearDay1 = LocalDate.ofYearDay(sDate.getYear() + 1, 1);
        while(ChronoUnit.DAYS.between(sDate, endDate) >= 1) {
            if(sDate.getMonthValue() == 12 && ChronoUnit.DAYS.between(sDate, LocalDate.ofYearDay(sDate.getYear() + 1, 1)) < 1) {
                sDate = nextYearDay1;
                nextYearDay1 = nextYearDay1.plusYears(1);
            }
            else {
                sDate = sDate.plusDays(1);
            }
            count += 1;
        }
        return count;
    }

}
