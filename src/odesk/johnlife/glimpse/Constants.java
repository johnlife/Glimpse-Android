package odesk.johnlife.glimpse;

public interface Constants {
	
	public static final String PREF_WIFI_BSSID = "wifi.bssid.pref";
	public static final String PREF_WIFI_PASSWORD = "wifi.password.pref";
	public static final String PREF_FIRST_LAUNCH = "glimpse.first.launch";
	public static final String PREF_POSITION_NEW_PHOTOS = "glimpse.position.new.photos";

	public static final String SSL_FACTORY = "odesk.johnlife.glimpse.util.AlwaysTrustSSLContextFactory";
	public static final String EMAIL_SERVER = "mx.glimpseframe.com"; //pop.mail.ru
	public static final String EMAIL_SUBJECT = "Someone liked a photo you sent to Glimpse.";
	public static final String EMAIL_TEXT = "In the body is some dummy text that we’ll fill in later + a thumbnail of the liked photo.";
	
	public static final int REFRESH_RATE = 10000;
	public static final int HINT_TIME = 3000;
	public static final int RESCHEDULE_REFRESH_RATE = 8000;
	public static final double IMAGE_SIZE_PERCENTAGE = 0.3;

}
