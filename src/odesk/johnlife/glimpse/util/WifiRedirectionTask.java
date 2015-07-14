package odesk.johnlife.glimpse.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import odesk.johnlife.glimpse.Constants;
import android.os.AsyncTask;
import android.util.Log;

public class WifiRedirectionTask extends AsyncTask<Object, Object, Boolean> implements Constants{

	@Override
	protected Boolean doInBackground(Object... params) {
		boolean result = false;
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) (new URL(WIFI_REDIRECT_URL).openConnection());
			con.setInstanceFollowRedirects(true);
			con.setUseCaches(false);
			con.connect();
			String location;
			int status = con.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				location = con.getHeaderField("Location");
				if (location != null && location.equals(WIFI_REDIRECT_URL)) {
					result = true;
				}
				Log.d("aaa", status + "");				
			} else {
				location = con.getURL().toString();
				Log.d("aaa", "URL: " + location);
				result = location.contains(GOOGLE);
				if (result) {
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuffer html = new StringBuffer();
					while ((inputLine = in.readLine()) != null) {
						html.append(inputLine);
					}
					in.close();
					String htmlString = html.toString();
					result = !htmlString.toUpperCase(Locale.ENGLISH).contains(REFRESH_STRING);
					Log.d("aaa", "URL Content... \n" + html.toString());
					Log.d("aaa", "Done");
					Log.d("aaa", "redirected url: " + con.getURL().toString());
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			result = false;
			Log.d("aaa", "Exception");
		} catch (IOException e) {
			e.printStackTrace();
			result = false;
			Log.d("aaa", "Exception");
		}
		return result;
	}
}
