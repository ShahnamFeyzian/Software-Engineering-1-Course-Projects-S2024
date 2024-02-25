package self;

public class Membership {
    private final String name;
    private final Period period;

    public Membership(String name, Date startDate, Date endDate) {
        this.name = name;
        this.period = new Period(startDate, endDate);
    }

    public Membership(String name, Period period) {
        this.name = name;
        this.period = period;
    }

    public int membershipDays() {
        return period.days();
    }

    public boolean hasOverlap(Membership m) {
        return this.period.hasOverlap(m.period);
    }

    public boolean isName(String name) {
        return this.name.equals(name);
    }
}
