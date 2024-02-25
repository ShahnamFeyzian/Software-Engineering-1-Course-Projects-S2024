package bank;

import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.JmsListener;

@SpringBootApplication
public class Bank {

    private static final String INQ = "INQ";

    @Autowired
    private BankAccountService bankAccountService;

    @JmsListener(destination = INQ)
    public void listener(String message) {
        ArrayList<String> messageParts = new ArrayList<>(
            Arrays.asList(message.split(" "))
        );
        switch (messageParts.get(0)) {
            case "DEPOSIT":
                deposit(messageParts);
                break;
            case "WITHDRAW":
                withdraw(messageParts);
                break;
            case "BALANCE":
                balance(messageParts);
                break;
            case "TRANSFER":
                transfer(messageParts);
                break;
            case "DEBUG":
                printAccounts();
                break;
            case "EXIT":
                this.exit();
                break;
            default:
                unknown(messageParts);
                break;
        }
    }

    private void deposit(ArrayList<String> messageParts) {
        String accountNumber = messageParts.get(1);
        int amount = Integer.parseInt(messageParts.get(2));
        bankAccountService.deposit(accountNumber, amount);
    }
    
    private void withdraw(ArrayList<String> messageParts) {
        String accountNumber = messageParts.get(1);
        int amount = Integer.parseInt(messageParts.get(2));
        bankAccountService.withdraw(accountNumber, amount);
    }
    
    private void balance(ArrayList<String> messageParts) {
        String accountNumber = messageParts.get(1);
        bankAccountService.getBalance(accountNumber);
    }
    
    private void transfer(ArrayList<String> messageParts) {
        String fromAccountNumber = messageParts.get(1);
        String toAccountNumber = messageParts.get(2);
        int amount = Integer.parseInt(messageParts.get(2));
        bankAccountService.transfer(fromAccountNumber, toAccountNumber, amount);
    }

    private void printAccounts() {
        bankAccountService.printAccounts();
    }

    private void exit() {
        bankAccountService.send("EXIT");
        System.exit(0);
    }

    private void unknown(ArrayList<String> messageParts) {
        bankAccountService.send("2 Unknown command: " + messageParts.get(0));
    }
}
