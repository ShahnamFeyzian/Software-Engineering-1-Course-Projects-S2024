package self.SE1.CA1.dto;

public class AccountTestSeed {
    
    private static AccountDto[] accArray = {
        new AccountDto("Shahnam", 500000),
        new AccountDto("Reza", 23700),
        new AccountDto("Ahmad", 1267001),
        new AccountDto("Ali", 50000),
        new AccountDto("Javad", 22300),
    };

    public static AccountDto[] getSeed() {
        return accArray;
    }
}
