package bank.communication;

import java.util.HashMap;
import java.util.Map;

public class Messages {

	public static final Map<Codes, String> msg = new HashMap<>();

	Messages() {
		msg.put(Codes.SUCCESSFUL, "successful");
		msg.put(Codes.INSUFFICIENT, "Insufficient");
		msg.put(Codes.UNKNOWN, "Unknown");
	}

	public static String generateMessage(Codes code, String message) {
		switch (code) {
			case SUCCESSFUL:
				return String.valueOf(code) + message + msg.get(code);
			case INSUFFICIENT:
				return String.valueOf(code) + msg.get(code) + message;
			case UNKNOWN:
				return String.valueOf(code) + msg.get(code) + message;
			default:
				return "";
		}
	}

	public static String getDepositMessage(Codes code) {
		return generateMessage(code, "Deposit");
	}

	public static String getWithdrawMessage(Codes code) {
		return generateMessage(code, "Withdraw");
	}

	public static String getTransferMessage(Codes code) {
		return generateMessage(code, "Transfer");
	}

	public static String getBalanceMessage(Codes code, int amount) {
		return generateMessage(code, "Balance" + String.valueOf(amount));
	}

	public static String getInsufficientMessage(Codes code, String info) {
		return generateMessage(code, info);
	}

	public static String getInsufficientMessage(Codes code) {
		return generateMessage(code, "balance");
	}

	public static String getUnknownMessage(Codes code) {
		return generateMessage(code, "Unknown");
	}
}
