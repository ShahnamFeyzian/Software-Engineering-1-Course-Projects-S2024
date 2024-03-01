package self.SE1.CA1.dataaccess;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

import self.SE1.CA1.dto.AccountDto;

@Service
public class AccountRepository {
    
    private final ArrayList<AccountDto> accounts = new ArrayList<AccountDto>();

    public int find(String accNo) {
        for(int i=0; i<accounts.size(); i++) {
            if (accounts.get(i).accNo.equals(accNo))
                return i;
        }
        return -1;
    }

    public void create(String accNo, int amount) {
        accounts.add(new AccountDto(accNo, amount));
    }

    public void deposit(int idx, int amount) {
        accounts.get(idx).amount += amount;
    }

    public void withdraw(int idx, int amount) {
        AccountDto acc = accounts.get(idx);
        
        if(acc.amount < amount) {
            throw new ArithmeticException();
        }
        else {
            acc.amount -= amount;
        }
    }

    public int balance(int idx) {
        return accounts.get(idx).amount;
    }
}
