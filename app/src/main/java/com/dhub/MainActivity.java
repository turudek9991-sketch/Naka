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
    private Button btnTerminalStart, btnTerminalStop, btnTerminalInject;
    
    private AppPrefs prefs;
    private List<ProfileModel> profiles;
    private boolean isMonitoring = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private SpannableStringBuilder terminalBuilder = new SpannableStringBuilder();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    private Runnable monitorRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new AppPrefs(this);
        profiles = prefs.loadProfiles();

        terminalText = findViewById(R.id.terminal_text);
        terminalScroll = findViewById(R.id.terminal_scroll);
        btnTerminalStart = findViewById(R.id.btn_terminal_start);
        btnTerminalStop = findViewById(R.id.btn_terminal_stop);
        btnTerminalInject = findViewById(R.id.btn_terminal_inject);

        terminalText.setTypeface(Typeface.MONOSPACE);
        terminalText.setTextSize(12);

        printHeader();

        btnTerminalInject.setOnClickListener(v -> runCookieInjectionFlow());
        btnTerminalStart.setOnClickListener(v -> startAutomationLog());
        btnTerminalStop.setOnClickListener(v -> stopAutomationLog());
        
        autoDetectRobloxClones();
    }

    private void printHeader() {
        terminalBuilder.clear();
        printLn("=======================================", "#00FFCC");
        printLn("  ____  _   _ _   _ ____     ____ _     I", "#00FFCC");
        printLn(" |  _ \\| | | | | | | __ )   / ___| |    I", "#00FFCC");
        printLn(" | | | | |_| | | | |  _ \\  | |   | |    I", "#00FFCC");
        printLn(" | |_| |  _  | |_| | |_) | | |___| |___ I", "#00FFCC");
        printLn(" |____/|_| |_|\\___/|____/   \\____|_____|I", "#00FFCC");
        printLn("=======================================", "#00FFCC");
        printLn("VERSION v2.0.0 · CLOUD RECOVERY MODE", "#7070A0");
        printLn("System Memory Status: Checking...", "#FFFF00");
        printLn("---------------------------------------", "#7070A0");
        updateTerminalDisplay();
    }

    private void autoDetectRobloxClones() {
        printLn("[*] Scanning Installed Roblox Packages...", "#E8E8F0");
        List<String> detected = new ArrayList<>();
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);
        
        for (PackageInfo pkg : packages) {
            if (pkg.packageName.toLowerCase().contains("roblox")) {
                detected.add(pkg.packageName);
            }
        }

        if (detected.isEmpty()) {
            printLn("[!] No Roblox Packages Found!", "#F87171");
            return;
        }

        profiles = prefs.loadProfiles();
        int addedCount = 0;
        for (String pkg : detected) {
            boolean exists = profiles.stream().anyMatch(p -> p.packageName.equals(pkg));
            if (!exists) {
                profiles.add(new ProfileModel(pkg));
                addedCount++;
            }
        }
        
        prefs.saveProfiles(profiles);
        printLn("[+] Found " + detected.size() + " apps (" + addedCount + " newly integrated).", "#4ADE80");
        renderStatusTable();
    }

    private void renderStatusTable() {
        printLn("\n+-----------------------------------+---------+", "#00FFCC");
        printLn("| ROBLOX PACKAGE TARGET             | STATUS  |", "#00FFCC");
        printLn("+-----------------------------------+---------+", "#00FFCC");
        
        long freeMem = getFreeMemory();
        
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
        printLn("Free Memory: " + freeMem + " MB", "#00FFCC");
        updateTerminalDisplay();
    }

    private void runCookieInjectionFlow() {
        btnTerminalInject.setEnabled(false);
        printLn("\n[*] Initializing Built-in Termux Cookie Injector...", "#FFFF00");
        
        new Thread(() -> {
            for (ProfileModel p : profiles) {
                if (p.cookie.isEmpty()) {
                    runOnUiThread(() -> printLn("[~] Skip " + p.packageName + " (Cookie Empty)", "#7070A0"));
                    continue;
                }
                
                runOnUiThread(() -> printLn("[>] Launching WebView pre-create setup for " + p.packageName, "#FFFF00"));
                executeShellCommand("am start -n " + p.packageName + "/com.roblox.client.Activity");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                
                runOnUiThread(() -> printLn("[>] Force-stopping for database synchronization...", "#FFFF00"));
                executeShellCommand("am force-stop " + p.packageName);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

                String escapedCookie = p.cookie.replace("'", "''");
                long currentMicros = System.currentTimeMillis() * 1000;
                long expiryMicros = currentMicros + (365L * 24 * 60 * 60 * 1000 * 1000);
                
                String sql = "PRAGMA journal_mode=DELETE; DELETE FROM cookies WHERE name='.ROBLOSECURITY'; " +
                             "INSERT OR REPLACE INTO cookies (creation_utc, host_key, name, value, path, expires_utc, is_secure, is_httponly, last_access_utc, has_expires, is_persistent, priority, samesite, source_scheme, source_port) " +
                             "VALUES (" + currentMicros + ", '.roblox.com', '.ROBLOSECURITY', '" + escapedCookie + "', '/', " + expiryMicros + ", 1, 1, " + currentMicros + ", 1, 1, 1, -1, 2, 443);";
                
                String dbPath1 = "/data/data/" + p.packageName + "/app_webview/Default/Cookies";
                String dbPath2 = "/data/data/" + p.packageName + "/app_webview/Cookies";
                
                String cmd = "setenforce 0\n" +
                             "DB=\"" + dbPath1 + "\"\n" +
                             "[ ! -f \"$DB\" ] && DB=\"" + dbPath2 + "\"\n" +
                             "[ -f \"$DB-wal\" ] && true > \"$DB-wal\"\n" +
                             "[ -f \"$DB-shm\" ] && true > \"$DB-shm\"\n" +
                             "sqlite3 \"$DB\" \"" + sql + "\"\n" +
                             "chmod 660 \"$DB\"\n" +
                             "chown $(stat -c '%u:%g' /data/data/" + p.packageName + ") \"$DB\"\n" +
                             "setenforce 1";
                             
                boolean success = executeShellCommand(cmd);
                runOnUiThread(() -> {
                    if (success) {
                        printLn("[✓] Cookie injected successfully into: " + p.packageName, "#4ADE80");
                    } else {
                        printLn("[✗] Injection failed for: " + p.packageName + " (Check Root/Sqlite3)", "#F87171");
                    }
                });
            }
            runOnUiThread(() -> {
                btnTerminalInject.setEnabled(true);
                renderStatusTable();
            });
        }).start();
    }

    private void startAutomationLog() {
        if (isMonitoring) return;
        isMonitoring = true;
        printLn("\n[+] AUTO-REJOIN AUTOMATION STARTED", "#4ADE80");
        
        int jedaCek = prefs.getJedaCek() * 1000;
        monitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) return;
                
                new Thread(() -> {
                    for (ProfileModel p : profiles) {
                        boolean running = isAppRunning(p.packageName);
                        if (!running) {
                            runOnUiThread(() -> printLn("[" + timeFormat.format(new Date()) + "] ⚠ Crash detected on " + p.packageName + "! Rejoining...", "#FFFF00"));
                            
                            executeShellCommand("am force-stop " + p.packageName);
                            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                            
                            Intent intent = getPackageManager().getLaunchIntentForPackage(p.packageName);
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                runOnUiThread(() -> printLn("[✓] Booted packet launcher: " + p.packageName, "#4ADE80"));
                            }
                            try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
                        }
                    }
                    runOnUiThread(() -> renderStatusTable());
                }).start();
                
                mainHandler.postDelayed(this, jedaCek);
            }
        };
        mainHandler.post(monitorRunnable);
    }

    private void stopAutomationLog() {
        if (!isMonitoring) return;
        isMonitoring = false;
        if (monitorRunnable != null) {
            mainHandler.removeCallbacks(monitorRunnable);
        }
        printLn("\n[-] AUTOMATION MONITOR STOPPED", "#F87171");
        renderStatusTable();
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

    private long getFreeMemory() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) {
            am.getMemoryInfo(mi);
            return mi.availMem / 0x100000L;
        }
        return 0;
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
