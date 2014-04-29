package odesk.johnlife.glimpse.app;

import java.io.File;

import odesk.johnlife.glimpse.data.FileHandler;
import odesk.johnlife.glimpse.util.DeviceScreen;
import android.app.Application;

public class GlimpseApp extends Application{

	private static File picturesDir;
	private static File tempDir;
	private static DeviceScreen screen;
	private static FileHandler fileHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		picturesDir = getExternalFilesDir("pictures");
		picturesDir.mkdirs();
		tempDir = getCacheDir();
		screen = new DeviceScreen(this);
		fileHandler = new FileHandler(this);
	}

	public static File getPicturesDir() {
		return picturesDir;
	}

	public static File getTempDir() {
		return tempDir;
	}

	public static DeviceScreen getScreen() {
		return screen;
	}

	public static FileHandler getFileHandler() {
		return fileHandler;
	}
}
