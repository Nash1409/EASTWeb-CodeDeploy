package version2.prototype;

import java.io.Serializable;
import java.time.LocalDate;

public final class DataDate implements Comparable<DataDate>, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final int dayOfMonth;
    private final int month;
    private final int year;
    private final int dayOfYear;
    //the range of hour is 0-23. default value is 0
    private final int hour;

    public static DataDate DataDateWithHour(int hour, int dayOfYear, int year){
        // Compute day, month
        //        GregorianCalendar cal = getClearedCalendar();
        //        cal.set(GregorianCalendar.YEAR, year);
        //        cal.set(GregorianCalendar.DAY_OF_YEAR, dayOfYear);
        //        int dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
        //        int month = cal.get(GregorianCalendar.MONTH) + 1; // Add 1 to convert from 0-based months to 1-based months
        int dayOfMonth = LocalDate.ofYearDay(year, dayOfYear).getDayOfMonth();
        int month = LocalDate.ofYearDay(year, dayOfYear).getMonthValue();
        return new DataDate(hour,dayOfMonth,month,year);
    }

    public LocalDate getLocalDate() {
        //        final GregorianCalendar cal = getClearedCalendar();
        //        cal.set(year, month - 1, day); // Subtract 1 to convert from 1-based months to 0-based months
        LocalDate date = LocalDate.of(year, month, dayOfMonth);
        return date;
    }

    public DataDate(int dayOfMonth, int month, int year) {
        this.dayOfMonth = dayOfMonth;
        this.month = month;
        this.year = year;
        hour=0;
        // Compute dayOfYear
        //        GregorianCalendar cal = getClearedCalendar();
        //        cal.set(year, month - 1, day); // Subtract 1 to convert from 1-based months to 0-based months
        //        dayOfYear = cal.get(GregorianCalendar.DAY_OF_YEAR);
        LocalDate date = LocalDate.of(year, month, dayOfMonth);
        //dayOfYear = LocalDate.now().getDayOfYear();
        dayOfYear = date.getDayOfYear();
    }

    public DataDate(int dayOfYear, int year) {
        this.dayOfYear = dayOfYear;
        this.year = year;
        hour=0;

        // Compute day, month
        //        GregorianCalendar cal = getClearedCalendar();
        //        cal.set(GregorianCalendar.YEAR, year);
        //        cal.set(GregorianCalendar.DAY_OF_YEAR, dayOfYear);
        //        dayOfMonth = cal.get(GregorianCalendar.DAY_OF_MONTH);
        //        month = cal.get(GregorianCalendar.MONTH) + 1; // Add 1 to convert from 0-based months to 1-based months
        dayOfMonth = LocalDate.ofYearDay(year, dayOfYear).getDayOfMonth();
        month = LocalDate.ofYearDay(year, dayOfYear).getMonthValue();
    }

    public DataDate(LocalDate date) {
        //        dayOfMonth = cal.get(GregorianCalendar.DATE);
        //        month = cal.get(GregorianCalendar.MONTH) + 1; // Add 1 to convert from 0-based months to 1-based months
        //        year = cal.get(GregorianCalendar.YEAR);
        //        dayOfYear = cal.get(GregorianCalendar.DAY_OF_YEAR);
        dayOfMonth = date.getDayOfMonth();
        dayOfYear = date.getDayOfYear();
        month = date.getMonthValue();
        year = date.getYear();
        hour=0;
    }

    //construct for date which contains hour
    public DataDate(int hour, int dayOfMonth, int month, int year) {
        this.dayOfMonth = dayOfMonth;
        this.month = month;
        this.year = year;
        this.hour=hour;

        // Compute dayOfYear
        //        GregorianCalendar cal = getClearedCalendar();
        //        cal.set(year, month - 1, dayOfMonth); // Subtract 1 to convert from 1-based months to 0-based months
        //        dayOfYear = cal.get(GregorianCalendar.DAY_OF_YEAR);
        dayOfYear = LocalDate.of(year, month, dayOfMonth).getDayOfYear();
    }

    /**
     * Gets a new DataDate representing today's date.
     */
    public static DataDate today() {
        return new DataDate(LocalDate.now());
    }

    /**
     * Constructs a DataDate from a compact string (as returned by toCompactString()).
     */
    public static DataDate fromCompactString(String str) {
        final String PARSE_ERROR_MESSAGE = "Failed to parse compact data date string";

        final String[] parts = str.split("-");
        if (parts.length != 4) {
            throw new IllegalArgumentException(PARSE_ERROR_MESSAGE);
        }

        final int year, month, day,hour;
        try {
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
            day = Integer.parseInt(parts[2]);
            hour=Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(PARSE_ERROR_MESSAGE);
        }

        return new DataDate(hour,day, month, year);
    }

    /**
     * Gets this instance's day (of the month).
     */
    public int getDay() {
        return dayOfMonth;
    }

    /**
     * Gets this instance's month. January is month 1.
     */
    public int getMonth() {
        return month;
    }

    /**
     * Gets this instance's year. This is the full year, not truncated.
     */
    public int getYear() {
        return year;
    }

    /**
     * Gets this instance's day-of-year. January 1 is day 1 of the year.
     */
    public int getDayOfYear() {
        return dayOfYear;
    }

    /**
     * Gets this instance's hour.
     */
    public int getHour() {
        return hour;
    }

    public DataDate next() {
        return next(1);
    }

    public DataDate next(int days) {
        //        final GregorianCalendar cal = getCalendar();
        //        cal.add(GregorianCalendar.DAY_OF_YEAR, days);
        //        return new DataDate(cal);
        return new DataDate(LocalDate.of(year, month, dayOfMonth).plusDays(days));
    }

    public DataDate lastDayOfMonth() {
        //        final GregorianCalendar cal = getCalendar();
        //        // Jump back to the first day of this month
        //        cal.set(GregorianCalendar.DATE, 1);
        //        // Roll around to the last day of this month
        //        cal.roll(GregorianCalendar.DATE, -1);
        //        return new DataDate(cal);
        return new DataDate(LocalDate.of(year, month, LocalDate.of(year, month, dayOfMonth).lengthOfMonth()));
    }

    /**
     * Returns the lesser of two DataDates, as defined by compareTo().
     */
    public static DataDate max(DataDate a, DataDate b) {
        if (a.compareTo(b) > 0) {
            return a;
        } else {
            return b;
        }
    }

    @Override
    public int compareTo(DataDate o) {
        // First, order by year
        if (year < o.year) {
            return -1;
        } else if (year > o.year) {
            return 1;
        }

        // Second, order by dayOfYear
        if (dayOfYear < o.dayOfYear) {
            return -1;
        } else if (dayOfYear > o.dayOfYear) {
            return 1;
        }

        // The objects are equal
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataDate) {
            return equals((DataDate)obj);
        } else {
            return false;
        }
    }

    public boolean equals(DataDate date) {
        return year == date.year && dayOfYear == date.dayOfYear;
    }

    @Override
    public int hashCode() {
        int hash = year;
        hash = hash * 17 + dayOfYear;
        return hash;
    }

    @Override
    public String toString() {
        return new StringBuilder()
        .append("{year: ").append(year)
        .append(", month: ").append(month)
        .append(", day: ").append(dayOfMonth)
        .append(", dayOfYear: ").append(dayOfYear)
        .append("}").toString();
    }


    public String toCompactString() {
        return String.format("%04d-%02d-%02d-%02d", year, month, dayOfMonth, hour);
    }
}