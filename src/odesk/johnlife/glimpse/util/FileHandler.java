package odesk.johnlife.glimpse.util;

import java.io.File;

import odesk.johnlife.glimpse.data.db.DatabaseHelper;
import android.content.Context;
public class FileHandler {
	
	private DatabaseHelper databaseHelper;
	
	public FileHandler(Context context) {
		databaseHelper = DatabaseHelper.getInstance(context);
	}

	public void add(File file) {
		try {
			//TODO: scale and encode here
			databaseHelper.add(file.getAbsolutePath());
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	public void delete(File file) {
		databaseHelper.delete(file.getAbsolutePath());
		file.delete();
	}
}
