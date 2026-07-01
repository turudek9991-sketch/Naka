package com.dhub;

public class ProfileModel {
    public String packageName;
    public String cookie;
    public String link;
    public String status;

    public ProfileModel(String packageName) {
        this.packageName = packageName;
        this.cookie = "";
        this.link = "";
        this.status = "OFFLINE";
    }
}
