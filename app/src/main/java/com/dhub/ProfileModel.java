package com.dhub;

public class ProfileModel {
    public String packageName;
    public String accountName;
    public String rawInput;
    public String cookieOnly;
    public String status;

    public ProfileModel(String packageName) {
        this.packageName = packageName;
        this.accountName = "Unknown Account";
        this.rawInput = "";
        this.cookieOnly = "";
        this.status = "OFFLINE";
    }
}
