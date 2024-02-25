package bank;

public class BankAccount {
    private String id;
    private int balance;

    // constructor, getters and setters
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

    public void setId(String id) {
        this.id = id;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public void withdraw(int amount) {
        if (amount > balance) {
            throw new IllegalArgumentException("1 Insufficient balance");
        }
        balance -= amount;
    }

    public void transfer(BankAccount to, int amount) {
        withdraw(amount);
        to.deposit(amount);
    }
}