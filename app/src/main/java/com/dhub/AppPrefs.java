package com.dhub;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class AppPrefs {
    private final SharedPreferences sharedPrefs;

    public AppPrefs(Context context) {
        this.sharedPrefs = context.getSharedPreferences("dhub_terminal_prefs", Context.MODE_PRIVATE);
    }

    public List<ProfileModel> loadProfiles() {
        List<ProfileModel> list = new ArrayList<>();
        Set<String> pkgs = sharedPrefs.getStringSet("saved_packages", new HashSet<>());
        for (String pkg : pkgs) {
            ProfileModel p = new ProfileModel(pkg);
            p.cookie = sharedPrefs.getString("cookie_" + pkg, "");
            p.link = sharedPrefs.getString("link_" + pkg, "");
            list.add(p);
        }
        return list;
    }

    public void saveProfiles(List<ProfileModel> list) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        Set<String> pkgs = new HashSet<>();
        for (ProfileModel p : list) {
            pkgs.add(p.packageName);
            editor.putString("cookie_" + p.packageName, p.cookie);
            editor.putString("link_" + p.packageName, p.link);
        }
        editor.putStringSet("saved_packages", pkgs);
        editor.apply();
    }

    public int getJedaCek() {
        return sharedPrefs.getInt("jeda_cek", 10);
    }

    public int getJedaProfil() {
        return sharedPrefs.getInt("jeda_profil", 5);
    }
}
