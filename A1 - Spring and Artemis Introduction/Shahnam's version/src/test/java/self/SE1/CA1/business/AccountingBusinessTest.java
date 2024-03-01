package self.SE1.CA1.business;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;

import self.SE1.CA1.dataaccess.AccountRepository;
import self.SE1.CA1.dto.AccountDto;
import self.SE1.CA1.dto.AccountTestSeed;

@SpringBootTest
public class AccountingBusinessTest {
    
    private static AccountDto[] accArray = AccountTestSeed.getSeed();
    
    private static AccountingBusiness createBusiness() {
        AccountingBusiness business = new AccountingBusiness(new AccountRepository());
        for (AccountDto acc : accArray) {
            business.deposit(acc.accNo, acc.amount);
        }
        return business;
    }

    @Test
    void balanceTest() {
        AccountingBusiness business = createBusiness();
        for (AccountDto acc : accArray) {
            Assertions.assertEquals(acc.amount, business.balance(acc.accNo));
        }

        Assertions.assertThrows(
            NoSuchElementException.class, 
            () -> business.balance("bla bla bal")
        );
    }

    @Test
    void depositTest() {
        AccountingBusiness business = createBusiness();
        for (AccountDto acc : accArray) {
            business.deposit(acc.accNo, acc.amount*2);
            Assertions.assertEquals(acc.amount*3, business.balance(acc.accNo));
        }
    }

    @Test
    void withdrawTest() {
        AccountingBusiness business = createBusiness();
        for (int i=0; i<accArray.length; i++) {
            business.withdraw(accArray[i].accNo, i*10);
            Assertions.assertEquals(accArray[i].amount-i*10, business.balance(accArray[i].accNo));
        }
        
        Assertions.assertThrows(
            NoSuchElementException.class, 
            () -> business.withdraw("bla bla bal", 10)
        );
        Assertions.assertThrows(
            ArithmeticException.class, 
            () -> business.withdraw(accArray[0].accNo, 100000000)
        );
    }
}
