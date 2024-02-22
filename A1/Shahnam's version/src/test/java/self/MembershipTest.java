package self;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MembershipTest {
    @Test
    void constructorTest() {
        Date date1 = new Date(10, 10, 1399);
        Date date2 = new Date(10, 10, 1401);
        Period p1 = new Period(date1, date2);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Membership("name", date2, date1)
        );
        Assertions.assertDoesNotThrow(
                () -> new Membership("name", date1, date2)
        );
        Assertions.assertDoesNotThrow(
                () -> new Membership("name", p1)
        );
    }
}
