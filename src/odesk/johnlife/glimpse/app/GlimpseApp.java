package odesk.johnlife.glimpse.app;

import java.io.File;

import odesk.johnlife.glimpse.util.DeviceScreen;
import android.app.Application;

import com.ubertesters.sdk.LockingMode;
import com.ubertesters.sdk.Ubertesters;

public class GlimpseApp extends Application{

	private static File picturesDir;
	private static DeviceScreen screen;

	@Override
	public void onCreate() {
		super.onCreate();
		Ubertesters.initialize(this, LockingMode.LockingModeDisableUbertestersIfBuildNotExist);
		picturesDir = getExternalFilesDir("pictures");
		picturesDir.mkdirs();
		screen = new DeviceScreen(this);
	}

	public static File getPicturesDir() {
		return picturesDir;
	}

	public static DeviceScreen getScreen() {
		return screen;
	}

}
