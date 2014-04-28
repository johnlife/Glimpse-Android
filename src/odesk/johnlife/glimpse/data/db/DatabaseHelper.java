package odesk.johnlife.glimpse.data.db;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import odesk.johnlife.glimpse.app.GlimpseApp;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "Glimpse.db";
	private static final String TABLE_NAME = "table_photo";
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_PICTURES = "pictures";
	private static final String COLUMN_COUNT = "count";
	private static final String COLUMN_LOAD_TIME = "load_time";
	private static final String COLUMN_LAST_TIME = "last_time";
	private static final int SCHEMA_VERSION = 1;
	private static DatabaseHelper instance = null;

	private DatabaseHelper(Context context) {
		super(context, DB_NAME, null, SCHEMA_VERSION);
	}

	public static DatabaseHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DatabaseHelper(context.getApplicationContext());
		}
		return instance;
	}

	public static DatabaseHelper getInstance() {
		if (instance == null) {
			throw new IllegalStateException("Instance is not created yet. Call getInstance(Context).");
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + 
				COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_PICTURES + " TEXT, " +
				COLUMN_COUNT + " INTEGER, " +
				COLUMN_LOAD_TIME + " INTEGER, " +
				COLUMN_LAST_TIME + " INTEGER);"); 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

	public void toDb(String picturePath) {
		if (!existsInDatabase(picturePath)) {
			getWritableDatabase().insert(TABLE_NAME, null, createFirstValues(picturePath));	
		}
	}
	
	public void deleteRow(File file) {
		try {
			int deleted = getWritableDatabase().delete(TABLE_NAME, COLUMN_PICTURES + "=?", new String[] { file.getCanonicalPath() });
			System.out.println("DELETED "+deleted+" ROWS"+ file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		file.delete();
	}

	private boolean existsInDatabase(String picturePath) {
		Cursor c = getWritableDatabase().rawQuery(
				"SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_PICTURES + " = '" + picturePath + "'", null);
		if (c.getCount() != 0) {
			return true;
		} else {
			return false;
		}
	}

	private ContentValues createFirstValues(String picturePath) {
		ContentValues firstValues = new ContentValues();
		firstValues.put(COLUMN_PICTURES, picturePath);
		firstValues.put(COLUMN_COUNT, 0);
		firstValues.put(COLUMN_LOAD_TIME, Calendar.getInstance().getTimeInMillis());
		firstValues.put(COLUMN_LAST_TIME, Calendar.getInstance().getTimeInMillis());
		return firstValues;
	}

	public File fromDb() {
		Cursor c = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_NAME, null);
		if (c.getCount() == 0) return null;
		int position = getImagePosition(c);
		c.moveToPosition(position);
		File picFile = new File(c.getString(c.getColumnIndex(COLUMN_PICTURES)));
		updateData(position, c.getLong(c.getColumnIndex(COLUMN_COUNT)));
		c.close();
		return picFile;		
	}

	private int getImagePosition(Cursor c) {
		int position = 0;
		c.moveToFirst();
		long minCount = c.getLong(c.getColumnIndex(COLUMN_COUNT));
		long minLastTime = c.getLong(c.getColumnIndex(COLUMN_LAST_TIME));
		long currentCount, currentLastTime;
		for (int i=1; i<c.getCount(); i++) {
			c.moveToPosition(i);
			currentCount = c.getLong(c.getColumnIndex(COLUMN_COUNT));
			currentLastTime = c.getLong(c.getColumnIndex(COLUMN_LAST_TIME));
			if (currentCount < 10 && currentCount < minCount) {
				minCount = currentCount;
				position = i;
			} else if (currentLastTime < minLastTime) {
				minLastTime = currentLastTime;
				position = i;
			} 
		}
		return position;
	}
	
	private void updateData(int position, long count) {
		String where = COLUMN_ID + "=" + (position+1);
		getWritableDatabase().update(TABLE_NAME, createUpdatedValues(count), where, null);		
	}

	private ContentValues createUpdatedValues(long count) {
		ContentValues updatedValues = new ContentValues();
		updatedValues.put(COLUMN_COUNT, ++count);
		updatedValues.put(COLUMN_LAST_TIME, Calendar.getInstance().getTimeInMillis());
		return updatedValues;
	}

	public boolean isImageLoadedToday(File picFile) {
		try {
			String picturePath = picFile.getCanonicalPath();
			Cursor c = getWritableDatabase().rawQuery("SELECT " + COLUMN_LOAD_TIME + " FROM " + TABLE_NAME + 
													" WHERE " + COLUMN_PICTURES + " = '" + picturePath + "'", null);
			if (c.getCount() != 0) {
				c.moveToFirst();
				Calendar loadTime = Calendar.getInstance();
				loadTime.setTimeInMillis(c.getLong(c.getColumnIndex(COLUMN_LOAD_TIME)));
				Calendar currentTime = Calendar.getInstance();
				if (loadTime.get(Calendar.DAY_OF_MONTH) == currentTime.get(Calendar.DAY_OF_MONTH) &&
						loadTime.get(Calendar.MONTH) == currentTime.get(Calendar.MONTH) &&
						loadTime.get(Calendar.YEAR) == currentTime.get(Calendar.YEAR)) {
					return true;
				}	
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
		return false;
	}

	public int getCount() {
		Cursor c = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_NAME, null);
		return c.getCount();
	}

}