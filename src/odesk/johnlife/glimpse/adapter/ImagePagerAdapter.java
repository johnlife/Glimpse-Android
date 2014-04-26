package odesk.johnlife.glimpse.adapter;

import java.io.File;
import java.io.IOException;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.activity.PhotoActivity;
import odesk.johnlife.glimpse.data.PictureData;
import odesk.johnlife.glimpse.data.db.DatabaseHelper;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImagePagerAdapter extends PagerAdapter {

	private DatabaseHelper databaseHelper;
	private PhotoActivity activity;
	private SparseArray<String> picturePathes = new SparseArray<String>();

	public ImagePagerAdapter(PhotoActivity activity, DatabaseHelper databaseHelper) {
		super();
		this.activity = activity;
		this.databaseHelper = databaseHelper;
	}

	@Override
	public int getCount() {
		if (databaseHelper.getCount() < 2) {
			return 1;
		} else {
			return databaseHelper.getCount();
		}	
	}

	@Override
	public Object instantiateItem(ViewGroup collection, int position){
		LayoutInflater inflater = (LayoutInflater) collection.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.image, collection, false);
		ImageView image = (ImageView)view.findViewById(R.id.base);
		Bitmap bitmap;
		if (activity.isPicturesFolderEmpty()) {
			bitmap = PictureData.createPicture(R.drawable.wp1, activity).getBitmap();
		} else {
			bitmap = getImageFromDb(position);
		}
		image.setImageBitmap(bitmap);
		image.setOnTouchListener(activity.getTouchListener());
		setScaleType(image, bitmap);
		((ViewPager) collection).addView(view, 0);
		return view;		
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
		picturePathes.remove(position);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

  	private Bitmap getImageFromDb(int position) {
		File picFile = databaseHelper.fromDb();
		try {
			picturePathes.put(position, picFile.getCanonicalPath());
		} catch (IOException exp) {
			exp.printStackTrace();
		}
		return picFile != null ? PictureData.createPicture(picFile).getBitmap() : null;
	}

	public String getPicturePath(int position) {
		return picturePathes.get(position);
	}

	private void setScaleType(ImageView imageView, Bitmap bitmap) {
		if (bitmap != null) {
			int height = bitmap.getHeight();
			int width = bitmap.getWidth();
			int currentOrientation = activity.getResources().getConfiguration().orientation;
			if ((height > width && currentOrientation == Configuration.ORIENTATION_PORTRAIT)
					|| (height < width && currentOrientation == Configuration.ORIENTATION_LANDSCAPE)) {
				imageView.setScaleType(ScaleType.CENTER_CROP);
			} else {
				imageView.setScaleType(ScaleType.FIT_CENTER);
			}
		}
	}
}