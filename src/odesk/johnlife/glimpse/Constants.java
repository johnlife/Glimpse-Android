package odesk.johnlife.glimpse;

import java.util.Arrays;
import java.util.List;

public interface Constants {

	String ACTION_WIFI_SCAN = "action.wifi.scan";

	String PREF_WIFI_BSSID = "wifi.bssid.pref";
	String PREF_FIRST_LAUNCH = "glimpse.first.launch";
	String PREF_USER_EMAIL = "user.email";

	String SSL_FACTORY = "odesk.johnlife.glimpse.util.AlwaysTrustSSLContextFactory";
	String EMAIL_SERVER = "frame.uzere.name";
//	String EMAIL_SERVER = "mx.glimpseframe.com";

	String EMAIL_POSTFIX = "@testmx.uzere.name";

	String WIFI_REDIRECT_URL = "http://google.com/";
	String GOOGLE = "google";
//	List<String> REDIRECTION_STRINGS = Arrays.asList("authorization", "redirect", "wifi", "wi-fi", "sms", "mobnum", "mobpass");

	String JSON_ERROR = "error";
	String JSON_EMAIL = "name";

	int REFRESH_RATE = 10000;
	int CONNECTION_TIMEOUT = 15000;
	int HINT_TIME = 6000;
	int ERROR_RECOGNIZE_TIME = 10000;
	int SCREEN_PAGE_LIMIT = 2;
	int RESCHEDULE_REFRESH_RATE = 8000;
	double IMAGE_SIZE_PERCENTAGE = 0.3;
	int SKYLIGHT_CODE_LENGTH = 6;

}
