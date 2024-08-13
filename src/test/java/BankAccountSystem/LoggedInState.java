package BankAccountSystem;

/**
 * @author Chao Chen
 */
public class LoggedInState implements BankAccountState {
    private BankAccount bankAccount;

    public LoggedInState(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    @Override
    public StateInfo login(String password) {
        return StateInfo.ERROR;
    }

    @Override
    public StateInfo logout() {
        bankAccount.setBankAccountState(bankAccount.getLoggedOut());
        return StateInfo.LOGGED_OUT;
    }

    @Override
    public StateInfo unlock(int resetCode) {
        return StateInfo.ERROR;
    }

    @Override
    public StateInfo withdrawMoney(int amount) {
        if (bankAccount.getCashBalance() >= amount) {
            bankAccount.setCashBalance(bankAccount.getCashBalance() - amount);
            return StateInfo.LOGGED_IN;
        }
        return StateInfo.ERROR;
    }
}
