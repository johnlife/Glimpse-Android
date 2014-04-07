package odesk.johnlife.glimpse.activity;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.util.SystemUiHider;
import odesk.johnlife.glimpse.util.WifiConnector;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class PhotoActivity extends Activity {
	private static final int[] pictures = {R.drawable.wp1, R.drawable.wp2, R.drawable.wp3, R.drawable.wp4, R.drawable.wp5, R.drawable.wp6};

	private Bitmap activeImage = null;
	private int activeIndex = -1;
	private ImageView top;
	private ImageView base;
	private View contentView;
	private ProgressBar progress;
	private WifiManager wifi;
	private Context context;
	private ListView list;
	private View listPane;
	private View wifiDialog;

	//wifi-selection
	private ScanResult activeNetwork;

	
	private Runnable hiderAction = new Runnable() {
		@Override
		public void run() {
			final int hideFlags = 
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					//			        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.STATUS_BAR_HIDDEN
					//			        | View.SYSTEM_UI_FLAG_IMMERSIVE
					;
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				// Pre-Jelly Bean, we must manually hide the action bar
				// and use the old window flags API.
				getWindow().setFlags(
						WindowManager.LayoutParams.FLAG_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
			contentView.setSystemUiVisibility(hideFlags);
			contentView.postDelayed(hiderAction, 30000);
		}
	};

	private Runnable swipeRunnable = new Runnable() {
		@Override
		public void run() {
			View[] swipeBlockers = {progress, listPane}; 
			boolean blocked = false;
			for (View blocker : swipeBlockers) {
				blocked |= blocker.getVisibility() == View.VISIBLE;
			}
			if (blocked) {
				base.postDelayed(swipeRunnable, 50);
			} else {
				swipeImage();
			}
		}
	};

	private Runnable progressRunnable = new Runnable() {
		@Override
		public void run() {
			System.out.println("tick");
			if (progress.getVisibility() != View.VISIBLE) {
				progress.removeCallbacks(progressRunnable);
			} else {
				int value = progress.getProgress()+1;
				if (value >= progress.getMax()) {
					//TODO: run some action
					progress.removeCallbacks(progressRunnable);
				}
				progress.setProgress(value);
				progress.postDelayed(progressRunnable, 30);
			}
		}
	};


	OnTouchListener touchListener = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getActionMasked();
			float x = event.getRawX();
			float y = event.getRawY();
			progress.setTranslationX(x-(progress.getWidth()/2));
			progress.setTranslationY(y-(progress.getHeight()/2));
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				progress.setVisibility(View.VISIBLE);
				progress.setProgress(0);
				progress.post(progressRunnable);
				System.out.println("boom");
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				System.out.println("hide");
				progress.setVisibility(View.INVISIBLE);
				progress.setProgress(0);
				progress.removeCallbacks(progressRunnable);
				break;
			default:
				break;
			}
			return true;
		}
	};

	private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context c, Intent intent) {
			String action = intent.getAction();
			if (action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
				if (isWifiConnected()) return;
				List<ScanResult> scanResults = wifi.getScanResults();
				Collections.sort(scanResults, new Comparator<ScanResult>() {
					@Override
					public int compare(ScanResult lhs, ScanResult rhs) {
						return -WifiManager.compareSignalLevel(lhs.level, rhs.level);
					}
				});
				final ArrayAdapter<ScanResult> adapter = new ArrayAdapter<ScanResult>(context, android.R.layout.simple_list_item_1, scanResults) {
					@Override
					public View getView(int position, View convertView, ViewGroup parent) {
						TextView view = (TextView) super.getView(position, convertView, parent);
						view.setText(getItem(position).SSID);
						return view;
					}
				};
				list.setAdapter(adapter);
				list.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						wifiDialog.setVisibility(View.VISIBLE);
						activeNetwork = adapter.getItem(position);
					}
				});
				listPane.setVisibility(View.VISIBLE);
				listPane.setAlpha(0);
				listPane.setTranslationX(listPane.getWidth());
				listPane.animate().translationX(0).alpha(1).start();
			} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
				boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
				boolean visible = listPane.getVisibility() == View.VISIBLE;
				if (visible && connected) {
					listPane.animate().translationX(listPane.getWidth()).alpha(0).setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							listPane.setVisibility(View.INVISIBLE);
							listPane.setTranslationX(0);
							listPane.setAlpha(1);
							listPane.animate().setListener(null).start();
						}
					}).start();
				}
				if (!visible && !connected) {
					scanWifi();
				}
			}
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photo);
		context = this;
		contentView = findViewById(android.R.id.content);
		top = (ImageView) findViewById(R.id.top);
		base = (ImageView) findViewById(R.id.base);
		list = (ListView) findViewById(R.id.list);
		listPane = findViewById(R.id.list_container);
		wifiDialog = findViewById(R.id.wifi_pane);
		wifiDialog.setVisibility(View.INVISIBLE);
		wifiDialog.findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				wifiDialog.setVisibility(View.INVISIBLE);
				if (activeNetwork != null) {
					new WifiConnector(PhotoActivity.this).connectTo(activeNetwork, "neverGo2Home");
				}				
			}
		});
		progress = (ProgressBar) findViewById(R.id.progress);
		progress.setRotation(-90);
		if (null == savedInstanceState) {
			wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
			if (!wifi.isWifiEnabled()) {
				wifi.setWifiEnabled(true);
				scanWifi();
			} 
		} 
		if (isWifiConnected()) {
			listPane.setVisibility(View.INVISIBLE);
		} else {
			scanWifi();
		}
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.hide();
		//		contentView.post(hiderAction);
		contentView.setOnTouchListener(touchListener);
		registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		swipeImage();
	}

	private boolean isWifiConnected() {
		ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiConnection = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifiConnection.isConnected();
	}

	private void scanWifi() {
		wifi.startScan();
		registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}

	private void swipeImage() {
		if (null != activeImage) {
			top.setImageBitmap(activeImage);
			top.setAlpha(1f);
			top.animate().alpha(0f).setDuration(600).start();
		}
		if (++activeIndex >= pictures.length) activeIndex = 0;
		Bitmap newBitmap = BitmapFactory.decodeResource(getResources(), pictures[activeIndex]);
		base.setImageBitmap(newBitmap);
		activeImage = newBitmap;
		base.postDelayed(swipeRunnable, 5000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


}
