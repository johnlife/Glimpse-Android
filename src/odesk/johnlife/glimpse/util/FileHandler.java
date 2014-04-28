package odesk.johnlife.glimpse.util;

import java.io.File;
import java.io.IOException;


import odesk.johnlife.glimpse.data.db.DatabaseHelper;
public class FileHandler {

	public void add(File file) {
		DatabaseHelper databaseHelper = DatabaseHelper.getInstance();
		try {
			databaseHelper.toDb(file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	public void delete(File file) {
		DatabaseHelper databaseHelper = DatabaseHelper.getInstance();
		databaseHelper.deleteRow(file);
	}
}
