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

    // State Machine untuk input interaktif terminal
    private int currentMenuMode = 0; // 0 = Main Menu, 1 = Cookie Input Mode, 2 = Setting Job ID, 3 = Setting Delay
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
        printLn("VERSION v3.0.0 · INTERACTIVE CONSOLE", "#7070A0");
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
        terminalInput.setText(""); // Bersihkan input setelah enter
        
        if (currentMenuMode == 0) {
            // Logika Menu Utama
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
            // Wizard Input Cookie
            handleCookieWizardInput(input);
        } else if (currentMenuMode == 2) {
            // Input Job ID / Place ID
            prefs.saveJobId(input);
            printLn("[✓] Target ID disimpan: " + input, "#4ADE80");
            currentMenuMode = 3;
            printLn("\n[>] Masukkan Delay Rejoin (Detik) [Default 7]:", "#FFFF00");
            terminalInput.setHint("Contoh: 7");
        } else if (currentMenuMode == 3) {
            // Input Delay Time
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

    // ========================================================================
    // WIZARD SET COOKIE (MENU 2)
    // ========================================================================
    private void startCookieWizard() {
        if (profiles.isEmpty()) {
            printLn("[!] Tidak ada aplikasi Roblox terdeteksi untuk disetel!", "#F87171");
            return;
        }
        currentMenuMode = 1;
        cookieInputIndex = 0;
        terminalBuilder.clear();
        printLn("=======================================", "#7C6FF7");
        printLn("      COOKIE CONFIGURATION WIZARD      ", "#FFFFFF");
        printLn("=======================================", "#7C6FF7");
        askForCookieAtIndex();
    }

    private void askForCookieAtIndex() {
        if (cookieInputIndex >= profiles.size()) {
            // Selesai input semua aplikasi
            prefs.saveProfiles(profiles);
            printLn("\n[✓] Semua cookie berhasil direkam di memori internal!", "#4ADE80");
            printLn("[*] Memulai proses injeksi massal ke sistem root WebView...", "#FFFF00");
            runMassCookieInjection();
            return;
        }

        ProfileModel currentProfile = profiles.get(cookieInputIndex);
        printLn("\n(" + (cookieInputIndex + 1) + "/" + profiles.size() + ") Target App: " + currentProfile.packageName, "#00FFCC");
        printLn("Silakan Tempel (Paste) Cookie .ROBLOSECURITY kamu di bawah, lalu tekan ENTER:", "#E8E8F0");
        terminalInput.setHint("Paste cookie di sini...");
    }

    private void handleCookieWizardInput(String input) {
        if (input.isEmpty()) {
            printLn("[~] Cookie dikosongkan untuk package ini.", "#7070A0");
        }
        profiles.get(cookieInputIndex).cookie = input;
        cookieInputIndex++;
        askForCookieAtIndex();
    }

    private void runMassCookieInjection() {
        new Thread(() -> {
            for (ProfileModel p : profiles) {
                if (p.cookie.isEmpty()) continue;
                
                runOnUiThread(() -> printLn("[>] Syncing database WebView untuk: " + p.packageName, "#FFFF00"));
                executeShellCommand("am start -n " + p.packageName + "/com.roblox.client.Activity");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
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
                        printLn("[✓] Success injected -> " + p.packageName, "#4ADE80");
                    } else {
                        printLn("[✗] Failed injected -> " + p.packageName, "#F87171");
                    }
                });
            }
            runOnUiThread(() -> {
                printLn("\n[✓] PROSES SELESAI. Tekan ENTER untuk kembali ke menu utama.", "#00FFCC");
                currentMenuMode = 0;
            });
        }).start();
    }

    // ========================================================================
    // WIZARD CONFIG SETTINGS (MENU 3)
    // ========================================================================
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

    // ========================================================================
    // AUTO REJOIN CORE ENGINE (MENU 1)
    // ========================================================================
    private void startAutomationLog() {
        if (isMonitoring) {
            // Jika ditekan lagi saat running, bertindak sebagai tombol STOP
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
                            runOnUiThread(() -> printLn("[" + timeFormat.format(new Date()) + "] ⚠ App disconnect/crash detected: " + p.packageName, "#F87171"));
                            
                            executeShellCommand("am force-stop " + p.packageName);
                            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                            
                            // Pemicu Rejoin otomatis menggunakan skema link permainan (jika dikonfigurasi)
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
        terminalInput.setHint("Ketik apa saja lalu enter untuk ke menu...");
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

    // ========================================================================
    // HELPER UTILITIES
    // ========================================================================
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
