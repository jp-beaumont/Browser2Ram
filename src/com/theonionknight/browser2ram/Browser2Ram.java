package com.theonionknight.browser2ram;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import java.io.DataOutputStream;

public class Browser2Ram extends BroadcastReceiver {
    private static final String ACTION_SYNCHRONIZE = "com.theonionknight.browser2ram.ACTION_SYNCHRONIZE";

    /* ****** ****** */

    @Override public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            this.onBootCompleted(context);
        } else if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            this.onShutdown(context);
        } else if (intent.getAction().equals(Browser2Ram.ACTION_SYNCHRONIZE)) {
            this.onSynchronize(context);
        }

        Log.d("Browser2Ram", intent.getAction());
    }

    /* ****** ****** */

    private void onBootCompleted(Context context) {
        try {
            final Process su = Runtime.getRuntime().exec("su");
            final DataOutputStream os = new DataOutputStream(su.getOutputStream());

            final String owners = "stat -c %u.%g /data/local/data/data/com.android.browser";
            final String access = "stat -c %a /data/local/data/data/com.android.browser";
            os.writeBytes("mkdir -p /data/local/data\n");
            os.writeBytes("mount -o bind /data /data/local/data\n");
            os.writeBytes("mount -t tmpfs browser /data/data/com.android.browser\n");
            os.writeBytes("chown $(" + owners + ") /data/data/com.android.browser\n");
            os.writeBytes("chmod $(" + access + ") /data/data/com.android.browser\n");
            os.writeBytes("rm -r /data/local/data/data/com.android.browser/cache/webview*\n");
            os.writeBytes("cp -ra /data/local/data/data/com.android.browser/* /data/data/com.android.browser\n");
        } catch (Exception e) {
            return;
        }

        final AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        final Intent intent = new Intent(Browser2Ram.ACTION_SYNCHRONIZE, null, context, Browser2Ram.class);
        final PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
            AlarmManager.INTERVAL_HALF_DAY, broadcast);
    }

    private static final String update =
        "find /data/data/com.android.browser/ -type f | while read X ; do" +
        "    Y=\"$(echo \"$X\" | sed 's:^/data/data/:/data/local/data/data/:')\" ;" +
        "    if ! [ x\"$(echo \"$X\" | sed 's:/data/data/com.android.browser/cache/.*::')\" = x\"\" ] ; then" +
        "        if ! [ -e \"$Y\" ] || [ $(stat -c '%Y' \"$X\") -gt $(stat -c '%Y' \"$Y\") ] ; then" +
        "            if [ -e \"$(dirname \"$X\")\" ] ; then" +
        "                cp -a \"$X\" \"$Y\" ;" +
        "            else" +
        "                while ! [ x\"$(dirname \"$X\")\" = x\"/data/data/com.android.browser\" ] ; do" +
        "                    X=\"$(dirname \"$X\")\" ;" +
        "                done ;" +
        "                cp -ra \"$X\" \"$(echo \"$X\" | sed 's:^/data/data/:/data/local/data/data/:')\" ;"  +
        "            fi ;" +
        "        fi ;" +
        "    fi ;" +
        "done ;" +
        "cp -a /data/local/data/data/com.android.browser/cache/browser_state.parcel " +
            "/data/data/com.android.browser/cache/browser_state.parcel" +
        "\n";

    private void onShutdown(Context context) {
        try {
            final Process su = Runtime.getRuntime().exec("su");
            final DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes(update);
        } catch (Exception e) {
            return;
        }
    }

    private void onSynchronize(Context context) {
        try {
            final Process su = Runtime.getRuntime().exec("su");
            final DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes(update);
        } catch (Exception e) {
            return;
        }
    }
}
