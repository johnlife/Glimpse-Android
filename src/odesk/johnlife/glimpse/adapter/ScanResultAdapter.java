package odesk.johnlife.glimpse.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import odesk.johnlife.glimpse.R;

public class ScanResultAdapter extends ArrayAdapter<ScanResult> {

    public ScanResultAdapter(Context context, List<ScanResult> list) {
        super(context, R.layout.wifi_list_item, list);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        view.setText(getItem(position).SSID);
        return view;
    }

}