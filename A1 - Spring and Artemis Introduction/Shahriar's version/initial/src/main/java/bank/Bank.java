package bank;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import bank.accounts.BankAccountsManager;
import bank.communication.Codes;
import bank.communication.Messages;
import bank.communication.Router;

@Repository
public class Bank {

	private final Map<String, Consumer<ArrayList<String>>> commands = new HashMap<>();

	Bank() {
		commands.put("DEPOSIT", this::deposit);
		commands.put("WITHDRAW", this::withdraw);
		commands.put("BALANCE", this::balance);
		commands.put("TRANSFER", this::transfer);
		commands.put("DEBUG", this::printAccounts);
		commands.put("EXIT", this::exit);
		commands.put("UNKNOWN", this::unknown);
	}

	@Autowired
	private BankAccountsManager bankAccountManager;

    @Autowired
    private Router router;

	public void callback(String message) {
        String inputSplitter = " ";
		ArrayList<String> messageParts = new ArrayList<>(
			Arrays.asList(message.split(inputSplitter))
		);
		String command = messageParts.get(0);

        try {
            commands.getOrDefault(command, this::unknown).accept(messageParts);
        } catch (IllegalArgumentException e) {
            router.send(e.getMessage());
        }
	}

	private void deposit(ArrayList<String> messageParts) {
		String accountNumber = messageParts.get(1);
		int amount = Integer.parseInt(messageParts.get(2));

		bankAccountManager.deposit(accountNumber, amount);
	}

	private void withdraw(ArrayList<String> messageParts) {
		String accountNumber = messageParts.get(1);
		int amount = Integer.parseInt(messageParts.get(2));

		bankAccountManager.withdraw(accountNumber, amount);
	}

	private void balance(ArrayList<String> messageParts) {
		String accountNumber = messageParts.get(1);

		bankAccountManager.getBalance(accountNumber);
	}

	private void transfer(ArrayList<String> messageParts) {
		String fromAccountNumber = messageParts.get(1);
		String toAccountNumber = messageParts.get(2);
		int amount = Integer.parseInt(messageParts.get(2));

		bankAccountManager.transfer(fromAccountNumber, toAccountNumber, amount);
	}

	private void printAccounts(ArrayList<String> messageParts) {
		bankAccountManager.printAccounts();
	}

	private void exit(ArrayList<String> messageParts) {
		System.exit(0);
	}

	private void unknown(ArrayList<String> messageParts) {
        router.send(Messages.getUnknownMessage(Codes.UNKNOWN));
	}
}
