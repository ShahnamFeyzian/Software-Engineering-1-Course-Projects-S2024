package self;

public class Period {
    private Date startDate;
    private Date endDate;

    public Period(Date startDate, Date endDate) {
        if(!checkCorrectness(startDate, endDate))
            throw new IllegalArgumentException("start date should be less than end date");

        this.startDate = startDate;
        this.endDate = endDate;
    }

    public boolean hasOverlap(Period p) {
        return (
            this.startDate.compareTo(p.endDate) <= 0 &&
            this.endDate.compareTo(p.startDate) >= 0
        );
    }

    public int days() {
        return startDate.dayDifference(endDate);
    }

    private static boolean checkCorrectness(Date startDate, Date endDate) {
        return (startDate.compareTo(endDate) <= 0);
    }
}
