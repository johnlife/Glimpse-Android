package odesk.johnlife.glimpse.data;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import odesk.johnlife.glimpse.app.GlimpseApp;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.util.Log;
public class FileHandler {
	private final Object lock = new Object();
	
	private DatabaseHelper databaseHelper;
	private List<PictureData> files;
	private DataSetObserver datasetObserver;
	
	public FileHandler(Context context) {
		databaseHelper = DatabaseHelper.getInstance(context);
		synchronized (lock) {
			files = databaseHelper.getPictures();
		}
	}

//	public final void resort() {
//		Collections.sort(files, PictureData.WEIGHT_COMPARATOR);
//		notifyObserver();
//	}
//
	public synchronized void add(File file) {
		try {
			Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
			if (null == bmp) return; //not an image
			//scale 
			float w = bmp.getWidth();
			float h = bmp.getHeight();
			float s = Math.min(w, h);
			float l = Math.max(w, h);
			float sw = GlimpseApp.getScreen().getSmallestWidth();
			float lw = GlimpseApp.getScreen().getLargestWidth();
			float ratio = Math.min(1f, Math.max(sw/s, lw/l));
			Bitmap scaled = Bitmap.createScaledBitmap(bmp, (int) (ratio*w), (int) (ratio*h), true);
			//output
			String path = new File(GlimpseApp.getPicturesDir(), "pic"+System.currentTimeMillis()+".jpg").getAbsolutePath();
			try {
				OutputStream out = new BufferedOutputStream(new FileOutputStream(path)); 
				scaled.compress(CompressFormat.JPEG, 85, out);
				out.close();
			} catch (IOException e) {
				Log.e(getClass().getName(), "Error writing scaled bitmap", e);
			}
			PictureData picture = new PictureData(path);
			picture = databaseHelper.addOrUpdate(picture);
			synchronized (lock) {
				files.add(picture);
				notifyObserver();
			}
			file.delete();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	public synchronized int delete(PictureData picture) {
		databaseHelper.delete(picture);
		File file = new File(picture.getPath());
		file.delete();
		int deleted = 0;
		synchronized (lock) {
			while (files.remove(picture)) {
				deleted++;
			};
			notifyObserver();
		}
		return deleted;
	}
	
	public void show(PictureData picture) {
		picture.shown();
		databaseHelper.addOrUpdate(picture);
	}

	private void notifyObserver() {
		if (null != datasetObserver) {
			datasetObserver.onChanged();
		}
	}

	public void setObserver(DataSetObserver datasetObserver) {
		this.datasetObserver = datasetObserver;
	}
	
	public synchronized boolean isEmpty() {
		synchronized (lock) {
			return files == null || files.isEmpty();
		}
	}

	public synchronized int size() {
		synchronized (lock) {
			if (null == files) return 0;
			return files.size();
		}
	}
	
	public synchronized PictureData getLightest() {
		synchronized (lock) {
			return Collections.min(files, PictureData.WEIGHT_COMPARATOR);
		}
	}
}
