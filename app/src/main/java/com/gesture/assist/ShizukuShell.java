package com.gesture.assist;

import android.util.Log;

import rikka.shizuku.Shizuku;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShizukuShell {
    private static final String TAG = "ShizukuShell";

    public static String runCommand(String cmd) {
        try {
            Process process = Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null);
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            return output.toString();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
