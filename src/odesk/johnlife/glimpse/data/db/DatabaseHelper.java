package odesk.johnlife.glimpse.data.db;

import java.util.Calendar;

import odesk.johnlife.glimpse.util.BitmapConverter;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DB_NAME = "Glimpse";
	private static final String TABLE_NAME = "table_photo";
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
		db.execSQL("CREATE TABLE " + TABLE_NAME +
				" (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_PICTURES + " BLOB, " +
				COLUMN_COUNT + " INTEGER, " +
				COLUMN_LAST_TIME + " INTEGER);"); 
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

	public void toDb(Bitmap bitmap) {
		getWritableDatabase().insert(TABLE_NAME, null, createContentValues(bitmap));	
	}
	 
	private ContentValues createContentValues(Bitmap bitmap) {
	    ContentValues cv = new ContentValues();
	    cv.put(COLUMN_PICTURES, BitmapConverter.getBytes(bitmap));
	    cv.put(COLUMN_COUNT, 0);
	    cv.put(COLUMN_LAST_TIME, Calendar.getInstance().getTimeInMillis());
	    return cv;
	}
	
}
