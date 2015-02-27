package odesk.johnlife.glimpse.util;

import java.util.regex.Pattern;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
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
			connectionResult = wifi.addNetwork(config);
			Log.d("wifi", "Network added");
			wifi.disconnect();
			wifi.enableNetwork(connectionResult, false);
			wifi.reconnect();
			Log.d("wifi", "Connecting..");
		}
		
		protected boolean isHexString(String s) {
		    if (s == null) {
		        return false;
		    }
		    int len = s.length();
		    if (len != 10 && len != 26 && len != 58) {
		        return false;
		    }
		    for (int i = 0; i < len; ++i) {
		        char c = s.charAt(i);
		        if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
		            continue;
		        }
		        return false;
		    }
		    return true;
		}
	}
	
	private class WepConnector extends Connector {
		public WepConnector(ScanResult net, String password) {
			super(net, password);
		}

		public void connect() {
			WifiConfiguration config;
			config = new WifiConfiguration();
			config.SSID = addQuotes(net.SSID);
			config.status = WifiConfiguration.Status.DISABLED;
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			config.priority = 40;
			if (isHexString(password)) {
				config.wepKeys[0] = password;
			} else {
				config.wepKeys[0] = "\"".concat(password).concat("\"");
			}
			config.wepTxKeyIndex = 0;
			
			wifi.setWifiEnabled(true);
			connectionResult = wifi.addNetwork(config);
		    wifi.saveConfiguration();
		    wifi.enableNetwork(connectionResult, true);
		}
		
		@Override
		public WifiConfiguration configure(WifiConfiguration config) {		
			return config;
		}
		
	}

	private class WpaConnector extends Connector {
		private ScanResult net;
		private String password;
		
		public WpaConnector(ScanResult net, String password) {
			super(net, password);
			this.net = net;
			this.password = password;
		}
		
		@Override
		public void connect() {
			WifiConfiguration config = new WifiConfiguration();
			config.SSID = addQuotes(net.SSID);
			config.BSSID = net.BSSID;
			config.preSharedKey = addQuotes(password);
			config.status = WifiConfiguration.Status.ENABLED;
			config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			connectionResult = wifi.addNetwork(config);
			int res = connectionResult;
	        wifi.enableNetwork(connectionResult, true);
	        wifi.setWifiEnabled(true);
		}

		@Override
		public WifiConfiguration configure(WifiConfiguration config) {
			return null;
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
	private int connectionResult;

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
	
	public void connectTo(ScanResult net) {
		new OpenConnector(net, "").connect();
	}

	private static final String addQuotes(String password) {
		return String.format("\"%s\"", password);
	}
	
	public int getConnectionResult() {
		return connectionResult;
	}

	public void forgetCurrent() {
		WifiInfo connection = wifi.getConnectionInfo();
		if (null == connection) return; 
 		wifi.disconnect();
		wifi.removeNetwork(connection.getNetworkId());
		wifi.startScan();
	}
}