package banking.model;

import java.io.Serializable;

public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private String pinHash;
    private int balance;

    public Account(String username, String pinHash, int balance) {
        this.username = username;
        this.pinHash = pinHash;
        this.balance = balance;
    }

    public String getUsername() {
        return username;
    }

    public String getPinHash() {
        return pinHash;
    }

    public int getBalance() {
        return balance;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public boolean withdraw(int amount) {
        if (amount > balance) {
            return false;
        }
        balance -= amount;
        return true;
    }
}
