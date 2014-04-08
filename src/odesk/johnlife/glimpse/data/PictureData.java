package odesk.johnlife.glimpse.data;

import java.io.File;

import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.util.DeviceScreen;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public abstract class PictureData {
	
	private static final class FilePictureData extends PictureData {
		private File file;

		public FilePictureData(File file) {
			this.file = file;
		}

		@Override
		protected Bitmap createBitmap(Options options) {
			return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
		}
	}

	private static final class ResourcePictureData extends PictureData {
		private int resource;
		private Context context;

		public ResourcePictureData(int resource, Context context) {
			super();
			this.resource = resource;
			this.context = context;
		}
		
		@Override
		protected Bitmap createBitmap(Options options) {
			return BitmapFactory.decodeResource(context.getResources(), resource, options);
		}
	}

	protected Bitmap bitmap = null;
	
	public static PictureData createPicture(int resourceId, Context context) {
		return new ResourcePictureData(resourceId, context);
	}
	
	public static PictureData createPicture(File file) {
		return new FilePictureData(file);
		
	}

	public Bitmap getBitmap() {
		if (null == bitmap) {
			Options options = new Options();
			options.inJustDecodeBounds = true;
			createBitmap(options);
			int w = options.outWidth;
			int h = options.outHeight;
			DeviceScreen screen = GlimpseApp.getScreen();
			int sw = screen.getWidth();
			int sh = screen.getHeight();
			options.inSampleSize = (int) Math.floor(Math.min(w/sw, h/sh));
			options.inJustDecodeBounds = false;
			bitmap = createBitmap(options);
		}
		return bitmap;
	}

	protected abstract Bitmap createBitmap(Options options);
	
}
