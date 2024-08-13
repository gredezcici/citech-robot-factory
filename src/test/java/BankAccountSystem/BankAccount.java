package BankAccountSystem;

/**
 * @author Chao Chen
 */
public class BankAccount {
    private BankAccountState loggedIn;
    private BankAccountState loggedOut;
    private BankAccountState suspended;
    private BankAccountState bankAccountState;
    private int cashBalance;
    private String password;
    private int passwordTries;
    private int resetCode;

    public BankAccount(int cashBalance, String password, int resetCode) {
        this.cashBalance = cashBalance;
        this.password = password;
        this.resetCode = resetCode;
        this.loggedIn = new LoggedInState(this);
        this.loggedOut = new LoggedOutState(this);
        this.suspended = new SuspendedState(this);
        this.bankAccountState = this.loggedOut;
    }

    public void setCashBalance(int cashBalance) {
        this.cashBalance = cashBalance;
    }

    public int getCashBalance() {
        return cashBalance;
    }

    public String getPassword() {
        return password;
    }

    public int getPasswordTries() {
        return passwordTries;
    }

    public int getResetCode() {
        return resetCode;
    }

    public void setPasswordTries(int passwordTries) {
        this.passwordTries = passwordTries;
    }

    public void setBankAccountState(BankAccountState bankAccountState) {
        this.bankAccountState = bankAccountState;
    }

    public BankAccountState getLoggedIn() {
        return loggedIn;
    }

    public BankAccountState getLoggedOut() {
        return loggedOut;
    }

    public BankAccountState getSuspended() {
        return suspended;
    }

    public BankAccountState getBankAccountState() {
        return bankAccountState;
    }
}
