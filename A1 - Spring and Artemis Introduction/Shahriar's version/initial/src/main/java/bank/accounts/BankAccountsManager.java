package bank.accounts;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import bank.communication.Codes;
import bank.communication.Messages;
import bank.communication.Router;

@Repository
public class BankAccountsManager {

	private Map<String, BankAccount> accounts = new HashMap<>();

    @Autowired
    private Router router;

	private BankAccount findAccount(String id) {
		return accounts.get(id);
	}

	private void createAccount(BankAccount account) {
		accounts.put(account.getId(), account);
	}

	public void deposit(String id, int amount) {
		BankAccount account = findAccount(id);
		if (account == null) {
			account = new BankAccount(id, 0);
			createAccount(account);
		}

		account.deposit(amount);
        router.send(Messages.getDepositMessage(Codes.SUCCESSFUL));
	}

	public void withdraw(String id, int amount) {
        BankAccount account = findAccount(id);
        if (account == null) {
            throw new IllegalArgumentException(Messages.getUnknownMessage(Codes.UNKNOWN));
        }

        account.withdraw(amount);
        router.send(Messages.getWithdrawMessage(Codes.SUCCESSFUL));
	}

	public void transfer(String fromId, String toId, int amount) {
		BankAccount from = findAccount(fromId);
		BankAccount to = findAccount(toId);
        if (from == null || to == null) {
            throw new IllegalArgumentException(Messages.getUnknownMessage(Codes.UNKNOWN));
        }

        from.transfer(to, amount);
        router.send(Messages.getTransferMessage(Codes.SUCCESSFUL));
	}

	public void getBalance(String id) {
		BankAccount account = findAccount(id);
        if (account == null) {
            throw new IllegalArgumentException(Messages.getUnknownMessage(Codes.UNKNOWN));
        }
        
        router.send(Messages.getBalanceMessage(Codes.SUCCESSFUL, account.getBalance()));
	}

	public void printAccounts() {
        StringBuilder msg = new StringBuilder();
        accounts.forEach((k, v) -> msg.append(k).append(" ").append(v.getBalance()).append("\n"));
        router.send(msg.toString());
    }
}
