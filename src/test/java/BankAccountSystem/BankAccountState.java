package BankAccountSystem;

/**
 * @author Chao Chen
 */
public interface BankAccountState {
    public StateInfo login(String password);
    public StateInfo logout();
    public StateInfo unlock(int resetCode);
    public StateInfo withdrawMoney(int amount);
}
