package odesk.johnlife.glimpse.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import odesk.johnlife.glimpse.Constants;
import ru.johnlife.lifetools.reporter.UpmobileExceptionReporter;

public class WifiRedirectionTask extends AsyncTask<Object, Object, Boolean> implements Constants {

	private static final String LOCATION = "Location";

	@Override
	protected Boolean doInBackground(Object... params) {
		boolean result = false;
		Log.d("Redirect", "Connected, checking for redirection");
        try {
            Thread.sleep(1500);
        } catch (Exception e1) {}
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) (new URL(WIFI_REDIRECT_URL).openConnection());
			con.setInstanceFollowRedirects(true);
			con.setUseCaches(false);
			con.setConnectTimeout(CONNECTION_TIMEOUT);
			con.setReadTimeout(CONNECTION_TIMEOUT);
            int status = -1;
			for (int i=0; i<9; i++) {
				try {
					con.connect();
                    status = con.getResponseCode();
                    break;
				} catch (IOException e) {
					try {
						Thread.sleep(1500);
					} catch (Exception e1) {}
				}
			}
			String location;
            if (status == -1) {
                result = false;
            } else if (status != HttpURLConnection.HTTP_OK) {
				location = con.getHeaderField(LOCATION);
				if (location != null && location.contains(GOOGLE)) {
					result = true;
				}
			} else {
				location = con.getURL().toString();
				result = location.contains(GOOGLE);
				if (result) {
					BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
					String inputLine;
					StringBuilder html = new StringBuilder();
					while ((inputLine = in.readLine()) != null) {
						html.append(inputLine);
					}
					in.close();
					String htmlString = html.toString().toLowerCase(Locale.ENGLISH);
//					for (String string : REDIRECTION_STRINGS) {
//						if (htmlString.contains(string)) return false;
//					}
//					return true;
					return htmlString.contains(GOOGLE);
				}
			}
		} catch (IOException e) {
			Log.e("Redirection Task", e.getMessage(), e);
			UpmobileExceptionReporter.logIfAvailable(e);
			result = false;
		}
		return result;
	}
}
