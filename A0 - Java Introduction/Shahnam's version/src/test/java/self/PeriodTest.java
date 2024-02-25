package self;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PeriodTest {
    @Test
    void constructorTest() {
        Date date1 = new Date(10, 10, 1399);
        Date date2 = new Date(10, 10, 1401);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Period(date2, date1)
        );
        Assertions.assertDoesNotThrow(
                () -> new Period(date1, date2)
        );
    }

    @Test
    void hasOverlapTest() {
        Date d1 = new Date(10, 10, 1400);
        Date d2 = new Date(10, 10, 1401);
        Date d3 = new Date(10, 10, 1402);
        Date d4 = new Date(10, 10, 1403);

        Period p1 = new Period(d1, d2);
        Period p2 = new Period(d2, d3);
        Period p3 = new Period(d1, d3);
        Period p4 = new Period(d2, d4);
        Period p5 = new Period(d3, d4);

        Assertions.assertTrue(p1.hasOverlap(p2));
        Assertions.assertTrue(p2.hasOverlap(p1));
        Assertions.assertTrue(p3.hasOverlap(p4));
        Assertions.assertTrue(p4.hasOverlap(p3));
        Assertions.assertFalse(p5.hasOverlap(p1));
        Assertions.assertFalse(p1.hasOverlap(p5));
        Assertions.assertTrue(p2.hasOverlap(p2));
    }
}
