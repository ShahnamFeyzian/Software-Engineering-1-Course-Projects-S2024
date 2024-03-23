package bank.accounts;

import bank.communication.Messages;
import bank.communication.Codes;

public class BankAccount {
    private String id;
    private int balance;

    public BankAccount(String id, int balance) {
        this.id = id;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public int getBalance() {
        return balance;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public void withdraw(int amount) {
        if (amount > balance) {
            throw new IllegalArgumentException(Messages.getInsufficientMessage(Codes.INSUFFICIENT));
        }
        balance -= amount;
    }

    public void transfer(BankAccount to, int amount) {
        withdraw(amount);
        to.deposit(amount);
    }
}