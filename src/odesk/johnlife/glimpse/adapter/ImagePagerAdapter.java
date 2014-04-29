package odesk.johnlife.glimpse.adapter;

import java.util.List;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.DatabaseHelper;
import odesk.johnlife.glimpse.data.FileHandler;
import odesk.johnlife.glimpse.data.PictureData;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImagePagerAdapter extends PagerAdapter {

	private Context context;
	private FileHandler fileHandler;
	private List<PictureData> files;
	private OnClickListener onClickListener;
	
	public ImagePagerAdapter(final Activity activity, DatabaseHelper databaseHelper, OnClickListener onClickListener) {
		super();
		this.context = activity;
		this.onClickListener = onClickListener;
		this.fileHandler = GlimpseApp.getFileHandler();
		fileHandler.setObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						notifyDataSetChanged();
					}
				});
			}
		});
		this.files = fileHandler.getFiles();
	}

	@Override
	public int getCount() {
		return Math.max(1, files.size());	
	}
	
	public PictureData getItem(int position) {
		return files.get(position);
	}

	@Override
	public Object instantiateItem(ViewGroup pager, int position){
		ImageView image = new ImageView(context);
		Bitmap bitmap;
		if (files.isEmpty()) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wp1);
		} else {
			bitmap = BitmapFactory.decodeFile(files.get(position).getPath());
		}
		image.setImageBitmap(bitmap);
		image.setOnClickListener(onClickListener);
		setScaleType(image, bitmap);
		pager.addView(image);
		return image;		
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == ((View) object);
	}
	
	@Override
	public int getItemPosition(Object object){
		return PagerAdapter.POSITION_NONE;
	}


	private void setScaleType(ImageView imageView, Bitmap bitmap) {
		if (bitmap != null) {
			int height = bitmap.getHeight();
			int width = bitmap.getWidth();
			int currentOrientation = context.getResources().getConfiguration().orientation;
			if (
				(height > width && currentOrientation == Configuration.ORIENTATION_PORTRAIT) || 
				(height < width && currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
			) {
				imageView.setScaleType(ScaleType.CENTER_CROP);
			} else {
				imageView.setScaleType(ScaleType.FIT_CENTER);
			}
		}
	}
	
	public void deleteCurrentItem(ViewPager pager) {
		if (files.isEmpty()) return;
		int position = pager.getCurrentItem();
		int deleted = fileHandler.delete(getItem(position));
		notifyDataSetChanged();
		pager.setCurrentItem(position-deleted);
	}
}