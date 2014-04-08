package odesk.johnlife.glimpse.app;

import java.io.File;

import odesk.johnlife.glimpse.util.DeviceScreen;
import android.app.Application;

public class GlimpseApp extends Application{

	private static File picturesDir;
	private static DeviceScreen screen;

	@Override
	public void onCreate() {
		super.onCreate();
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
