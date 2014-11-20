package odesk.johnlife.glimpse.adapter;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.DatabaseHelper;
import odesk.johnlife.glimpse.data.FileHandler;
import odesk.johnlife.glimpse.data.PictureData;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class ImagePagerAdapter extends PagerAdapter {

	private Context context;
	private FileHandler fileHandler;
	private SparseArray<PictureData> pictures;
	private OnClickListener onClickListener;
	
	public ImagePagerAdapter(final Activity activity, DatabaseHelper databaseHelper, OnClickListener onClickListener) {
		super();
		this.context = activity;
		this.onClickListener = onClickListener;
		this.fileHandler = GlimpseApp.getFileHandler();
		final boolean wasEmpty = fileHandler.isEmpty();
		Log.d("Start - adapter", "Filehadler ready "+(wasEmpty ? "and empty" : ", pics: "+fileHandler.size()));
		fileHandler.setObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				if (fileHandler.isEmpty()) {
					activity.startActivity(new Intent(activity, activity.getClass()));
					activity.finish();
				} else {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							notifyDataSetChanged();
						}
					});
				}
			}
		});
		this.pictures = new SparseArray<PictureData>(fileHandler.size());
	}

	@Override
	public int getCount() {
		return fileHandler.size() < 2 ? 1 : Integer.MAX_VALUE;
//		return Math.max(1, fileHandler.size());	
	}
	
	private PictureData getItem(int position) {
		return pictures.get(position);
	}

	@Override
	public Object instantiateItem(ViewGroup pager, int position){
		ImageView image = new ImageView(context);
		View view = image;
		Bitmap bitmap;
		if (fileHandler.isEmpty()) {
			bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.wp1);
			Log.d("Start - adapter", "Created view for startup screen");
		} else {
			PictureData pictureData = fileHandler.getLightest();
			pictureData.viewCreated();
			pictures.put(position, pictureData);
			if (pictureData.createdToday()) {
				FrameLayout frame = new FrameLayout(context);
				frame.addView(image);
				ImageView poster = new ImageView(context);
				poster.setImageResource(R.drawable.new_pane);
				poster.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP|Gravity.RIGHT));
				frame.addView(poster);
				view = frame;
			}
			bitmap = BitmapFactory.decodeFile(pictureData.getPath());
			Log.d("Start - adapter", "Created view for "+pictureData.getPath());
		}
		image.setImageBitmap(bitmap);
		image.setOnClickListener(onClickListener);
		setScaleType(image, bitmap);
		pager.addView(view);
		return view;		
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
		pictures.remove(position);
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
		if (fileHandler.isEmpty()) return;
		int position = pager.getCurrentItem();
		PictureData item = getItem(position);
		if (null == item) return; 
		int deleted = fileHandler.delete(item);
		notifyDataSetChanged();
		pager.setCurrentItem(position-deleted);
	}

	public void setImageShown(int position) {
		PictureData item = getItem(position);
		if (null == item) return; 
		fileHandler.show(item);
	}

}