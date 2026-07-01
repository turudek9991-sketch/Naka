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

    public void saveJedaCek(int delaySeconds) {
        sharedPrefs.edit().putInt("jeda_cek", delaySeconds).apply();
    }

    public int getJedaCek() {
        return sharedPrefs.getInt("jeda_cek", 7); // Default diubah menjadi 7 detik sesuai instruksi
    }

    public void saveJobId(String jobId) {
        sharedPrefs.edit().putString("saved_job_id", jobId).apply();
    }

    public String getJobId() {
        return sharedPrefs.getString("saved_job_id", "");
    }
}
