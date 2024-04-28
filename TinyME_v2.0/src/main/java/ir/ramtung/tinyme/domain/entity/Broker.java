package ir.ramtung.tinyme.domain.entity;

import ir.ramtung.tinyme.domain.exception.NotEnoughCreditException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class Broker {
    @Getter
    @EqualsAndHashCode.Include
    private long brokerId;
    @Getter
    private String name;
    @Getter
    private long credit;

    public void increaseCreditBy(long amount) {
        if (amount < 0)
            throw new IllegalArgumentException("negative amount passed to increaseCreditBy method in Broker class");

        credit += amount;
    }

    public void decreaseCreditBy(long amount) {
        if (amount < 0)
            throw new IllegalArgumentException("negative amount passed to decreaseCreditBy method in Broker class");
        if (!hasEnoughCredit(amount))
            throw new NotEnoughCreditException();

        credit -= amount;
    }

    private boolean hasEnoughCredit(long amount) {
        return credit >= amount;
    }
}
