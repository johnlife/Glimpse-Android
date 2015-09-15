package odesk.johnlife.glimpse.adapter;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.FileHandler;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class ImagesGalleryAdapter extends BaseAdapter implements Constants {

	private Context context;
	private FileHandler fileHandler;
	private final static int CACHE_CAPACITY = 50;
	private final int SMALL_BITMAP_SIZE = 500;

	private static final HashMap<String, Bitmap> hardBitmapCache = new LinkedHashMap<String, Bitmap>(CACHE_CAPACITY, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
			return size() > CACHE_CAPACITY;
		}
	};

	private void putToCache(String key, Bitmap bitmap) {
		if (bitmap != null) {
			synchronized (hardBitmapCache) {
				hardBitmapCache.put(key, bitmap);
			}
		}
	}

	private Bitmap getFromCache(String key) {
		Bitmap bitmap;
		synchronized (hardBitmapCache) {
			bitmap = hardBitmapCache.get(key);
			if (bitmap != null) {
				hardBitmapCache.remove(key);
				hardBitmapCache.put(key, bitmap);
				return bitmap;
			}
		}
		return bitmap;
	}

	public ImagesGalleryAdapter(Context context) {
		this.context = context;
		this.fileHandler = GlimpseApp.getFileHandler();
	}

	@Override
	public int getCount() {
		return fileHandler.getFiles().size();
	}

	@Override
	public Object getItem(int position) {
		return fileHandler.getFiles().get(position);
	}

	@Override
	public long getItemId(int position) {
		return fileHandler.getFiles().get(position).hashCode();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView view = new ImageView(context);
		String path = fileHandler.getFiles().get(position).getPath();
		Bitmap bitmap = getFromCache(path);
		if (null == bitmap) {
			bitmap = resizeToSmall(BitmapFactory.decodeFile(path));
			putToCache(path, bitmap);
		}
		view.setImageBitmap(bitmap);
		int sizeHeight = GlimpseApp.getScreen().getHeight();
		int sizeWidth = GlimpseApp.getScreen().getWidth();
		sizeHeight /= sizeHeight > sizeWidth ? 6 : 3;
		sizeWidth /= 3;
		view.setLayoutParams(new GridView.LayoutParams(sizeWidth, sizeHeight));
		view.setScaleType(ImageView.ScaleType.FIT_XY);
		return view;
	}

	private Bitmap resizeToSmall(Bitmap original) {
		int originalSize = Math.max(original.getWidth(), original.getHeight());
		float scale = 1;
		while (originalSize > SMALL_BITMAP_SIZE) {
			originalSize /= 2;
			scale /= 2;
		}
		Matrix m = new Matrix();
		m.postScale(scale, scale);
		return Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), m, false);
	}
	
}