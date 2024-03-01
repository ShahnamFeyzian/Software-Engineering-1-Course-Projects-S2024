package self.SE1.CA1.communication;

import org.springframework.stereotype.Component;

import self.SE1.CA1.controller.AccountingController;

@Component
public class Router {

    private final AccountingController accountingController;

    public Router(AccountingController accountingController) {
        this.accountingController = accountingController;
    }

    private static enum Method {
        DEPOSIT,
        WITHDRAW,
        BALANCE,
        NON
    }

    private static Method getMethod(String methodStr) {
        if (methodStr.equals("DEPOSIT"))
            return Method.DEPOSIT;
        if (methodStr.equals("WITHDRAW"))
            return Method.WITHDRAW;
        if (methodStr.equals("BALANCE"))
            return Method.BALANCE;

        return Method.NON;
    }

    public void callProperController(String[] msg) {
        String header = msg[0];
        Method method = getMethod(header);

        switch (method) {
            case Method.DEPOSIT: {
                accountingController.deposit(msg[1], Integer.valueOf(msg[2]));
                break;
            }
            case Method.WITHDRAW: {
                accountingController.withdraw(msg[1], Integer.valueOf(msg[2]));
                break;
            }
            case Method.BALANCE: {
                accountingController.balance(msg[1]);
                break;
            }
            default: {
                accountingController.nomatch();
            }
        }
    }
}
