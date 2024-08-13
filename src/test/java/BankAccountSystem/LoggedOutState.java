package BankAccountSystem;

/**
 * @author Chao Chen
 */
public class LoggedOutState implements BankAccountState {
    private BankAccount bankAccount;
    private int maxAttempts = 3;

    public LoggedOutState(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    @Override
    public StateInfo login(String password) {
        if (bankAccount.getPassword().equals(password)) {
            bankAccount.setBankAccountState(bankAccount.getLoggedIn());
            return StateInfo.LOGGED_IN;
        } else {
            bankAccount.setPasswordTries(bankAccount.getPasswordTries() + 1);
            if (bankAccount.getPasswordTries() >= maxAttempts) {
                bankAccount.setBankAccountState(bankAccount.getSuspended());
                return StateInfo.SUSPENDED;
            }
            return StateInfo.ERROR;
        }
    }

    @Override
    public StateInfo logout() {
        return StateInfo.ERROR;
    }

    @Override
    public StateInfo unlock(int resetCode) {
        return StateInfo.ERROR;
    }

    @Override
    public StateInfo withdrawMoney(int amount) {
        return StateInfo.ERROR;
    }
}
