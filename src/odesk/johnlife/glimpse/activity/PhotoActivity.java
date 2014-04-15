package odesk.johnlife.glimpse.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.PictureData;
import odesk.johnlife.glimpse.data.db.DatabaseHelper;
import odesk.johnlife.glimpse.util.MailConnector;
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
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
	private static final int[] samples = {R.drawable.wp1, R.drawable.wp2, R.drawable.wp3, R.drawable.wp4, R.drawable.wp5, R.drawable.wp6};

	private Bitmap activeImage = null;
	private int activeIndex = -1 , myProgress = 0;
	private ImageView top;
	private ImageView base;
	private View contentView;
	private ProgressBar progress , progressBar;
	private View listPane;
	private View errorPane;
	private WifiManager wifi;
	private WifiConnectionHandler wifiConnectionHandler = new WifiConnectionHandler();
	private Context context;
	private List<PictureData> pictures = new ArrayList<PictureData>();
	private DatabaseHelper databaseHelper;
	private SQLiteDatabase db;

	public interface ConnectedListener {
		public void onConnected();
	}

	private class WifiConnectionHandler {
		private ListView list;
		private View wifiDialog;
		private TextView password; 
		private TextView networkName;
		private ScanResult activeNetwork;

		private final Runnable focusRunnable = new Runnable() {
			@Override
			public void run() {
				password.requestFocus();
			}
		};

		private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				String action = intent.getAction();
				if (action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
					if (isConnectedOrConnecting() || errorPane.getVisibility() == View.VISIBLE) return;
					TreeSet<ScanResult> sortedResults = new TreeSet<ScanResult>(new Comparator<ScanResult>() {
						@Override
						public int compare(ScanResult lhs, ScanResult rhs) {
								return -WifiManager.compareSignalLevel(lhs.level, rhs.level);
						}
					});			
					sortedResults.addAll(wifi.getScanResults());
					ArrayList<ScanResult> scanResults = new ArrayList<ScanResult>(sortedResults.size());
					TreeSet<String> nameLans = new TreeSet<String>();
					for (ScanResult net : sortedResults) {
						if (!net.SSID.trim().isEmpty() && nameLans.add(net.SSID)) {
							scanResults.add(net);	
						}
					}
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
							activeNetwork = adapter.getItem(position);
							if (activeNetwork.capabilities.startsWith("[ESS")) {
								progressBar.setVisibility(View.VISIBLE);
								new WifiConnector(PhotoActivity.this).connectTo(activeNetwork);
							} else {
								wifiDialog.setVisibility(View.VISIBLE);
								password.setText("");
								password.post(focusRunnable);
								networkName.setText(activeNetwork.SSID);								
							}
						}
					});
					if (isConnectedOrConnecting()) return;
					progressBar.setVisibility(View.INVISIBLE);					
					listPane.setVisibility(View.VISIBLE);
					listPane.setAlpha(0);
					listPane.setTranslationX(listPane.getWidth());
					listPane.animate().translationX(0).alpha(1).start();
				} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
					boolean connecting = info.getState() == NetworkInfo.State.CONNECTING;
					boolean visible = listPane.getVisibility() == View.VISIBLE;
					if (connected) {
						connectedListener.onConnected();
						if (visible) {
							listPane.animate().translationX(listPane.getWidth()).alpha(0).setListener(new AnimatorListenerAdapter() {
								@Override
								public void onAnimationEnd(Animator animation) {
									progressBar.setVisibility(View.INVISIBLE);
									listPane.setVisibility(View.INVISIBLE);
									listPane.setTranslationX(0);
									listPane.setAlpha(1);
									listPane.animate().setListener(null).start();
								}
							}).start();
						}						
					}
					if (!visible && !connected && !connecting) {
						scanWifi();
					}
				}
			}
		};

		public void createUi(Bundle savedInstanceState) {
			list = (ListView) findViewById(R.id.list);
			listPane = findViewById(R.id.list_container);
			wifiDialog = findViewById(R.id.wifi_pane);
			wifiDialog.setVisibility(View.INVISIBLE);
			password = (TextView) wifiDialog.findViewById(R.id.password);
			password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) { 
						connectNetwork();						
						return true;
					}
					return false;
				}
			});
			networkName = (TextView) wifiDialog.findViewById(R.id.title);
			wifiDialog.findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					connectNetwork();				
				}
			});
			if (null == savedInstanceState) {
				wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
				if (!wifi.isWifiEnabled()) {
					wifi.setWifiEnabled(true);
					scanWifi();
				} 
			} 
			if (isConnectedOrConnecting()) {
				listPane.setVisibility(View.INVISIBLE);
			} else {
				scanWifi();
			}
			registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		}

		public void connectNetwork() {
			wifiDialog.setVisibility(View.INVISIBLE);
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
			if (activeNetwork != null) {
				progressBar.setVisibility(View.VISIBLE);
				new WifiConnector(PhotoActivity.this).connectTo(activeNetwork, password.getText().toString());
			}
		}

		public void scanWifi() {
			wifi.startScan();
			registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}

		public View getView() {
			return listPane;
		}

		public boolean isConnectionDialogVisible() {
			return wifiDialog.getVisibility() == View.VISIBLE;
		}

		public void hideConnectionDialog() {
			wifiDialog.setVisibility(View.INVISIBLE); 
		}
	}

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
			View[] swipeBlockers = {progress, wifiConnectionHandler.getView(), errorPane}; 
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

	private Runnable myThread = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			while (myProgress < 1000) {
				try {
					myProgress++;
					progressBar.setProgress(myProgress);
				} catch (Throwable t) {
				}
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_photo);
		context = this;
		databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
		db = databaseHelper.getWritableDatabase();
		contentView = findViewById(android.R.id.content);
		top = (ImageView) findViewById(R.id.top);
		base = (ImageView) findViewById(R.id.base);
		errorPane = findViewById(R.id.error_pane);
		final String user = getUser();
		if (user == null) {
			((TextView) errorPane.findViewById(R.id.error_text)).setText(R.string.error_no_user_data);
			errorPane.setVisibility(View.VISIBLE);
		}
		wifiConnectionHandler.createUi(savedInstanceState);		
		progress = (ProgressBar) findViewById(R.id.progress);
		progressBar = (ProgressBar) findViewById(R.id.progressLoading);	
		new Thread(myThread).start();
		progress.setRotation(-90);
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.hide();
		
		//		contentView.post(hiderAction);
		contentView.setOnTouchListener(touchListener);
		initPictures();
		swipeImage();
		if (user != null) {
			Timer mailTimer = new Timer();
			mailTimer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					MailConnector mailer = new MailConnector(user, "temppass", context);
					mailer.connect();
					initPictures();
				}
			}, 0, 5000);
		}
	}

	private String getUser() {
		String user = null;
		try {
			File dataFile = getExternalFilesDir(context.getString(R.string.data_file));
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
			String line = br.readLine();
			if (line != null) {
				user = line;
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			return user;
		}
	}

	private void initPictures() {
		synchronized (pictures) {
			File folder = GlimpseApp.getPicturesDir();
			if (folder.listFiles().length != 0) {
				runOnUiThread(hideErrorPane);
				pictures.clear();
				for (File picFile : folder.listFiles()) {
					pictures.add(PictureData.createPicture(picFile));
					//TODO
					databaseHelper.toDb(PictureData.createPicture(picFile).getBitmap());
				}
			} else {
				if (pictures.size() == 0) {
					for (int sample : samples) {
						pictures.add(PictureData.createPicture(sample, context));
					}
				}
			}
		}
	}

	private Runnable hideErrorPane = new Runnable() {
		@Override
		public void run() {
			errorPane.setVisibility(View.INVISIBLE);
		}
	};

	public ConnectedListener connectedListener = new ConnectedListener() {
		@Override
		public void onConnected() {
			File folder = GlimpseApp.getPicturesDir();
			if (folder.listFiles().length == 0 && getUser()!= null) {
				String message = getString(R.string.error_no_foto, getUser());
				((TextView) errorPane.findViewById(R.id.error_text)).setText(message);
				errorPane.setVisibility(View.VISIBLE);
			}
		}
	};

	private boolean isConnectedOrConnecting() {
		ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiConnection = connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifiConnection.isConnectedOrConnecting();
	}

	private void swipeImage() {
		if (null != activeImage) {
			top.setImageBitmap(activeImage);
			top.setAlpha(1f);
			top.animate().alpha(0f).setDuration(600).start();
		}
		Bitmap newBitmap;
		synchronized (pictures) {
			if (++activeIndex >= pictures.size()) activeIndex = 0;
			newBitmap = pictures.get(activeIndex).getBitmap();
		}
		base.setImageBitmap(newBitmap);
		activeImage = newBitmap;
		base.postDelayed(swipeRunnable, 5000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onBackPressed() {
		if (wifiConnectionHandler.isConnectionDialogVisible()) {
			wifiConnectionHandler.hideConnectionDialog();
		} else {
			super.onBackPressed();
		}
	}

}
