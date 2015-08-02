package odesk.johnlife.glimpse.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import odesk.johnlife.glimpse.Constants;

public class WifiReceiver implements Constants {

    public enum WifiError { NEED_PASSWORD, CONNECT_ERROR, DISCONNECTED, UNKNOWN_ERROR }

    private abstract class Connector {

        public abstract WifiConfiguration configure(WifiConfiguration config);

        public void connect() {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotes(selectedNetwork.SSID);
            config.status = WifiConfiguration.Status.ENABLED;
            config = configure(config);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            selectedNetworkId = wifi.addNetwork(config);
            wifi.disconnect();
            wifi.enableNetwork(selectedNetworkId, false);
            wifi.reconnect();
        }

        protected final String addQuotes(String password) {
            return String.format("\"%s\"", password);
        }
    }

    private class WepConnector extends Connector {

        public void connect() {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotes(selectedNetwork.SSID);
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
            if (isHexString(selectedNetworkPass)) {
                config.wepKeys[0] = selectedNetworkPass;
            } else {
                config.wepKeys[0] = addQuotes(selectedNetworkPass);
            }
            config.wepTxKeyIndex = 0;
            wifi.setWifiEnabled(true);
            selectedNetworkId = wifi.addNetwork(config);
            wifi.saveConfiguration();
            wifi.enableNetwork(selectedNetworkId, true);
        }

        @Override
        public WifiConfiguration configure(WifiConfiguration config) {
            return config;
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

    private class WpaConnector extends Connector {

        @Override
        public void connect() {
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = addQuotes(selectedNetwork.SSID);
            config.BSSID = selectedNetwork.BSSID;
            config.preSharedKey = addQuotes(selectedNetworkPass);
            config.status = WifiConfiguration.Status.ENABLED;
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            selectedNetworkId = wifi.addNetwork(config);
            wifi.enableNetwork(selectedNetworkId, true);
            wifi.setWifiEnabled(true);
        }

        @Override
        public WifiConfiguration configure(WifiConfiguration config) {
            return null;
        }
    }

    private class OpenConnector extends Connector {

        @Override
        public WifiConfiguration configure(WifiConfiguration config) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            return config;
        }
    }

    private final BroadcastReceiver supplicantStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (state == SupplicantState.DISCONNECTED) {
                    int errorCode = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                    if (errorCode == WifiManager.ERROR_AUTHENTICATING) {
                        prefs.edit().remove(PREF_WIFI_PASSWORD).apply();
                        listener.onDisconnected(WifiError.CONNECT_ERROR);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                if (!isConnecting) listener.onScansResultReceive(wifi.getScanResults());
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.DetailedState details = info.getDetailedState();
                boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
                boolean connecting = info.getState() == NetworkInfo.State.CONNECTING;
                boolean isSuspended = info.getState() == NetworkInfo.State.SUSPENDED;
                boolean unknown = info.getState() == NetworkInfo.State.UNKNOWN;
                if (isSuspended || unknown) {
                    isConnecting = false;
                    listener.onDisconnected(WifiError.UNKNOWN_ERROR);
                } else if (details == NetworkInfo.DetailedState.DISCONNECTED) {
                    isConnecting = false;
                    resetCurrentWifi();
                    prefs.edit().putString(PREF_WIFI_PASSWORD, "").apply();
                    listener.onDisconnected(WifiError.DISCONNECTED);
                } else if (connected && details == NetworkInfo.DetailedState.CONNECTED) {
                    isConnecting = false;
                    listener.onConnected();
                } else if (!connected && !connecting && details != NetworkInfo.DetailedState.SCANNING) {
                    listener.onScanning();
                    scanWifi();
                }

            }
        }
    };

    private int selectedNetworkId;
    private ScanResult selectedNetwork;
    private String selectedNetworkPass;
    private static WifiReceiver instance;
    private WifiManager wifi;
    private Context context;
    private WifiConnectionListener listener;
    private SharedPreferences prefs;
    private boolean isConnecting;

    private WifiReceiver(Context context, WifiConnectionListener listener) {
        this.context = context;
        this.listener = listener;
        registerWifiBroadcast(true);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(true);
        }
        //TODO
//			if (isConnectedOrConnecting()) {
//				wifiList.hide(false);
//			} else {
//				registerScanReciver();
//				scanWifi();
//			}

    }

    public static WifiReceiver createInstance(Context context, WifiConnectionListener listener) {
        instance = new WifiReceiver(context, listener);
        return instance;
    }

    public static WifiReceiver getInstance() {
        return instance;
    }

    public void registerWifiBroadcast(boolean isNeed) {
        try {
            if (isNeed) {
                IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                context.registerReceiver(wifiScanReceiver, new IntentFilter(filter));
                context.registerReceiver(supplicantStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            } else {
                context.unregisterReceiver(wifiScanReceiver);
                context.unregisterReceiver(supplicantStateReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToNetwork(ScanResult selectedNetwork) {
        this.selectedNetwork = selectedNetwork;
        String pass = prefs.getString(PREF_WIFI_PASSWORD, "");
        String bssid = prefs.getString(PREF_WIFI_BSSID, "");
        connectToSelectedNetwork(selectedNetwork.BSSID.equals(bssid) ? pass : "");
    }

    public void connectToSelectedNetwork(String pass) {
        isConnecting = true;
        listener.onConnecting();
        selectedNetworkPass = pass;
        String cap = selectedNetwork.capabilities;
        if (pass.isEmpty() && !(cap.isEmpty() || cap.startsWith("[ESS"))) {
            isConnecting = false;
            listener.onDisconnected(WifiError.NEED_PASSWORD);
        } else {
            if (cap.contains("[WPA")) {
                new WpaConnector().connect();
            } else if (cap.contains("[WEP")) {
                new WepConnector().connect();
            } else {
                new OpenConnector().connect();
            }
            checkConnectionResult();
        }
    }

    private void checkConnectionResult() {
        isConnecting = false;
        if (selectedNetworkId != -1) {
            prefs.edit().putString(PREF_WIFI_BSSID, selectedNetwork.BSSID)
                    .putString(PREF_WIFI_PASSWORD, selectedNetworkPass).apply();
        } else {
            listener.onDisconnected(WifiError.UNKNOWN_ERROR);
            resetCurrentWifi();
            scanWifi();
        }
    }

    public void scanWifi() {
        wifi.startScan();
    }

    public boolean isConnectedOrConnecting() {
        return getNetworkInfo().isConnectedOrConnecting();
    }

    public boolean isConnected() {
        return getNetworkInfo().isConnected();
    }

    public void resetCurrentWifi() {
        WifiInfo connection = wifi.getConnectionInfo();
        if (null == connection) return;
        wifi.removeNetwork(connection.getNetworkId());
    }

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager connectionManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

    public ScanResult getSelectedNetwork() {
        return selectedNetwork;
    }

}