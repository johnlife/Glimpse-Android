package odesk.johnlife.glimpse.util;

import java.util.regex.Pattern;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiConnector {
	private abstract class Connector {
		protected ScanResult net;
		protected String password;
		
		public Connector(ScanResult net, String password) {
			this.net = net;
			this.password = password;
		}

		public abstract WifiConfiguration configure(WifiConfiguration config);
		
		public void connect() {
			WifiConfiguration config = new WifiConfiguration();
			config.SSID = addQuotes(net.SSID);

			config.status = WifiConfiguration.Status.ENABLED;
			config = configure(config);
			
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			
			int res = wifi.addNetwork(config);
			Log.d("wifi", "Network added");
			wifi.disconnect();
			wifi.enableNetwork(res, false);
			wifi.reconnect();
			Log.d("wifi", "Connecting..");
		}
	}
	
	private class WepConnector extends Connector {
		public WepConnector(ScanResult net, String password) {
			super(net, password);
		}

		@Override
		public WifiConfiguration configure(WifiConfiguration config) {
			config.wepKeys[0] = addQuotes(password); 
			config.wepTxKeyIndex = 0;
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40); 
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			return config;
		}
	}

	private class WpaConnector extends Connector {
		public WpaConnector(ScanResult net, String password) {
			super(net, password);
		}

		@Override
		public WifiConfiguration configure(WifiConfiguration config) {
			config.preSharedKey = addQuotes(password);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			
			config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			
			return config;
		}
	}

	
	private class OpenConnector extends Connector {
		public OpenConnector(ScanResult net, String password) {
			super(net, password);
		}

		@Override
		public WifiConfiguration configure(WifiConfiguration config) {
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			return config;
		}
	}

	
	private static final Pattern CAPABILITIES_SPLITTER = Pattern.compile("(?<=[\\]])");

	private WifiManager wifi;

	public WifiConnector(Context context) {
		wifi = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	}
	
	public void connectTo(ScanResult net, String password) {
		String[] capabilities = CAPABILITIES_SPLITTER.split(net.capabilities);
		Connector connector = null;
		for (String cap : capabilities) {
			if (cap.startsWith("[WPA")) {
				connector = new WpaConnector(net, password);
				break;
			} else if (cap.startsWith("[WEP")) {
				connector = new WepConnector(net, password);
				break;
			} 
		}
		if (null == connector) {
			connector = new OpenConnector(net, password);
		}
		connector.connect();
	}

	private static final String addQuotes(String password) {
		return String.format("\"%s\"", password);
	}
}
