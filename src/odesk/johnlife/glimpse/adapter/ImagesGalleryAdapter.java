package odesk.johnlife.glimpse.adapter;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.FileHandler;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImagesGalleryAdapter extends BaseAdapter implements Constants {

	private Context context;
	private FileHandler fileHandler;
	private final int SMALL_BITMAP_SIZE = 500;
	private int horizontalSpacing;

	private static final int cacheSize = (int)(Runtime.getRuntime().maxMemory() / 1024) / 8;

	private static LruCache<String, Bitmap> mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
		@Override
		protected int sizeOf(String key, Bitmap bitmap) {
			return bitmap.getByteCount() / 1024;
		}
	};

	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		if (getBitmapFromMemCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		return mMemoryCache.get(key);
	}

	public ImagesGalleryAdapter(Context context) {
		this.context = context;
		this.fileHandler = GlimpseApp.getFileHandler();
		this.horizontalSpacing = dpToPx(16);
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
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ImageView view = new ImageView(context);
		final String path = fileHandler.getFiles().get(position).getPath();
		Bitmap bitmap = getBitmapFromMemCache(path);
		if (null == bitmap) {
			view.setVisibility(view.INVISIBLE);
			new Thread(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = resizeToSmall(BitmapFactory.decodeFile(path));
					addBitmapToMemoryCache(path, bitmap);
					setBitmap(view, bitmap);
				}
			}).start();
		} else {
			view.setImageBitmap(bitmap);
		}
		int sizeWidth = GlimpseApp.getScreen().getWidth();
		sizeWidth = (sizeWidth - horizontalSpacing * 4) / 3;
		view.setLayoutParams(new GridView.LayoutParams(sizeWidth, (int) (sizeWidth/1.5)));
		view.setScaleType(ImageView.ScaleType.CENTER_CROP);
		return view;
	}

	private void setBitmap(final ImageView view, final Bitmap bitmap) {
		((Activity)context).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				view.setImageBitmap(bitmap);
				AlphaAnimation animation = new AlphaAnimation(0, 1);
				animation.setDuration(250);
				animation.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						view.setVisibility(View.VISIBLE);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
				view.startAnimation(animation);
			}
		});
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

	private int dpToPx(int dp) {
		return Math.round(dp * ((float) GlimpseApp.getScreen().getDensityDpi() / (float) DisplayMetrics.DENSITY_DEFAULT));
	}
}