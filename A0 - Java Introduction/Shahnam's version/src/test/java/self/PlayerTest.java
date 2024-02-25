package self;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

public class PlayerTest {
    @Test
    void addMembershipTest() {
        Date d1 = new Date(1, 1, 1);
        Date d2 = new Date(1, 1, 2);
        Date d3 = new Date(1, 1, 3);
        Date d4 = new Date(1, 1, 4);

        Membership m1 = new Membership("name", new Period(d1, d2));
        Membership m2 = new Membership("name", new Period(d3, d4));
        Membership m3 = new Membership("name", new Period(d1, d3));
        Membership m4 = new Membership("name", new Period(d2, d4));

        Player pl1 = new Player("name", new ArrayList<Membership>());
        Player pl2 = new Player("name", new ArrayList<Membership>());

        Assertions.assertDoesNotThrow(() -> pl1.addMembership(m1));
        Assertions.assertDoesNotThrow(() -> pl1.addMembership(m2));
        Assertions.assertDoesNotThrow(() -> pl2.addMembership(m3));
        Assertions.assertThrows(Exception.class, () -> pl1.addMembership(m3));
        Assertions.assertThrows(Exception.class, () -> pl1.addMembership(m4));
        Assertions.assertThrows(Exception.class, () -> pl2.addMembership(m2));
    }

    @Test
    void totalMembershipDaysTest() throws Exception {
        String[] teams = {"Barca", "Real", "FashaPooye"};
        Player pl = new Player("name", new ArrayList<Membership>());
        int i = 1;

        for(String team: teams)
        {
            for(int j=0; j<2; j++)
            {
                Date d1 = new Date(i, i, i+1400);
                Date d2 = new Date(i+1, i+1, i+1401);
                Membership m = new Membership(team, d1, d2);
                pl.addMembership(m);
                i += 2;
            }
        }

        Assertions.assertEquals(797, pl.totalMembershipDays("Barca"));
        Assertions.assertEquals(795, pl.totalMembershipDays("Real"));
        Assertions.assertEquals(794, pl.totalMembershipDays("FashaPooye"));
    }
}
