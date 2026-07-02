package com.dhub;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DHubOverlayService extends Service {

    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;

    private TextView txtTerminal;
    private ScrollView scrollTerminal;
    private EditText inputCommand;
    private Button btnExecute;

    private AppPrefs prefs;
    private List<ProfileModel> profiles = new ArrayList<>();
    private boolean isMonitoring = false;
    private Thread automationThread;
    private SpannableStringBuilder logBuilder = new SpannableStringBuilder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private int currentMenu = 0; // 0=Main, 1=Cookie Wizard, 2=Setting Job, 3=Setting Delay, 4=Performance, 5=Delta Executor
    private int wizardIndex = 0;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new AppPrefs(this);
        startForegroundServiceNotification();
        initFloatingWindow();
        autoDetectPackages();
        printMainMenu();
    }

    private void startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("dhub_core", "DHub Background Worker", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, "dhub_core")
                .setContentTitle("DHub Terminal Engine")
                .setContentText("Floating console worker actively running...")
                .build();
        startForeground(991, notification);
    }

    private void initFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        overlayView = LayoutInflater.from(this).inflate(R.layout.activity_main, null);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                700, 
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.START | Gravity.TOP;
        params.x = 0;
        params.y = 0;

        windowManager.addView(overlayView, params);

        txtTerminal = overlayView.findViewById(R.id.terminal_text);
        scrollTerminal = overlayView.findViewById(R.id.terminal_scroll);
        inputCommand = overlayView.findViewById(R.id.terminal_input);
        btnExecute = overlayView.findViewById(R.id.btn_terminal_enter);

        txtTerminal.setTypeface(Typeface.MONOSPACE);
        txtTerminal.setTextSize(11);

        txtTerminal.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return true;
                }
                return false;
            }
        });

        btnExecute.setOnClickListener(v -> handleConsoleInput());
    }

    private void printMainMenu() {
        currentMenu = 0;
        logBuilder.clear();
        printLn("========= DHUB PREMIUM TERMINAL =========", "#00FFCC");
        printLn("1. Start Auto-Rejoin Daemon & Monitor Launcher", "#E8E8F0");
        printLn("2. Setup Accounts Cookie Wizard (Mass Parser)", "#E8E8F0");
        printLn("3. Adjust Global Configuration (Job/Place ID)", "#E8E8F0");
        printLn("4. Performance Booster Sub-System (RAM/Cache)", "#E8E8F0");
        printLn("5. Delta Script Autoexecute Manager (Inject)", "#E8E8F0");
        printLn("6. Trigger Auto-Grid Windows Alignment", "#FFFF00");
        printLn("=========================================", "#00FFCC");
        printLn("Status Monitor: " + (isMonitoring ? "ACTIVE" : "STANDBY"), isMonitoring ? "#4ADE80" : "#F87171");
        printLn("Ketik opsi menu (1-6) + Tekan ENTER:", "#00FFCC");
        inputCommand.setHint("Ketik menu...");
    }

    private void handleConsoleInput() {
        String input = inputCommand.getText().toString().trim();
        inputCommand.setText("");

        if (currentMenu == 0) {
            switch (input) {
                case "1": toggleAutomationLog(); break;
                case "2": startCookieWizard(); break;
                case "3": startSettingsWizard(); break;
                case "4": showPerformanceMenu(); break;
                case "5": showDeltaManagerMenu(); break;
                case "6": triggerAutoGridAlignment(); break;
                default: printLn("[!] Invalid Option!", "#F87171"); break;
            }
        } else if (currentMenu == 1) {
            processCookieWizardStep(input);
        } else if (currentMenu == 2) {
            prefs.saveJobId(input);
            printLn("[✓] Target ID Locked: " + input, "#4ADE80");
            currentMenu = 3;
            printLn("[>] Enter Delay Frequency (Seconds) [Default: 7]:", "#FFFF00");
        } else if (currentMenu == 3) {
            try {
                int d = Integer.parseInt(input);
                prefs.saveJedaCek(d);
                printLn("[✓] Cycle delay set to " + d + "s", "#4ADE80");
            } catch (Exception e) {
                prefs.saveJedaCek(7);
                printLn("[~] Invalid input. Reset to default 7s", "#7070A0");
            }
            returnToMainMenuDelayed();
        } else if (currentMenu == 4) {
            executePerformanceTask(input);
        } else if (currentMenu == 5) {
            executeDeltaInjectionTask(input);
        }
    }

    private void triggerAutoGridAlignment() {
        printLn("[*] Re-calculating Window Boundaries for Auto-Grid Alignment...", "#FFFF00");
        new Thread(() -> {
            List<String> livePackages = new ArrayList<>();
            for (ProfileModel p : profiles) {
                if (isAppRunning(p.packageName)) {
                    livePackages.add(p.packageName);
                }
            }

            int totalApps = livePackages.size();
            if (totalApps == 0) {
                runOnUiThread(() -> printLn("[!] No Active Roblox instances detected on screen.", "#F87171"));
                return;
            }

            int displayWidth = 1920;  
            int displayHeight = 1080;
            int startXPosition = 710; 
            int availableWidth = displayWidth - startXPosition;

            int columns = (totalApps <= 2) ? 1 : (totalApps <= 4) ? 2 : 3;
            int rows = (int) Math.ceil((double) totalApps / columns);

            int cellWidth = availableWidth / columns;
            int cellHeight = displayHeight / rows;

            for (int i = 0; i < totalApps; i++) {
                String pkg = livePackages.get(i);
                int col = i % columns;
                int row = i / columns;

                int x = startXPosition + (col * cellWidth);
                int y = row * cellHeight;

                String cmd = "wm set-user-rotation 1\n" + 
                             "am stack resize-docked-stack " + x + " " + y + " " + (x + cellWidth) + " " + (y + cellHeight);
                executeShell(cmd);
            }
            runOnUiThread(() -> printLn("[✓] Auto-Grid completed for " + totalApps + " instances.", "#4ADE80"));
        }).start();
        returnToMainMenuDelayed();
    }

    private void startCookieWizard() {
        if (profiles.isEmpty()) {
            printLn("[!] No Roblox clones located.", "#F87171");
            return;
        }
        currentMenu = 1;
        wizardIndex = 0;
        logBuilder.clear();
        printLn("====== COOKE PARSER WIZARD ======", "#7C6FF7");
        askNextAccountCookie();
    }

    private void askNextAccountCookie() {
        if (wizardIndex >= profiles.size()) {
            prefs.saveProfiles(profiles);
            printLn("\n[*] Parsing complete. Initiating internal systems sync...", "#FFFF00");
            executeMassHybridInjection();
            return;
        }
        printLn("\nTarget Modul: " + profiles.get(wizardIndex).packageName, "#00FFCC");
        printLn("Format Input -> username:password:cookie", "#7070A0");
        printLn("Masukkan data akun, lalu ENTER:", "#FFFFFF");
        inputCommand.setHint("username:password:cookie...");
    }

    private void processCookieWizardStep(String input) {
        if (input.contains(":")) {
            String[] parts = input.split(":");
            if (parts.length >= 3) {
                ProfileModel p = profiles.get(wizardIndex);
                p.rawInput = input;
                p.accountName = parts[0];
                StringBuilder token = new StringBuilder();
                for (int i = 2; i < parts.length; i++) {
                    token.append(parts[i]).append(i == parts.length - 1 ? "" : ":");
                }
                p.cookieOnly = token.toString().trim();
                printLn("[+] Parsed account user: " + p.accountName, "#4ADE80");
            }
        } else {
            profiles.get(wizardIndex).cookieOnly = input; 
        }
        wizardIndex++;
        askNextAccountCookie();
    }

    private void executeMassHybridInjection() {
        new Thread(() -> {
            String termuxSqlitePath = "/data/data/com.termux/files/usr/bin/sqlite3";
            String sqliteBinary = new File(termuxSqlitePath).exists() ? termuxSqlitePath : "sqlite3";

            for (ProfileModel p : profiles) {
                if (p.cookieOnly.isEmpty()) continue;

                executeShell("am force-stop " + p.packageName);
                
                String escapedCookie = p.cookieOnly.replace("'", "''");
                long currentMicros = System.currentTimeMillis() * 1000;
                long expiryMicros = currentMicros + (365L * 24 * 60 * 60 * 1000 * 1000);

                String sqlCommand = "PRAGMA journal_mode=DELETE; DELETE FROM cookies WHERE name='.ROBLOSECURITY'; " +
                        "INSERT OR REPLACE INTO cookies (creation_utc, host_key, name, value, path, expires_utc, is_secure, is_httponly, last_access_utc, has_expires, is_persistent, priority, samesite, source_scheme, source_port) " +
                        "VALUES (" + currentMicros + ", '.roblox.com', '.ROBLOSECURITY', '" + escapedCookie + "', '/', " + expiryMicros + ", 1, 1, " + currentMicros + ", 1, 1, 1, -1, 2, 443);";

                String path1 = "/data/data/" + p.packageName + "/app_webview/Default/Cookies";
                String path2 = "/data/data/" + p.packageName + "/app_webview/Cookies";

                String script = "setenforce 0\n" +
                        "TARGET_DB=\"" + path1 + "\"\n" +
                        "[ ! -f \"$TARGET_DB\" ] && TARGET_DB=\"" + path2 + "\"\n" +
                        "if [ -f \"$TARGET_DB\" ]; then\n" +
                        "  " + sqliteBinary + " \"$TARGET_DB\" \"" + sqlCommand + "\"\n" +
                        "  echo \"INJECT_DB_OK\"\n" +
                        "else\n" +
                        "  echo \"NO_DB_FILE\"\n" +
                        "fi\n" +
                        "chown -R $(stat -c '%u:%g' /data/data/" + p.packageName + ") /data/data/" + p.packageName + "\n" +
                        "setenforce 1\n";

                String res = executeShellWithOutput(script);
                if (res.contains("INJECT_DB_OK")) {
                    runOnUiThread(() -> printLn("[✓] Injected successfully (SQLite) -> " + p.packageName, "#4ADE80"));
                } else {
                    String xmlPath = "/data/data/" + p.packageName + "/shared_prefs/com.roblox.client.X.xml";
                    String xmlScript = "mkdir -p /data/data/" + p.packageName + "/shared_prefs\n" +
                            "echo -e \"<?xml version='1.0' encoding='utf-8' standalone='yes'?>\\n<map>\\n<string name=\\\".ROBLOSECURITY\\\"></string>\\n</map>\" > " + xmlPath + "\n" +
                            "sed -i 's|<string name=\".ROBLOSECURITY\"></string>|<string name=\".ROBLOSECURITY\">" + escapedCookie + "</string>|g' " + xmlPath + "\n" +
                            "chown -R $(stat -c '%u:%g' /data/data/" + p.packageName + ") /data/data/" + p.packageName + "\n";
                    executeShell(xmlScript);
                    runOnUiThread(() -> printLn("[✓] Injected successfully (XML Config) -> " + p.packageName, "#4ADE80"));
                }
            }
            runOnUiThread(() -> returnToMainMenuDelayed());
        }).start();
    }

    private void startSettingsWizard() {
        currentMenu = 2;
        logBuilder.clear();
        printLn("===== GLOBAL SETTINGS =====", "#FFFF00");
        printLn("[Current Target ID]: " + prefs.getJobId(), "#E8E8F0");
        printLn("[Current Delay]: " + prefs.getJedaCek() + " Detik", "#E8E8F0");
        printLn("---------------------------", "#7070A0");
        printLn("[>] Masukkan Place ID atau Link/Job ID baru:", "#FFFF00");
        inputCommand.setHint("Masukkan Link / Place ID...");
    }

    private void showPerformanceMenu() {
        currentMenu = 4;
        logBuilder.clear();
        printLn("===== PERFORMANCE SUB-SYSTEM =====", "#FFFF00");
        printLn("1. Clear System & Roblox Package Cache Memory", "#E8E8F0");
        printLn("2. Deep RAM Memory Booster Aggressive Release", "#E8E8F0");
        printLn("3. Kill All Inactive Background Programs", "#E8E8F0");
        printLn("==================================", "#FFFF00");
        printLn("Ketik nomor tugas (1-3):", "#FFFF00");
    }

    private void executePerformanceTask(String task) {
        new Thread(() -> {
            if ("1".equals(task)) {
                runOnUiThread(() -> printLn("[*] Wiping internal cached data folders...", "#FFFF00"));
                for (ProfileModel p : profiles) {
                    executeShell("rm -rf /data/data/" + p.packageName + "/cache/*");
                }
                runOnUiThread(() -> printLn("[✓] Cache completely cleared.", "#4ADE80"));
            } else if ("2".equals(task)) {
                runOnUiThread(() -> printLn("[*] Aggressive memory allocation purge initiated...", "#FFFF00"));
                executeShell("echo 3 > /proc/sys/vm/drop_caches\n" + "am kill-all\n");
                runOnUiThread(() -> printLn("[✓] Kernel level memory released successfully.", "#4ADE80"));
            } else if ("3".equals(task)) {
                runOnUiThread(() -> printLn("[*] Terminating third party dangling components...", "#FFFF00"));
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
                        if (!proc.processName.contains("com.dhub") && !proc.processName.contains("roblox")) {
                            am.killBackgroundProcesses(proc.processName);
                        }
                    }
                }
                runOnUiThread(() -> printLn("[✓] Background optimization finished.", "#4ADE80"));
            }
            runOnUiThread(() -> returnToMainMenuDelayed());
        }).start();
    }

    private void showDeltaManagerMenu() {
        currentMenu = 5;
        logBuilder.clear();
        printLn("===== DELTA AUTOEXECUTE WORKER =====", "#7C6FF7");
        printLn("Format Ketik -> nama_file:isi_script_lua", "#E8E8F0");
        printLn("Contoh -> farm:loadstring(game:HttpGet('...'))()", "#7070A0");
        printLn("Ketik perintah inject script kamu:", "#FFFFFF");
        inputCommand.setHint("nama_file:script_lua...");
    }

    private void executeDeltaInjectionTask(String input) {
        if (!input.contains(":")) {
            printLn("[!] Invalid format configuration.", "#F87171");
            returnToMainMenuDelayed();
            return;
        }
        String[] parts = input.split(":", 2);
        String fileName = parts[0].replaceAll("[^a-zA-Z0-9]", "") + ".lua";
        String luaScript = parts[1];

        new Thread(() -> {
            File deltaDir = new File("/storage/emulated/0/Delta/Autoexecute");
            if (!deltaDir.exists()) deltaDir.mkdirs();

            File scriptFile = new File(deltaDir, fileName);
            try (FileWriter fw = new FileWriter(scriptFile)) {
                fw.write(luaScript);
                runOnUiThread(() -> printLn("[✓] Script '" + fileName + "' injected into Delta directory.", "#4ADE80"));
            } catch (Exception e) {
                runOnUiThread(() -> printLn("[✗] Direct Write failed. Elevating to shell root...", "#F87171"));
                String rootWritter = "mkdir -p /storage/emulated/0/Delta/Autoexecute\n" +
                        "echo '" + luaScript.replace("'", "'\\''") + "' > /storage/emulated/0/Delta/Autoexecute/" + fileName;
                executeShell(rootWritter);
            }
            runOnUiThread(() -> returnToMainMenuDelayed());
        }).start();
    }

    private void toggleAutomationLog() {
        if (isMonitoring) {
            isMonitoring = false;
            printLn("[!] Halting active automation loops...", "#F87171");
            return;
        }
        isMonitoring = true;
        logBuilder.clear();
        printLn("[+] DHUB COMPACT GUARDIAN SERVICE ONLINE", "#4ADE80");
        renderStatusTable();

        automationThread = new Thread(() -> {
            while (isMonitoring) {
                try {
                    int delayInSeconds = prefs.getJedaCek();
                    Thread.sleep(delayInSeconds * 1000L);

                    for (ProfileModel p : profiles) {
                        if (!isMonitoring) break;

                        boolean alive = isAppRunning(p.packageName);
                        if (!alive) {
                            runOnUiThread(() -> printLn("[⚠/CRASH] Dead thread -> " + p.packageName + ". Booting instance...", "#F87171"));
                            executeShell("am force-stop " + p.packageName);
                            Thread.sleep(1000);

                            Intent intent;
                            String job = prefs.getJobId().trim();
                            if (!job.isEmpty()) {
                                if (job.matches("\\d+")) {
                                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("roblox://placeId=" + job));
                                } else {
                                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(job));
                                }
                                intent.setPackage(p.packageName);
                            } else {
                                intent = getPackageManager().getLaunchIntentForPackage(p.packageName);
                            }

                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                runOnUiThread(() -> printLn("[✓] Worker initialized container for: " + p.packageName, "#4ADE80"));
                            }
                            Thread.sleep(5000); 
                        }
                    }
                    runOnUiThread(() -> renderStatusTable());
                } catch (Exception e) {
                    break;
                }
            }
        });
        automationThread.start();
    }

    private void renderStatusTable() {
        printLn("\n+-----------------------------------+---------+", "#00FFCC");
        printLn("| INSTANCE IDENTIFIER               | STATUS  |", "#00FFCC");
        printLn("+-----------------------------------+---------+", "#00FFCC");
        for (ProfileModel p : profiles) {
            boolean active = isAppRunning(p.packageName);
            String row = String.format(Locale.getDefault(), "| %-33s | %-7s |", 
                    p.packageName.length() > 33 ? p.packageName.substring(0, 30) + "..." : p.packageName, 
                    active ? "ONLINE" : "CRASHED");
            printLn(row, active ? "#4ADE80" : "#F87171");
        }
        printLn("+-----------------------------------+---------+", "#00FFCC");
    }

    private void executeShell(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\nexit\n");
            os.flush();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private String executeShellWithOutput(String cmd) {
        StringBuilder out = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            os.writeBytes(cmd + "\nexit\n");
            os.flush();
            String l;
            while ((l = reader.readLine()) != null) out.append(l).append("\n");
            p.waitFor();
        } catch (Exception ignored) {}
        return out.toString();
    }

    private boolean isAppRunning(String pkg) {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return false;
        for (ActivityManager.RunningAppProcessInfo p : processes) {
            if (p.processName != null && p.processName.equals(pkg)) return true;
        }
        return false;
    }

    private void autoDetectPackages() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> packs = pm.getInstalledPackages(0);
        profiles = prefs.loadProfiles();
        for (PackageInfo p : packs) {
            if (p.packageName.toLowerCase().contains("roblox") && !p.packageName.equals("com.roblox.client")) {
                boolean has = profiles.stream().anyMatch(pr -> pr.packageName.equals(p.packageName));
                if (!has) profiles.add(new ProfileModel(p.packageName));
            }
        }
        prefs.saveProfiles(profiles);
    }

    private void printLn(String t, String c) {
        int s = logBuilder.length();
        logBuilder.append(t).append("\n");
        logBuilder.setSpan(new ForegroundColorSpan(Color.parseColor(c)), s, logBuilder.length(), 0);
        txtTerminal.setText(logBuilder);
        scrollTerminal.post(() -> scrollTerminal.fullScroll(View.FOCUS_DOWN));
    }

    private void returnToMainMenuDelayed() {
        mainHandler.postDelayed(this::printMainMenu, 2500);
    }

    private void runOnUiThread(Runnable r) { mainHandler.post(r); }

    @Override
    public void onDestroy() {
        isMonitoring = false;
        if (overlayView != null && windowManager != null) windowManager.removeView(overlayView);
        super.onDestroy();
    }
}
