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
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

public class FileHandler {
	private boolean locked;
	private DatabaseHelper databaseHelper;
	private List<PictureData> files;
	private DataSetObserver datasetObserver;
	
	public FileHandler(Context context) {
		databaseHelper = DatabaseHelper.getInstance(context);
		files = databaseHelper.getPictures();
	}

	public synchronized void add(File file) {
		cleanup(file.length());
		try {
			Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
			if (null == bmp) return; //not an image
			Bitmap scaled = scaleAndRotate(bmp, file);
			String path = new File(GlimpseApp.getPicturesDir(), "pic"+System.currentTimeMillis()+".jpg").getAbsolutePath();
			try {
				OutputStream out = new BufferedOutputStream(new FileOutputStream(path)); 
				scaled.compress(CompressFormat.JPEG, 85, out);
				out.close();
			} catch (IOException e) {
				Log.e(getClass().getName(), "Error writing scaled bitmap", e);
			}
			locked = true;
			PictureData picture = new PictureData(path);
			picture = databaseHelper.addOrUpdate(picture);
			files.add(picture);
			notifyObserver();
			file.delete();
			locked = false;
		} catch (IllegalStateException e) {
//			PushLink.sendAsyncException(e);
		}
	}

	private Bitmap scaleAndRotate(Bitmap bmp, File file) {
		float w = bmp.getWidth();
		float h = bmp.getHeight();
		float s = Math.min(w, h);
		float l = Math.max(w, h);
		float sw = GlimpseApp.getScreen().getSmallestWidth();
		float lw = GlimpseApp.getScreen().getLargestWidth();
		float ratio = Math.min(1f, Math.max(sw/s, lw/l));
		Bitmap scaled = Bitmap.createScaledBitmap(bmp, (int) (ratio*w), (int) (ratio*h), true);
        Bitmap rotated = scaled;
        try {
	        ExifInterface ei = new ExifInterface(file.getAbsolutePath());
	        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
	        Matrix mtx = new Matrix();
	        int scaledW = scaled.getWidth();
	        int scaledH = scaled.getHeight();
	        switch (orientation) {
		        case ExifInterface.ORIENTATION_ROTATE_90:
		            //rotate CCW
		            mtx.preRotate(90);
		            rotated = Bitmap.createBitmap(scaled, 0, 0, scaledW, scaledH, mtx, true);
		            break;
		        case ExifInterface.ORIENTATION_ROTATE_180:
		            //rotate CCW
		            mtx.preRotate(180);
		            rotated = Bitmap.createBitmap(scaled, 0, 0, scaledW, scaledH, mtx, true);
		            break;
		        case ExifInterface.ORIENTATION_ROTATE_270:
		            //rotate CW
		            mtx.preRotate(-90);
		            rotated = Bitmap.createBitmap(scaled, 0, 0, scaledW, scaledH, mtx, true);
		            break;
	        }
        } catch (IOException e) {}
		return rotated;
	}

	public synchronized int delete(PictureData picture) {
		locked = true;
		databaseHelper.delete(picture);
		File file = new File(picture.getPath());
		file.delete();
		int deleted = 0;
		while (files.remove(picture)) {
			deleted++;
		};
		notifyObserver();
		locked = false;
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
		return files == null || files.isEmpty();
	}

	public synchronized int size() {
		if (null == files) return 0;
		return files.size();
	}
	
	public synchronized PictureData getLightest() {
		locked = true;
		PictureData value = Collections.min(files, PictureData.WEIGHT_COMPARATOR);
		locked = false;
		return value;
	}
	
	public synchronized void cleanup(long size) {
		locked = true;
		while (GlimpseApp.getPicturesDir().getUsableSpace() < size) {
			if (!files.isEmpty()) {
				PictureData victim = Collections.min(files, PictureData.TIME_COMPARATOR);
				delete(victim);
			} else {
				break;
			}
		}
		notifyObserver();
		locked = false;
	}

	public boolean isLocked() {
		return locked;
	}
	
}
