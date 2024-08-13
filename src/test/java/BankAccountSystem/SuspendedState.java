package BankAccountSystem;

/**
 * @author Chao Chen
 */
public class SuspendedState implements BankAccountState {
    private BankAccount bankAccount;

    public SuspendedState(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    @Override
    public StateInfo login(String password) {
        return StateInfo.ERROR;
    }

    @Override
    public StateInfo logout() {
        return StateInfo.ERROR;
    }

    @Override
    public StateInfo unlock(int resetCode) {
        if (bankAccount.getResetCode() == resetCode) {
            bankAccount.setBankAccountState(bankAccount.getLoggedOut());
            return StateInfo.LOGGED_OUT;
        } else {
            return StateInfo.ERROR;
        }
    }

    @Override
    public StateInfo withdrawMoney(int amount) {
        return StateInfo.ERROR;
    }
}
