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
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import java.util.List;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.activity.PhotoActivity;

public class WifiReceiver implements Constants {

    public class WifiErrorData {

        private WifiError type;
        private Object data;

        public WifiErrorData(WifiError type) {
            this.type = type;
            this.data = null;
        }

        public WifiErrorData(WifiError type, Object data) {
            this.type = type;
            this.data = data;
        }

        public WifiError getType() {
            return type;
        }

        public Object getData() {
            return data;
        }
    }

    public enum WifiError {NEED_PASSWORD, CONNECT_ERROR, DISCONNECTED, UNKNOWN_ERROR }

    public interface OnWifiConnectionListener {
        void connecting();
        void connected();
        void disconnected(WifiErrorData error);
        void onScanning();
        void onScansResultReceive(List<ScanResult> scanResults);
    }

    public static WifiReceiver getInstance(PhotoActivity activity, OnWifiConnectionListener listener) {
        if (instance == null) {
            instance = new WifiReceiver(activity, listener);
        } else {
            instance.updateFields(activity, listener);
        }
        return instance;
    }

    private WifiReceiver(PhotoActivity activity, OnWifiConnectionListener listener) {
        updateFields(activity, listener);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        wifi = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(true);
            scanWifi();
        }
    }

    private void updateFields(PhotoActivity activity, OnWifiConnectionListener listener) {
        this.activity = activity;
        this.listener = listener;
        registerWifiBroadcast(true);
    }

    private static WifiReceiver instance;
    private WifiManager wifi;
    private PhotoActivity activity;
    private OnWifiConnectionListener listener;
    private SharedPreferences prefs;
    private boolean isConnecting;

    private final BroadcastReceiver supplicantStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (supplicantState == SupplicantState.DISCONNECTED) {
                    if (intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING) {
                        prefs.edit().remove(PREF_WIFI_PASSWORD).apply();
                        listener.disconnected(new WifiErrorData(WifiError.CONNECT_ERROR));
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
                    listener.disconnected(new WifiErrorData(WifiError.UNKNOWN_ERROR));
                } else if (details == NetworkInfo.DetailedState.DISCONNECTED) {
                    isConnecting = false;
                    resetCurrentWifi();
                    prefs.edit().putString(PREF_WIFI_PASSWORD, "").apply();
                    listener.disconnected(new WifiErrorData(WifiError.DISCONNECTED));
                } else if (connected && details == NetworkInfo.DetailedState.CONNECTED) {
                    isConnecting = false;
                    listener.connected();
                } else if (!connected && !connecting) {
                    if (details != NetworkInfo.DetailedState.SCANNING) {
                        listener.onScanning();
                        scanWifi();
                    }
                }

            }
        }
    };

    public void registerWifiBroadcast(boolean isNeed) {
        try {
            if (isNeed) {
                IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                activity.registerReceiver(wifiScanReceiver, new IntentFilter(filter));
                activity.registerReceiver(supplicantStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            } else {
                activity.unregisterReceiver(wifiScanReceiver);
                activity.unregisterReceiver(supplicantStateReceiver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectToNetwork(ScanResult activeNetwork) {
        String pass = prefs.getString(PREF_WIFI_PASSWORD, "");
        connectToNetwork(activeNetwork, activeNetwork.BSSID.equals(prefs.getString(PREF_WIFI_BSSID, "")) && !pass.isEmpty() ? pass : null);
    }

    public void connectToNetwork(ScanResult activeNetwork, String pass) {
        listener.connecting();
        String cap = activeNetwork.capabilities;
        if (cap.isEmpty() || cap.startsWith("[ESS")) {
            checkConnectionResult(activeNetwork, pass);
        } else{
            if (pass == null) {
                isConnecting = false;
                listener.disconnected(new WifiErrorData(WifiError.NEED_PASSWORD, activeNetwork));
            } else {
                checkConnectionResult(activeNetwork, pass);
            }
        }
    }

    private void checkConnectionResult(ScanResult activeNetwork, String pass) {
        isConnecting = true;
        WifiConnector wifiConnector = new WifiConnector(activity);
        if (pass == null) {
            wifiConnector.connectTo(activeNetwork);
        } else {
            wifiConnector.connectTo(activeNetwork, pass);
        }
        if (wifiConnector.getConnectionResult() != -1) {
            isConnecting = true;
            if (pass != null) prefs.edit().putString(PREF_WIFI_BSSID, activeNetwork.BSSID).putString(PREF_WIFI_PASSWORD, pass).apply();
        } else {
            isConnecting = false;
            listener.disconnected(new WifiErrorData(WifiError.UNKNOWN_ERROR));
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
        new WifiConnector(activity).forgetCurrent();
    }

    private NetworkInfo getNetworkInfo() {
        ConnectivityManager connectionManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    }

}