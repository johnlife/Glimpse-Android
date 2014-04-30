package odesk.johnlife.glimpse.app;

import java.io.File;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.data.FileHandler;
import odesk.johnlife.glimpse.util.DeviceScreen;
import android.app.Application;
import android.provider.Settings.Secure;

import com.pushlink.android.PushLink;
import com.pushlink.android.StrategyEnum;

public class GlimpseApp extends Application{

	private static File picturesDir;
	private static File tempDir;
	private static DeviceScreen screen;
	private static FileHandler fileHandler;

	@Override
	public void onCreate() {
		super.onCreate();
		PushLink.start(this, R.drawable.ic_launcher, "uk9ijtcmm32b1guh", 
				Secure.getString(getContentResolver(), Secure.ANDROID_ID));
		PushLink.setCurrentStrategy(StrategyEnum.FRIENDLY_POPUP); 
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
