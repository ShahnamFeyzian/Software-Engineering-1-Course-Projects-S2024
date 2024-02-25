package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateTest {

    @Test
    void compareToTest() {
        Date date1 = new Date(12, 10, 1400);
        Date date2 = new Date(12, 10, 1403);
        Date date3 = new Date(12, 12, 1400);
        Date date4 = new Date(19, 10, 1400);
        Date date5 = new Date(12, 10, 1400);

        Assertions.assertTrue(0 > date1.compareTo(date2));
        Assertions.assertTrue(0 < date2.compareTo(date1));
        Assertions.assertTrue(0 > date1.compareTo(date3));
        Assertions.assertTrue(0 < date3.compareTo(date1));
        Assertions.assertTrue(0 > date1.compareTo(date4));
        Assertions.assertTrue(0 < date4.compareTo(date1));
        Assertions.assertEquals(0, date1.compareTo(date5));
        Assertions.assertEquals(0, date5.compareTo(date1));
        Assertions.assertEquals(0, date1.compareTo(date1));
    }

    @Test
    void daysBetweenTest() {
        Date date1 = new Date(12, 10, 1400);
        Date date2 = new Date(31, 1, 1403);
        Date date3 = new Date(17, 2, 1226);
        Date date4 = new Date(1, 11, 1381);

        Assertions.assertEquals(838, date1.daysBetween(date2));
        Assertions.assertEquals(64631, date3.daysBetween(date2));
        Assertions.assertEquals(0, date4.daysBetween(date4));
    }
}
