package odesk.johnlife.glimpse.adapter;

import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.FileHandler;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

public class ImagesGalleryAdapter extends BaseAdapter {

	private Context context;
	private FileHandler fileHandler;

	public ImagesGalleryAdapter(Context �ontext) {
		this.context = �ontext;
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
		Bitmap bitmap = BitmapFactory.decodeFile(fileHandler.getFiles().get(position).getPath());
		view.setImageBitmap(bitmap);
		view.setLayoutParams(new Gallery.LayoutParams(400, 400));
		view.setScaleType(ImageView.ScaleType.CENTER_CROP);
		return view;
	}
	
}