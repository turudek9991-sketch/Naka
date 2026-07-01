package com.dhub;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView terminalText;
    private ScrollView terminalScroll;
    private EditText terminalInput;
    private Button btnTerminalEnter;
    
    private AppPrefs prefs;
    private List<ProfileModel> profiles;
    private boolean isMonitoring = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpannableStringBuilder terminalBuilder = new SpannableStringBuilder();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    private Runnable monitorRunnable;

    private int currentMenuMode = 0; 
    private int cookieInputIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new AppPrefs(this);
        profiles = prefs.loadProfiles();

        terminalText = findViewById(R.id.terminal_text);
        terminalScroll = findViewById(R.id.terminal_scroll);
        terminalInput = findViewById(R.id.terminal_input);
        btnTerminalEnter = findViewById(R.id.btn_terminal_enter);

        terminalText.setTypeface(Typeface.MONOSPACE);
        terminalText.setTextSize(12);

        autoDetectRobloxClones();
        printMainMenu();

        btnTerminalEnter.setOnClickListener(v -> handleTerminalInput());
    }

    private void printMainMenu() {
        currentMenuMode = 0;
        terminalBuilder.clear();
        printLn("=======================================", "#00FFCC");
        printLn("  ____  _   _ _   _ ____     ____ _     I", "#00FFCC");
        printLn(" |  _ \\| | | | | | | __ )   / ___| |    I", "#00FFCC");
        printLn(" | | | | |_| | | | |  _ \\  | |   | |    I", "#00FFCC");
        printLn(" | |_| |  _  | |_| | |_) | | |___| |___ I", "#00FFCC");
        printLn(" |____/|_| |_|\\___/|____/   \\____|_____|I", "#00FFCC");
        printLn("=======================================", "#00FFCC");
        printLn("VERSION v3.1.0 · HYBRID INJECTOR", "#7070A0");
        printLn("Status Monitor: " + (isMonitoring ? "RUNNING" : "STOPPED"), isMonitoring ? "#4ADE80" : "#F87171");
        printLn("---------------------------------------", "#7070A0");
        printLn("Select Menu Options:", "#FFFF00");
        printLn("1. Start Auto Rejoin (Scan & Monitor Clones)", "#E8E8F0");
        printLn("2. Set Cookie Wizard (Configure token step-by-step)", "#E8E8F0");
        printLn("3. Global Settings (Place ID, Job ID & Custom Delay)", "#E8E8F0");
        printLn("---------------------------------------", "#7070A0");
        printLn("Ketik angka (1-3) lalu klik ENTER di bawah:", "#00FFCC");
        terminalInput.setHint("Ketik perintah...");
    }

    private void handleTerminalInput() {
        String input = terminalInput.getText().toString().trim();
        terminalInput.setText(""); 
        
        if (currentMenuMode == 0) {
            switch (input) {
                case "1":
                    startAutomationLog();
                    break;
                case "2":
                    startCookieWizard();
                    break;
                case "3":
                    startSettingsWizard();
                    break;
                default:
                    printLn("[!] Perintah tidak valid. Pilih 1, 2, atau 3.", "#F87171");
                    break;
            }
        } else if (currentMenuMode == 1) {
            handleCookieWizardInput(input);
        } else if (currentMenuMode == 2) {
            prefs.saveJobId(input);
            printLn("[✓] Target ID disimpan: " + input, "#4ADE80");
            currentMenuMode = 3;
            printLn("\n[>] Masukkan Delay Rejoin (Detik) [Default 7]:", "#FFFF00");
            terminalInput.setHint("Contoh: 7");
        } else if (currentMenuMode == 3) {
            try {
                int delay = Integer.parseInt(input);
                prefs.saveJedaCek(delay);
                printLn("[✓] Delay Rejoin disetel ke: " + delay + " detik", "#4ADE80");
            } catch (NumberFormatException e) {
                printLn("[~] Input tidak valid, menggunakan default 7 detik.", "#7070A0");
                prefs.saveJedaCek(7);
            }
            printLn("\nTekan ENTER untuk kembali ke Menu Utama...", "#00FFCC");
            currentMenuMode = 0;
        }
    }

    private void startCookieWizard() {
        if (profiles.isEmpty()) {
            printLn("[!] Tidak ada aplikasi Roblox terdeteksi!", "#F87171");
            return;
        }
        currentMenuMode = 1;
        cookieInputIndex = 0;
        terminalBuilder.clear();
        printLn("=======================================", "#7C6FF7");
        printLn("      HYBRID COOKIE INJECTION WORKER    ", "#FFFFFF");
        printLn("=======================================", "#7C6FF7");
        askForCookieAtIndex();
    }

    private void askForCookieAtIndex() {
        if (cookieInputIndex >= profiles.size()) {
            prefs.saveProfiles(profiles);
            printLn("\n[✓] Semua cookie tersimpan di memori lokal.", "#4ADE80");
            printLn("[*] Menjalankan Injeksi Multi-Metode (Root)...", "#FFFF00");
            runMassCookieInjection();
            return;
        }

        ProfileModel currentProfile = profiles.get(cookieInputIndex);
        printLn("\n(" + (cookieInputIndex + 1) + "/" + profiles.size() + ") Target App: " + currentProfile.packageName, "#00FFCC");
        printLn("Masukkan Cookie .ROBLOSECURITY, lalu tekan ENTER:", "#E8E8F0");
        terminalInput.setHint("Paste cookie di sini...");
    }

    private void handleCookieWizardInput(String input) {
        profiles.get(cookieInputIndex).cookie = input;
        cookieInputIndex++;
        askForCookieAtIndex();
    }

    private void runMassCookieInjection() {
        new Thread(() -> {
            for (ProfileModel p : profiles) {
                if (p.cookie.isEmpty()) continue;
                
                runOnUiThread(() -> printLn("[>] Menyiapkan penutupan paksa: " + p.packageName, "#FFFF00"));
                executeShellCommand("am force-stop " + p.packageName);
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}

                String escapedCookie = p.cookie.replace("'", "''");
                long currentMicros = System.currentTimeMillis() * 1000;
                long expiryMicros = currentMicros + (365L * 24 * 60 * 60 * 1000 * 1000);
                
                // METODE 1: SQLITE3 ENGINE (Utama)
                String sql = "PRAGMA journal_mode=DELETE; DELETE FROM cookies WHERE name='.ROBLOSECURITY'; " +
                             "INSERT OR REPLACE INTO cookies (creation_utc, host_key, name, value, path, expires_utc, is_secure, is_httponly, last_access_utc, has_expires, is_persistent, priority, samesite, source_scheme, source_port) " +
                             "VALUES (" + currentMicros + ", '.roblox.com', '.ROBLOSECURITY', '" + escapedCookie + "', '/', " + expiryMicros + ", 1, 1, " + currentMicros + ", 1, 1, 1, -1, 2, 443);";
                
                String dbPath1 = "/data/data/" + p.packageName + "/app_webview/Default/Cookies";
                String dbPath2 = "/data/data/" + p.packageName + "/app_webview/Cookies";
                
                // METODE 2: DIRECT XML INJECTION (Cadangan jika sqlite3 tidak terpasang di Cloud Phone)
                String xmlContent = "<?xml version='1.0' encoding='utf-8' standalone='yes'?>\\n<map>\\n    <string name=\\\".ROBLOSECURITY\\\"></string>\\n</map>";
                String xmlPath = "/data/data/" + p.packageName + "/shared_prefs/com.roblox.client.X.xml";

                String cmd = "setenforce 0\n" +
                             "chmod -R 777 /data/data/" + p.packageName + "\n" +
                             "DB=\"" + dbPath1 + "\"\n" +
                             "[ ! -f \"$DB\" ] && DB=\"" + dbPath2 + "\"\n" +
                             "if [ -f \"$DB\" ] && command -v sqlite3 >/dev/null 2>&1; then\n" +
                             "  sqlite3 \"$DB\" \"" + sql + "\"\n" +
                             "  echo \"SQLITE_SUCCESS\"\n" +
                             "else\n" +
                             "  mkdir -p /data/data/" + p.packageName + "/shared_prefs\n" +
                             "  echo -e \"" + xmlContent + "\" > " + xmlPath + "\n" +
                             "  sed -i 's|<string name=\".ROBLOSECURITY\"></string>|<string name=\".ROBLOSECURITY\">" + escapedCookie + "</string>|g' " + xmlPath + "\n" +
                             "  echo \"XML_SUCCESS\"\n" +
                             "fi\n" +
                             "chown -R $(stat -c '%u:%g' /data/data/" + p.packageName + ") /data/data/" + p.packageName + "\n" +
                             "setenforce 1";
                             
                String output = executeShellCommandWithOutput(cmd);
                runOnUiThread(() -> {
                    if (output.contains("SQLITE_SUCCESS")) {
                        printLn("[✓] Injeksi Berhasil (Metode: SQLite3 DB) -> " + p.packageName, "#4ADE80");
                    } else if (output.contains("XML_SUCCESS")) {
                        printLn("[✓] Injeksi Berhasil (Metode: SharedPrefs XML) -> " + p.packageName, "#4ADE80");
                    } else {
                        printLn("[✗] Gagal! Pastikan popup izin ROOT/SU sudah diizinkan di emulator -> " + p.packageName, "#F87171");
                    }
                });
            }
            runOnUiThread(() -> {
                printLn("\n[✓] SELESAI. Tekan ENTER untuk kembali ke menu.", "#00FFCC");
                currentMenuMode = 0;
            });
        }).start();
    }

    private void startSettingsWizard() {
        terminalBuilder.clear();
        printLn("=======================================", "#FFFF00");
        printLn("            GLOBAL SETTINGS            ", "#000000");
        printLn("=======================================", "#FFFF00");
        printLn("[Current Target ID]: " + prefs.getJobId(), "#E8E8F0");
        printLn("[Current Delay]: " + prefs.getJedaCek() + " Detik", "#E8E8F0");
        printLn("---------------------------------------", "#7070A0");
        printLn("[>] Masukkan Place ID atau Link/Job ID baru:", "#FFFF00");
        terminalInput.setHint("Masukkan Link / Place ID...");
        currentMenuMode = 2;
    }

    private void startAutomationLog() {
        if (isMonitoring) {
            stopAutomationLog();
            return;
        }
        isMonitoring = true;
        terminalBuilder.clear();
        printLn("[+] D HUB AUTO-REJOIN ONLINE WORKER ACTIVATED", "#4ADE80");
        printLn("[*] Target URI Scheme: " + prefs.getJobId(), "#7070A0");
        printLn("[*] Checking cycle delay: " + prefs.getJedaCek() + " seconds", "#7070A0");
        renderStatusTable();
        
        int checkInterval = prefs.getJedaCek() * 1000;
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) return;
                
                new Thread(() -> {
                    for (ProfileModel p : profiles) {
                        boolean running = isAppRunning(p.packageName);
                        if (!running) {
                            runOnUiThread(() -> printLn("[" + timeFormat.format(new Date()) + "] ⚠ App crash/disconnect: " + p.packageName, "#F87171"));
                            
                            executeShellCommand("am force-stop " + p.packageName);
                            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                            
                            Intent intent;
                            if (!prefs.getJobId().isEmpty()) {
                                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(prefs.getJobId()));
                                intent.setPackage(p.packageName);
                            } else {
                                intent = getPackageManager().getLaunchIntentForPackage(p.packageName);
                            }
                            
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                runOnUiThread(() -> printLn("[✓] Sent launch intent to: " + p.packageName, "#4ADE80"));
                            }
                            try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
                        }
                    }
                    runOnUiThread(() -> renderStatusTable());
                }).start();
                
                mainHandler.postDelayed(this, checkInterval);
            }
        };
        mainHandler.post(monitorRunnable);
        terminalInput.setHint("Ketik perintah...");
    }

    private void stopAutomationLog() {
        if (!isMonitoring) return;
        isMonitoring = false;
        if (monitorRunnable != null) {
            mainHandler.removeCallbacks(monitorRunnable);
        }
        printLn("\n[-] AUTOMATION WORKER SAFELY TERMINATED", "#F87171");
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        printMainMenu();
    }

    private void autoDetectRobloxClones() {
        List<String> detected = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        
        for (PackageInfo pkg : packages) {
            if (pkg.packageName.toLowerCase().contains("roblox")) {
                detected.add(pkg.packageName);
            }
        }

        profiles = prefs.loadProfiles();
        for (String pkg : detected) {
            boolean exists = profiles.stream().anyMatch(p -> p.packageName.equals(pkg));
            if (!exists) {
                profiles.add(new ProfileModel(pkg));
            }
        }
        prefs.saveProfiles(profiles);
    }

    private void renderStatusTable() {
        printLn("\n+-----------------------------------+---------+", "#00FFCC");
        printLn("| ROBLOX ACTIVE INTERFACE           | STATUS  |", "#00FFCC");
        printLn("+-----------------------------------+---------+", "#00FFCC");
        
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) am.getMemoryInfo(mi);
        long freeMem = mi.availMem / 0x100000L;
        
        for (ProfileModel p : profiles) {
            String shortPkg = p.packageName;
            if (shortPkg.length() > 33) {
                shortPkg = shortPkg.substring(0, 30) + "...";
            } else {
                shortPkg = String.format(Locale.getDefault(), "%-33s", shortPkg);
            }
            
            boolean isRunning = isAppRunning(p.packageName);
            String statusStr = isRunning ? "ONLINE " : "OFFLINE";
            String statusColor = isRunning ? "#4ADE80" : "#F87171";
            
            terminalBuilder.append("| ").append(shortPkg).append(" | ");
            int colorStart = terminalBuilder.length();
            terminalBuilder.append(statusStr);
            terminalBuilder.setSpan(new ForegroundColorSpan(Color.parseColor(statusColor)), colorStart, terminalBuilder.length(), 0);
            terminalBuilder.append(" |\n");
        }
        printLn("+-----------------------------------+---------+", "#00FFCC");
        printLn("Available Hardware Memory: " + freeMem + " MB", "#00FFCC");
        updateTerminalDisplay();
    }

    private boolean executeShellCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        } finally {
            try { if (os != null) os.close(); if (process != null) process.destroy(); } catch (Exception ignored) {}
        }
    }

    private String executeShellCommandWithOutput(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            return "ERROR";
        }
        return output.toString();
    }

    private boolean isAppRunning(String packageName) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return false;
        for (ActivityManager.RunningAppProcessInfo proc : processes) {
            if (proc.processName != null && proc.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void printLn(String text, String colorHex) {
        int start = terminalBuilder.length();
        terminalBuilder.append(text).append("\n");
        terminalBuilder.setSpan(new ForegroundColorSpan(Color.parseColor(colorHex)), start, terminalBuilder.length(), 0);
        updateTerminalDisplay();
    }

    private void updateTerminalDisplay() {
        terminalText.setText(terminalBuilder);
        terminalScroll.post(() -> terminalScroll.fullScroll(View.FOCUS_DOWN));
    }
}
