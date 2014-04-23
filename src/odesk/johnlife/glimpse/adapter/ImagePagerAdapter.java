package odesk.johnlife.glimpse.adapter;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.activity.PhotoActivity;
import odesk.johnlife.glimpse.data.db.DatabaseHelper;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class ImagePagerAdapter extends PagerAdapter {

	private DatabaseHelper databaseHelper;
	private PhotoActivity activity;

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
			return 3;
		}	
	}

	@Override
	public Object instantiateItem(ViewGroup collection, int position){
		LayoutInflater inflater = (LayoutInflater) collection.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.image, collection, false);
		ImageView image = (ImageView)view.findViewById(R.id.base);
		Bitmap bitmap = activity.getImageFromDb();
		image.setImageBitmap(bitmap);
		activity.setScaleType(image, bitmap);
		((ViewPager) collection).addView(view, 0);
		return view;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}

}