package odesk.johnlife.glimpse.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.activity.PhotoActivity;
import odesk.johnlife.glimpse.util.WifiConnector;

public class BlurListView extends BlurLayout {

    private ArrayAdapter<ScanResult> adapter;

    public BlurListView(Context context) {
        super(context);
        createView(context);
    }

    public BlurListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView(context);
    }

    public BlurListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createView(context);
    }

    private void createView(Context context) {
        inflate(context, R.layout.list_view, this);
        ListView list = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<ScanResult>(context, R.layout.wifi_list_item, new ArrayList<ScanResult>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setText(getItem(position).SSID);
                return view;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hide(false);
                //TODO
//                activeNetwork = adapter.getItem(position);
//                String cap = activeNetwork.capabilities;
//                if (cap.isEmpty() || cap.startsWith("[ESS")) {
//                    progressBar.setVisibility(View.VISIBLE);
//                    new WifiConnector(PhotoActivity.this).connectTo(activeNetwork);
//                } else {
//                    String BSSID = preferences.getString(PREF_WIFI_BSSID, "");
//                    String pass = preferences.getString(PREF_WIFI_PASSWORD, "");
//                    if (activeNetwork.BSSID.equals(BSSID) && !pass.equals("")) {
//                        connectToNetwork(pass);
//                    } else {
//                        wifiDialogFrame.setVisibility(View.VISIBLE);
//                        wifiDialog.setVisibility(View.VISIBLE);
//                        password.setText("");
//                        password.postDelayed(focusRunnable, 150);
//                        password.requestFocus();
//                        networkName.setText(activeNetwork.SSID);
//                    }
//                }
//                if (!isConnectedOrConnecting() && wifiDialogFrame.getVisibility() != View.VISIBLE
//                        && progressBar.getVisibility() == View.GONE) {
//                    showHint(getResources().getString(R.string.hint_wifi_error));
//                }
//                view.setClickable(true);
                //or
                //WifiReceiver.getInstance().connectToNetwork(adapter.getItem(position));
            }
        });
    }

    public void update(List<ScanResult> result) {
        TreeSet<ScanResult> sortedResults = new TreeSet<ScanResult>(
                new Comparator<ScanResult>() {
                    @Override
                    public int compare(ScanResult lhs, ScanResult rhs) {
                        return -WifiManager.compareSignalLevel(lhs.level, rhs.level);
                    }
                });
        sortedResults.addAll(result);
        ArrayList<ScanResult> scanResults = new ArrayList<ScanResult>(sortedResults.size());
        TreeSet<String> nameLans = new TreeSet<String>();
        for (ScanResult net : sortedResults) {
            if (!net.SSID.trim().isEmpty() && nameLans.add(net.SSID)) scanResults.add(net);
        }
        adapter.clear();
        adapter.addAll(scanResults);
        show();
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide(boolean withAnimation) {
        if (withAnimation) {
            animate().translationX(getWidth()).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setVisibility(View.GONE);
                    setTranslationX(0);
                    setAlpha(1);
                    animate().setListener(null).start();
                }
            }).start();
        } else {
            setVisibility(View.GONE);
        }
    }
}