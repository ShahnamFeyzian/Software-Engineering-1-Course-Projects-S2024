package self.SE1.CA1.dto;

import java.util.Objects;

public class AccountDto {
    
    public String accNo;

    public int amount;

    public AccountDto(String accNo, int amount) {
        this.accNo = accNo;
        this.amount = amount;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AccountDto acc = (AccountDto) other;
        return acc.accNo.equals(this.accNo) && acc.amount == this.amount; 
    }

    @Override
    public int hashCode() {
        return Objects.hash(accNo, amount);
    }
}
