package odesk.johnlife.glimpse.data;

import java.util.Calendar;
import java.util.Comparator;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PictureData {
	private static final String TABLE_NAME = "table_photo";
	private static final String COLUMN_ID = "_id";
	private static final String COLUMN_PICTURES = "pictures";
	private static final String COLUMN_COUNT = "count";
	private static final String COLUMN_LOAD_TIME = "load_time";
	private static final String COLUMN_LAST_TIME = "last_time";
	private static final int[] calendarFields = {Calendar.DAY_OF_MONTH, Calendar.MONTH, Calendar.YEAR};

	static String getCreationSQL() {
		return "CREATE TABLE " + TABLE_NAME + " (" + 
				COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
				COLUMN_PICTURES + " TEXT, " +
				COLUMN_COUNT + " INTEGER, " +
				COLUMN_LOAD_TIME + " INTEGER, " +
				COLUMN_LAST_TIME + " INTEGER);";
	}

	static String getSelectionSQL() {
		return "SELECT * FROM "+TABLE_NAME;
	}

	public static final Comparator<PictureData> WEIGHT_COMPARATOR = new Comparator<PictureData>() {

		@Override
		public int compare(PictureData lhs, PictureData rhs) {
			return (int) Math.signum(lhs.getWeight() - rhs.getWeight());
		}
	};
	
	private int id = -1;
	private String path;
	private int count;
	private long created;
	private long lastSeen;
	private long weight = -1;
	private long weightCalc = -1;
	
	public PictureData(String path) {
		this.path = path;
		this.created = System.currentTimeMillis();
		this.lastSeen = -1;
		this.count = 0;
	}
	
	public PictureData(Cursor c) {
		id = c.getInt(0);
		path = c.getString(1);
		count = c.getInt(2);
		created = c.getLong(3);
		lastSeen = c.getLong(4);
	}

	void toDb(SQLiteDatabase db) {
		ContentValues cv = new ContentValues();
		cv.put(COLUMN_PICTURES, path);
		cv.put(COLUMN_COUNT, count);
		cv.put(COLUMN_LOAD_TIME, created);
		cv.put(COLUMN_LAST_TIME, lastSeen);
		if (-1 == id) {
			db.insert(TABLE_NAME, null, cv);
		} else {
			cv.put(COLUMN_ID, id);
			db.update(TABLE_NAME, cv, COLUMN_ID+"="+id, null);
		}
	}

	public void delete(SQLiteDatabase db) {
		int deleted = db.delete(TABLE_NAME, COLUMN_PICTURES + "=?", new String[] { path });
		System.out.println("DELETED "+deleted+" ROWS"+ path);
	}

	public boolean isToday() {
		Calendar created = Calendar.getInstance();
		created.setTimeInMillis(this.created);
		Calendar now = Calendar.getInstance();
		boolean value = true;
		for (int field : calendarFields) {
			value &= created.get(field) == now.get(field);
		}
		return value;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof PictureData && id == ((PictureData)o).id;
	}

	private long getWeight() {
		if (-1 == weight || (System.currentTimeMillis() - weightCalc > 5000)) {
			weightCalc = System.currentTimeMillis();
			weight = count*86400000 - (weightCalc-lastSeen);
		}
		return weight;
	}

	public String getPath() {
		return path;
	}

}
