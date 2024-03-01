package self.SE1.CA1.dataaccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.boot.test.context.SpringBootTest;

import self.SE1.CA1.dto.AccountDto;
import self.SE1.CA1.dto.AccountTestSeed;

@SpringBootTest
class AccountRepositoryTest {
    
    private static AccountDto[] accArray = AccountTestSeed.getSeed();

    private static AccountRepository createRepo() {
        AccountRepository repo = new AccountRepository();
        for (AccountDto acc : accArray) {
            repo.create(acc.accNo, acc.amount);
        }
        return repo;
    }

    @Test
    void findTest() {
        AccountRepository repo = createRepo();
        for (int i=0; i<accArray.length; i++) {
            Assertions.assertEquals(i, repo.find(accArray[i].accNo));
        }
        Assertions.assertEquals(-1, repo.find("There is no account with this name"));
    }

    @Test
    void balanceTest() {
        AccountRepository repo = createRepo();
        for (int i=0; i<accArray.length; i++) {
            Assertions.assertEquals(accArray[i].amount, repo.balance(i));
        }
    }

    @Test
    void depositTest() {
        AccountRepository repo = createRepo();
        for (int i=0; i<accArray.length; i++) {
            repo.deposit(i, 10*i);
            Assertions.assertEquals(accArray[i].amount+10*i, repo.balance(i));
        }
    }

    @Test
    void withdraw() {
        AccountRepository repo = createRepo();
        for (int i=0; i<accArray.length; i++) {
            repo.withdraw(i, 10*i);
            Assertions.assertEquals(accArray[i].amount-10*i, repo.balance(i));
        }
        for (int i=0; i<accArray.length; i++) {
            Assertions.assertThrows(ArithmeticException.class, () -> repo.withdraw(0, 10000000));
        }
    }
}
