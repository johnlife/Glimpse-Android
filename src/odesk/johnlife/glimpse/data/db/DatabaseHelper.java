package odesk.johnlife.glimpse.data.db;

import java.io.File;
import java.util.Calendar;

import odesk.johnlife.glimpse.data.PictureData;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "Glimpse.db";
	private static final String TABLE_NAME = "table_photo";
	private static final String COLUMN_ID = "id";
	private static final String COLUMN_PICTURES = "pictures";
	private static final String COLUMN_COUNT = "count";
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
				COLUMN_ID + " integer PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_PICTURES + " text, " +
				COLUMN_COUNT + " integer, " +
				COLUMN_LAST_TIME + " integer);"); 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

	public void toDb(String picturePath) {
		getWritableDatabase().insert(TABLE_NAME, null, createFirstValues(picturePath));	
	}
	 
	private ContentValues createFirstValues(String picturePath) {
	    ContentValues firstValues = new ContentValues();
	    firstValues.put(COLUMN_PICTURES, picturePath);
	    firstValues.put(COLUMN_COUNT, 0);
	    firstValues.put(COLUMN_LAST_TIME, Calendar.getInstance().getTimeInMillis());
	    return firstValues;
	}
	
	public Bitmap fromDb() {
		Cursor c = getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_NAME, null);
		if (c.getCount() == 0) return null;
		int position = getImagePosition(c);
		c.moveToPosition(position);
		File picFile = new File(c.getString(c.getColumnIndex(COLUMN_PICTURES)));
		Bitmap bitmap = PictureData.createPicture(picFile).getBitmap();
		updateData(c);
		c.close();
		return bitmap;		
	}

	private int getImagePosition(Cursor c) {
		//TODO Вернуть картинку с меньшими COUNT и LAST_TIME
		int pos = 0;
		c.moveToFirst();
		long count = c.getLong(c.getColumnIndex(COLUMN_COUNT));
		long lastTime = c.getLong(c.getColumnIndex(COLUMN_LAST_TIME));
		long count2, lastTime2;
		for (int i=1; i<c.getCount(); i++) {
			c.moveToPosition(i);
			count2 = c.getLong(c.getColumnIndex(COLUMN_COUNT));
			lastTime2 = c.getLong(c.getColumnIndex(COLUMN_LAST_TIME));
			if (lastTime2 < lastTime) {
				count = count2;
				lastTime = lastTime2;
				pos = i;
			}
		}
		return pos;
	}
	
	private void updateData(Cursor c) {
		String where = COLUMN_ID + "=" + (c.getPosition()+1);
		getWritableDatabase().update(TABLE_NAME, createUpdatedValues(c), where, null);		
	}
	
	private ContentValues createUpdatedValues(Cursor c) {
	    ContentValues updatedValues = new ContentValues();
	    long count = c.getLong(c.getColumnIndex(COLUMN_COUNT));
	    updatedValues.put(COLUMN_COUNT, count++);
	    updatedValues.put(COLUMN_LAST_TIME, Calendar.getInstance().getTimeInMillis());
	    return updatedValues;
	}
	
}