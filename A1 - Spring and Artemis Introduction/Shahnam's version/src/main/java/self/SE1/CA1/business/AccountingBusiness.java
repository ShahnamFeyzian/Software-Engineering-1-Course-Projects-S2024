package self.SE1.CA1.business;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import self.SE1.CA1.dataaccess.AccountRepository;

@Service
public class AccountingBusiness {
    
    private final AccountRepository accRepo;

    public AccountingBusiness(AccountRepository accountRepository) {
        this.accRepo = accountRepository;
    }

    public void deposit(String accNo, int amount) {
        int idx = accRepo.find(accNo);
        
        if (idx < 0) {
            accRepo.create(accNo, amount);
        }
        else {
            accRepo.deposit(idx, amount);
        }
    }

    public void withdraw(String accNo, int amount) {
        int idx = accRepo.find(accNo);

        if (idx < 0) {
            throw new NoSuchElementException();
        }
        else {
            accRepo.withdraw(idx, amount);
        }
    }

    public int balance(String accNo) {
        int idx = accRepo.find(accNo);

        if (idx < 0) {
            throw new NoSuchElementException();
        }
        else {
            return accRepo.balance(idx);            
        }
    }
}
