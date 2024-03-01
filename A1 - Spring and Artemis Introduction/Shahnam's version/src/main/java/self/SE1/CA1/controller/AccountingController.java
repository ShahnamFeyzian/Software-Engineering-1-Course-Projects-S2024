package self.SE1.CA1.controller;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Component;

import self.SE1.CA1.business.AccountingBusiness;
import self.SE1.CA1.communication.ResponseStatus;
import self.SE1.CA1.communication.Sender;

@Component
public class AccountingController extends BaseController {

    private static class Msgs {
        private static final String depoSucc  = "Deposit successful";
        private static final String withSucc  = "Withdraw successful";
        private static final String withBreak = "Insufficient funds";
        private static final String unknown   = "Unknown account number";
        private static final String nomatch   = "There isn't any method with this name";

        private static final String balanceSucc(int amount) {
            return "Balance: " + amount;
        } 
    }

    private final AccountingBusiness accountingBusiness;

    public AccountingController(Sender sender, AccountingBusiness accountingBusiness) {
        super(sender);
        this.accountingBusiness = accountingBusiness;
    }

    public void deposit(String accNo, int amount) {
        try {
            accountingBusiness.deposit(accNo, amount);
            response(ResponseStatus.SUCCESS, Msgs.depoSucc);
        }
        catch (Exception exp) {
            System.out.println(exp.getMessage());
            response(ResponseStatus.NON, exp.getMessage());
        }
    }

    public void withdraw(String accNo, int amount) {
        try {
            accountingBusiness.withdraw(accNo, amount);
            response(ResponseStatus.SUCCESS, Msgs.withSucc);
        }
        catch (NoSuchElementException exp) {
            response(ResponseStatus.NOT_FOUND, Msgs.unknown);
        }
        catch (ArithmeticException exp) {
            response(ResponseStatus.LOGIC_ERROR, Msgs.withBreak);
        }
        catch (Exception exp) {
            System.out.println(exp.getMessage());
            response(ResponseStatus.NON, exp.getMessage());
        }
    }

    public void balance(String accNo) {
        try {
            int amount = accountingBusiness.balance(accNo);
            response(ResponseStatus.SUCCESS, Msgs.balanceSucc(amount));
        }
        catch (NoSuchElementException exp) {
            response(ResponseStatus.NOT_FOUND, Msgs.unknown);
        }
        catch (Exception exp) {
            System.out.println(exp.getMessage());
            response(ResponseStatus.NON, exp.getMessage());
        }
    }

    public void nomatch() {
        System.out.println(Msgs.nomatch);
    }
}
