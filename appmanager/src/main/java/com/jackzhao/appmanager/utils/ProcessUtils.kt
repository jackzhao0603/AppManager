package com.jackzhao.appmanager.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class ProcessUtils {
    private static final String TAG = "ProcessUtils";
    private static volatile String processName = null;

    public static String getProcessName(Context context) {
        if (!TextUtils.isEmpty(processName))
            return processName;
        processName = doGetProcessName(context);
        return processName;
    }

    private static String doGetProcessName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return context.getPackageName();
    }

    public static boolean isMainProcess(Context context) {
        String processName = getProcessName(context);
        String pkgName = context.getPackageName();
        if (!TextUtils.isEmpty(processName) && !TextUtils.equals(processName, pkgName)) {
            return false;
        } else {
            return true;
        }
    }

    public static int execute(String[] cmds, String[] envs, StringBuilder outputBuilder, StringBuilder errorBuilder) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(cmds, envs);
        new StreamConsumer(p.getInputStream(), outputBuilder).start();
        new StreamConsumer(p.getErrorStream(), errorBuilder).start();
        return p.waitFor();
    }

    public static Process execute(String[] cmds, String[] envs, StreamCallback outputCallback, StreamCallback errorCallback) throws IOException {
        Process p = Runtime.getRuntime().exec(cmds, envs);
        new StreamConsumer(p.getInputStream(), outputCallback).start();
        new StreamConsumer(p.getErrorStream(), errorCallback).start();
        return p;
    }

    private static class StreamConsumer extends Thread {
        private InputStream inputStream;
        private StringBuilder stringBuilder;
        private StreamCallback streamCallback;

        public StreamConsumer(InputStream inputStream, StringBuilder stringBuilder) {
            this.inputStream = inputStream;
            this.stringBuilder = stringBuilder;
        }

        public StreamConsumer(InputStream inputStream, StreamCallback streamCallback) {
            this.inputStream = inputStream;
            this.streamCallback = streamCallback;
        }

        @Override
        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    if (streamCallback != null) {
                        streamCallback.onReadLine(line);
                    } else if (stringBuilder != null) {
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                    } else {
                        Log.v(TAG, "StreamConsumer: " + line);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "StreamConsumer error", e);
            }
        }
    }

    public interface StreamCallback {
        void onReadLine(String line);
    }


}
