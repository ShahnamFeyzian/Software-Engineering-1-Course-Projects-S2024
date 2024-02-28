package bank;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
public class BankAccountsService {

    private static final String OUTQ = "OUTQ";

    @Autowired
    private JmsTemplate jmsTemplate;

    private Map<String, BankAccount> accounts = new HashMap<>();

    public BankAccount findAccount(String id) {
        return accounts.get(id);
    }

    public void createAccount(BankAccount account) {
        accounts.put(account.getId(), account);
    }

    public void deleteAccount(String id) {
        accounts.remove(id);
    }

    public void deposit(String id, int amount) {
        BankAccount account = findAccount(id);
        if (account == null) {
            account = new BankAccount(id, 0);
            createAccount(account);
        }
        account.deposit(amount);
        send("0 Deposit successful");
    }

    public void withdraw(String id, int amount) {
        try {
            BankAccount account = findAccount(id);
            if (account == null) {
                throw new IllegalArgumentException("2 Unknown account number");
            }
            account.withdraw(amount);
            send("0 Withdrawal successful");
        } catch (IllegalArgumentException e) {
            send(e.getMessage());
        }
    }

    public void transfer(String fromId, String toId, int amount) {
        BankAccount from = findAccount(fromId);
        BankAccount to = findAccount(toId);
        try {
            if (from == null || to == null) {
                throw new IllegalArgumentException("2 Unknown account number");
            }
            from.transfer(to, amount);
            send("0 Transfer successful");
        } catch (IllegalArgumentException e) {
            send(e.getMessage());
        }
    }

    public void getBalance(String id) {
        BankAccount account = findAccount(id);
        try {
            if (account == null) {
                throw new IllegalArgumentException("2 Unknown account number");
            }
            send("0 Balance: " + account.getBalance());
        } catch (IllegalArgumentException e) {
            send(e.getMessage());
        }
    }

    public void printAccounts() {
        accounts.forEach((k, v) -> send(k + " " + v.getBalance()));
    }

    @SuppressWarnings("null")
    public void send(String message) {
        System.out.println("Sending message: " + message);
        jmsTemplate.convertAndSend(OUTQ, message);
    }
}
