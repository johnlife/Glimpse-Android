/*** Eclipse Class Decompiler plugin, copyright (c) 2012 Chao Chen (cnfree2000@hotmail.com) ***/
package com.pushlink.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

public final class NinjaStrategy extends Strategy {
	private String hash;

	protected String getHash() {
		return this.hash;
	}

	protected void setHash(String hash) {
		this.hash = hash;
	}

	protected void notifyUser(Intent intent, Context context, int icon, String hash)
  {
    this.hash = hash;

    Process shell = null;
    OutputStream outputStream = null;

    Boolean[] isSuShell = { Boolean.valueOf(false) };

    String[] places = { "/system/xbin/su", "/system/bin/su", "/sbin/su", "su" };
    try
    {
      for (String place : places) {
        try {
          if (shell != null)
            shell.destroy();
          Log.e("PUSHLINK", "NINJA - opening '" + place + "'");
          shell = new ProcessBuilder(new String[] { place }).redirectErrorStream(true).start();
          outputStream = shell.getOutputStream();
          readResponse(shell.getInputStream(), isSuShell);
        }
        catch (Throwable t) {
          Log.e("PUSHLINK", "NINJA - " + t.getMessage());
        }
      }

      Thread.sleep(5000L);

      Log.e("PUSHLINK", "NINJA - calling 'id'");
      outputStream.write("id\n".getBytes());
      outputStream.flush();

      Thread.sleep(2000L);

      Log.e("PUSHLINK", "NINJA - export 'LD_LIBRARY_PATH'");
      outputStream.write("export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/system/lib\n".getBytes());
      outputStream.flush();

      Thread.sleep(2000L);

      if (isSuShell[0].booleanValue()) {
        Log.e("PUSHLINK", "NINJA - calling 'pm/am/exit'");
        File apkFile = context.getFileStreamPath(hash + ".apk");
        String install = "pm install -r " + apkFile.getAbsolutePath();
        String launch = "am start -a android.intent.action.MAIN -n " + getMainLauncherComponentName(context);
        outputStream.write((install + "\n").getBytes());
        outputStream.write((launch + "\n").getBytes());
        outputStream.write("exit\n".getBytes());
        outputStream.flush();
        Thread.sleep(2000L);
        return; }
      label478: Log.e("PUSHLINK", "NINJA - Root access failed (implicitly denied - timeout)!");
    }
    catch (Throwable t)
    {
      Log.e("PUSHLINK", "NINJA - Root access failed (explicitly denied - proccess killed)!", t);
    } finally {
      if (shell != null)
        shell.destroy();
    }
  }

	private void readResponse(final InputStream inputStream, final Boolean[] isSuShell) {
		new Thread(new Runnable() {
			public void run() {
				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(inputStream));
					String line;
					while ((line = reader.readLine()) != null) {
						Log.e("PUSHLINK", "NINJA - " + line);
						if (line.toLowerCase(Locale.US).contains("uid=0"))
							isSuShell[0] = Boolean.valueOf(true);
					}
				} catch (Throwable t) {
					Log.e("PUSHLINK", "NINJA", t);
				}
			}
		}).start();
	}

	protected void unNotifyUser(Context context) {
	}

	protected boolean remind() {
		return true;
	}

	private static String getMainLauncherComponentName(Context context) {
		PackageManager packmngr = context.getPackageManager();
		Intent ints = new Intent("android.intent.action.MAIN", null);
		ints.addCategory("android.intent.category.LAUNCHER");
		List<ResolveInfo> list = packmngr.queryIntentActivities(ints, 0);
		for (ResolveInfo resolveInfo : list)
			if (resolveInfo.activityInfo.packageName.equals(context
					.getPackageName()))
				return resolveInfo.activityInfo.packageName + "/"
						+ resolveInfo.activityInfo.name;
		return null;
	}
}