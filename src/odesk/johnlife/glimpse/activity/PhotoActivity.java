package odesk.johnlife.glimpse.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.adapter.ImagePagerAdapter;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.DatabaseHelper;
import odesk.johnlife.glimpse.ui.BlurActionBar;
import odesk.johnlife.glimpse.ui.BlurActionBar.OnActionClick;
import odesk.johnlife.glimpse.ui.FreezeViewPager;
import odesk.johnlife.glimpse.util.MailConnector;
import odesk.johnlife.glimpse.util.WifiConnector;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class PhotoActivity extends Activity implements Constants {

	private ProgressBar progressBar;
	private View listPane;
	private View errorPane;
	private TextView errorText;
	private WifiManager wifi;
	private WifiConnectionHandler wifiConnectionHandler = new WifiConnectionHandler();
	private Context context;
	private DatabaseHelper databaseHelper;
	private ViewPager pager;
	private ImagePagerAdapter pagerAdapter;
	private BlurActionBar actionBar;
	private boolean isConnectErrorVisible = false;
	private Timer mailTimer = new Timer();
	private View deleteDialog;
	private FrameLayout deleteFrame;
	private NewEmailWizard newEmail;
	private HowItWorks howItWorks;
	private View messagePane;
	private TextView hintText;
	private SharedPreferences preferences;
	private boolean isAnimationNeeded = true;

	public interface ConnectedListener {
		public void onConnected();
	}

	private class NewEmailWizard {
		View frame;
		View dialog;
		View step1;
		View step2;
		TextView emailView;
		TextView error;
		
		OnClickListener closeListener = new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (null == dialog) return;
				dialog.setVisibility(View.GONE);
				step1.setVisibility(View.VISIBLE);
				step2.setVisibility(View.GONE);
			}
		};
		
		public NewEmailWizard() {
			frame = findViewById(R.id.new_email_frame);
			dialog = findViewById(R.id.new_email_dialog);
			dialog.setClickable(false);
			step1 = dialog.findViewById(R.id.step1);
			step2 = dialog.findViewById(R.id.step2);
			emailView = (TextView) step2.findViewById(R.id.email);
			error = (TextView) step2.findViewById(R.id.error);
			step1.findViewById(R.id.cancel).setOnClickListener(closeListener);
			step2.findViewById(R.id.cancel).setOnClickListener(closeListener);
			frame.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					hide();
					return true;
				}
			});
			step1.findViewById(R.id.received).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					step1.setVisibility(View.GONE);
					step2.setVisibility(View.VISIBLE);
					error.setVisibility(View.GONE);
				}
			});
			step2.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {				
				@Override
				public void onClick(View v) {
					String email = emailView.getText().toString();
					if (email.isEmpty()) {
						showError(R.string.error_email_empty);
						return;
					}
					String fullEmail = email+"@glimpseframe.com";
					if (!android.util.Patterns.EMAIL_ADDRESS.matcher(fullEmail).matches()) {
						showError(R.string.error_email_invalid);
						return;
					}
					try {
						FileWriter out = new FileWriter(getUserDataFile());
						out.write(fullEmail);
						out.close();
						restart();
					} catch (IOException e) {
//						PushLink.sendAsyncException(e);
					}
				}
				
				void showError(int errorId) {
					error.setText(errorId);
					error.setVisibility(View.VISIBLE);
				}
			});
		}

		public void show() {
			frame.setVisibility(View.VISIBLE);
			dialog.setVisibility(View.VISIBLE);
			step1.setVisibility(View.VISIBLE);
			step2.setVisibility(View.GONE);
		}
		
		public void hide(){
			frame.setVisibility(View.GONE);
			dialog.setVisibility(View.GONE);
		}
	}
	
	private class HowItWorks {
		View frame;
		
		public HowItWorks() {
			frame = findViewById(R.id.how_it_works_frame);
			frame.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent motionEvent) {
					hide();
					return true;
				}
			});
			findViewById(R.id.dialogButtonOK).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hide();
				}
			});
			TextView textEmail = (TextView) findViewById(R.id.textEmail);
			final SpannableStringBuilder string = new SpannableStringBuilder(getString(R.string.how_it_works_email) + " " + getUser());
		    string.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), getString(R.string.how_it_works_email).length(), string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			textEmail.setText(string);
			
		}

		public void show() {
			frame.setVisibility(View.VISIBLE);
		}
		
		public void hide(){
			frame.setVisibility(View.GONE);
		}
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
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(password, InputMethodManager.SHOW_IMPLICIT);
			}
		};

		private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				String action = intent.getAction();
				if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
					if (isConnectedOrConnecting() || isConnectErrorVisible == true) return;
					TreeSet<ScanResult> sortedResults = new TreeSet<ScanResult>(
							new Comparator<ScanResult>() {
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
					final ArrayAdapter<ScanResult> adapter = new ArrayAdapter<ScanResult>(
							context, R.layout.wifi_list_item, scanResults) {
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
							String cap = activeNetwork.capabilities;
							if (cap.isEmpty() || cap.startsWith("[ESS")) {
								progressBar.setVisibility(View.VISIBLE);
								new WifiConnector(PhotoActivity.this).connectTo(activeNetwork);
							} else {
								String BSSID = preferences.getString(PREF_WIFI_BSSID, "");
								String pass = preferences.getString(PREF_WIFI_PASSWORD, "");
								if (activeNetwork.BSSID.equals(BSSID) && !pass.equals("")) {
									connectToNetwork(pass);
								} else {
									wifiDialog.setVisibility(View.VISIBLE);
									password.setText("");
									password.postDelayed(focusRunnable, 150);
									password.requestFocus();
									networkName.setText(activeNetwork.SSID);
								}
							}
							if (!isConnectedOrConnecting())
								if (wifiDialog.getVisibility() != View.VISIBLE)
									if (progressBar.getVisibility() == View.GONE)
										showHint(getResources().getString(R.string.hint_wifi_error));
						}
					});
					if (isConnectedOrConnecting()) return;
					progressBar.setVisibility(View.INVISIBLE);
					errorPane.setVisibility(View.INVISIBLE);
					listPane.setVisibility(View.VISIBLE);
					if (isAnimationNeeded) {
						listPane.setAlpha(0);
						listPane.setTranslationX(listPane.getWidth());
						listPane.animate().translationX(0).alpha(1).start();
						isAnimationNeeded = false;
					}
				} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					NetworkInfo.DetailedState details = info.getDetailedState();
					boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
					boolean connecting = info.getState() == NetworkInfo.State.CONNECTING;
					boolean visible = listPane.getVisibility() == View.VISIBLE;
					boolean isSuspended = info.getState() == NetworkInfo.State.SUSPENDED;
					boolean unknown = info.getState() == NetworkInfo.State.UNKNOWN;
					if (isSuspended || unknown) {
						showHint(getResources().getString(R.string.hint_wifi_error));
					} else if (details == NetworkInfo.DetailedState.DISCONNECTED) {
						getActionBar().hide();
						if (wifi.isWifiEnabled()) {
							if (info.getExtraInfo() != null && info.getExtraInfo().equals("<unknown ssid>")) {
								showHint(getResources().getString(R.string.hint_wifi_error));
								new WifiConnector(context).forgetCurrent();
							} else {
								showHint(getResources().getString(R.string.hint_wifi_disconnected));
							}
						} else {
							showHint(getResources().getString(R.string.error_not_connected));
							wifi.setWifiEnabled(true);
						}
						scanWifi();
					} else if (connected && details == NetworkInfo.DetailedState.CONNECTED) {
						connectedListener.onConnected();
						hideConnectionDialog();
						showHint(getResources().getString(R.string.hint_success));
					} else if (!visible && !connected && !connecting) {
						getActionBar().hide();
						if (details != NetworkInfo.DetailedState.SCANNING)
							scanWifi();
					}
				}	
			}
		};
		public  BroadcastReceiver getReceiver() {
			return wifiScanReceiver;
		}
		private void hideListPane() {
			listPane.animate()
			.translationX(listPane.getWidth()).alpha(0)
			.setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					listPane.setVisibility(View.INVISIBLE);
					listPane.setTranslationX(0);
					listPane.setAlpha(1);
					listPane.animate().setListener(null).start();
				}
			}).start();
			isAnimationNeeded = true;
		}
		
		public void createUi(Bundle savedInstanceState) {
			list = (ListView) findViewById(R.id.list);
			listPane = findViewById(R.id.list_container);
			wifiDialog = findViewById(R.id.wifi_pane);
			password = (TextView) wifiDialog.findViewById(R.id.password);
			password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						connectToNetwork(password.getText().toString());
						hideConnectionDialog();
						if (!isConnectedOrConnecting()) 
							showHint(getResources().getString(R.string.hint_wifi_error));
						return true;
					}
					return false;
				}
			});
			networkName = (TextView) wifiDialog.findViewById(R.id.title);
			wifiDialog.findViewById(R.id.connect).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						hideConnectionDialog();
						connectToNetwork(password.getText().toString());
						if (!isConnectedOrConnecting())
							if (progressBar.getVisibility() == View.GONE)
								showHint(getResources().getString(R.string.hint_wifi_error));
					}
				});
			wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if (!wifi.isWifiEnabled()) {
				wifi.setWifiEnabled(true);
				registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				scanWifi();
			}
			if (isConnectedOrConnecting()) {
				listPane.setVisibility(View.INVISIBLE);
			} else {
				registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				scanWifi();
			}
			registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
		}
		
		public void unregisterBroadcast(){
			unregisterReceiver(wifiScanReceiver);
		}

		public void connectToNetwork(String pass) {
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
			if (activeNetwork != null) {
				progressBar.setVisibility(View.VISIBLE);
				WifiConnector wifiConnector = new WifiConnector(PhotoActivity.this);
				wifiConnector.connectTo(activeNetwork, pass); //password.getText().toString()
				if (wifiConnector.getConnectionResult() != -1){
					Editor editor = preferences.edit();
					editor.putString(PREF_WIFI_BSSID, activeNetwork.BSSID);
					editor.putString(PREF_WIFI_PASSWORD, pass);
					editor.apply();
				} else {
					progressBar.setVisibility(View.INVISIBLE);
					errorText.setText(R.string.error_not_connected);
					errorPane.setVisibility(View.VISIBLE);
					isConnectErrorVisible = true;
					errorPane.postDelayed(hideErrorPane, HINT_TIME);
				}
			}
		}

		public void scanWifi() {
			wifi.startScan();
			
		}

		public View getView() {
			return listPane;
		}

		public boolean isConnectionDialogVisible() {
			if (wifiDialog == null) {
				return false;
			} else {
				return wifiDialog.getVisibility() == View.VISIBLE;
			}
		}

		public void hideConnectionDialog() {
			wifiDialog.setVisibility(View.GONE);
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
		}
	}
	
	public ConnectedListener connectedListener = new ConnectedListener() {
		@Override
		public void onConnected() {
			Log.d("ConnectedListener", "Connected");
			progressBar.setVisibility(View.INVISIBLE);
			wifiConnectionHandler.hideListPane();
			if (GlimpseApp.getFileHandler().isEmpty() && getUser() != null) {
				showPaneError(getString(R.string.error_no_foto, getUser()));
			}
		}
	};

	private Runnable swipeRunnable = new Runnable() {
		@Override
		public void run() {
			boolean blocked = actionBar.isFreeze() || getActionBar().isShowing() || isBlocked();
			if (blocked || GlimpseApp.getFileHandler().isLocked()) {
				pager.postDelayed(swipeRunnable, 50);
			} else {
				int idx = pager.getCurrentItem()+1;
				if (idx == pagerAdapter.getCount()) {
					idx = 0;
				}
				pager.setCurrentItem(idx);
				rescheduleImageSwipe();
			}
		}

	};
	
	private Runnable hideErrorPane = new Runnable() {
		@Override
		public void run() {
			errorPane.setVisibility(View.INVISIBLE);
			if (isConnectErrorVisible == true) {
				isConnectErrorVisible = false;
				wifiConnectionHandler.scanWifi();
			}
		}
	};
	
	private TimerTask mailPollTask = new TimerTask() {
		private final static String tag = "MailPolling";
		@Override
		public void run() {
			Log.w(tag, "Polling mail server");
			String user = getUser();
			if (isConnected() && null != user) {
				MailConnector mailer = new MailConnector(user, "HPgqL2658P", context);
				mailer.connect();
				if (!GlimpseApp.getFileHandler().isEmpty()) {
					hideErrorPane();
				}
			} else {
				Log.w(tag, "Mail server poll canceled - not connected or user is null");
			}
		}
	};
	
	private boolean isBlocked() {
		View[] swipeBlockers = {
			wifiConnectionHandler.getView(),
			errorPane, 
			deleteDialog,
			newEmail.dialog,
			howItWorks.frame,
			progressBar 
		};
		if (!isConnectedOrConnecting())
			return true;
		for (View blocker : swipeBlockers) {
			if (blocker.getVisibility() == View.VISIBLE)
				return true;
		}
		return false;
	}

	private void restart() {
		Activity activity = PhotoActivity.this;
		activity.startActivity(new Intent(activity, activity.getClass()));
		activity.finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		setContentView(R.layout.activity_photo);
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final String tag = "StartUp";
		newEmail = new NewEmailWizard();
		progressBar = (ProgressBar) findViewById(R.id.progressLoading);
		boolean sdcardReady = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && GlimpseApp.getPicturesDir().canWrite();
		if (!sdcardReady) {
			Log.w(tag, "SDcard isn't ready, scheduling restart");
			progressBar.setVisibility(View.VISIBLE);
			IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED); 
			filter.addDataScheme("file"); 
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.w(tag, "Restarting");
					restart();
				}
			}, filter);
			return;
		} else {
			Log.d(tag, "SDCard is ready");
		}
		createActionBar();
		Log.w(tag, "Actionbar created");
		context = this;
		howItWorks = new HowItWorks();
		databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
		pager = (ViewPager) findViewById(R.id.pager);
		pagerAdapter = new ImagePagerAdapter(this, databaseHelper, new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isBlocked()) return;
				ActionBar actionBar = getActionBar();
				if (actionBar.isShowing()) {
					actionBar.hide();
				} else {
					actionBar.show();
				}
			}
		});
		((FreezeViewPager)pager).setSwipeValidator(new FreezeViewPager.SwipeValidator() {
			@Override
			public boolean isSwipeBlocked() {
				return isBlocked();
			}
		});
		pager.setAdapter(pagerAdapter);
		pager.setOffscreenPageLimit(2);
		pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				rescheduleImageSwipe();
				pagerAdapter.setImageShown(position);
				if (actionBar.isFreeze()) {
					actionBar.unFreeze();
				}
			}
		});
		Log.w(tag, "Pager created");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		errorPane = findViewById(R.id.error_pane);
		messagePane = findViewById(R.id.message_pane);
		hintText = ((TextView) messagePane.findViewById(R.id.hint_text));
		errorText = ((TextView) errorPane.findViewById(R.id.error_text));
		wifiConnectionHandler.createUi(savedInstanceState);
		Log.w(tag, "Wifi ui created");
		deleteFrame = (FrameLayout) findViewById(R.id.delete_frame);
		deleteFrame.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				deleteFrame.setVisibility(View.GONE);
				deleteDialog.setVisibility(View.GONE);
				return true;
			}
		});
		deleteDialog = findViewById(R.id.delete_confirm);
		deleteDialog.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pagerAdapter.deleteCurrentItem(pager);
				deleteDialog.setVisibility(View.GONE);
				if (GlimpseApp.getFileHandler().isEmpty() && getUser() != null) {
					showPaneError(getString(R.string.error_no_foto, getUser()));
				}
			}
		});
		deleteDialog.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				deleteDialog.setVisibility(View.GONE);
			}
		});
		final String user = getUser();
		mailTimer.scheduleAtFixedRate(mailPollTask, 0, REFRESH_RATE);
		Log.w(tag, "Got user "+user);
		if (user == null) {
			showPaneError(R.string.error_no_user_data);
			return;
		}
		if (pagerAdapter.getCount() >= 2) rescheduleImageSwipe();
	}
	
	@Override
	protected void onDestroy() {
		wifiConnectionHandler.unregisterBroadcast();
		mailTimer.cancel();
		super.onDestroy();
	}

	private void createActionBar() {
		actionBar = new BlurActionBar(this);
		actionBar.setOnActionClickListener(new OnActionClick() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.action_delete) {
					newEmail.hide();
					deleteFrame.setVisibility(View.VISIBLE);
					deleteDialog.setVisibility(View.VISIBLE);
				} else if (v.getId() == R.id.action_setting) {
					showPopupMenu(v);
				} else
					newEmail.hide();
			}
		});
	}

	private void showPopupMenu(View view) {
		try {
			LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = layoutInflater.inflate(R.layout.popup_menu, null);
			final PopupWindow popupWindow = new PopupWindow(context);
			popupWindow.setContentView(layout);
			popupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
			popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
			popupWindow.setOutsideTouchable(true);
			popupWindow.setTouchable(true);
			popupWindow.setFocusable(true);
			popupWindow.setBackgroundDrawable(new BitmapDrawable());
			final TextView change_email = (TextView) layout.findViewById(R.id.change_email);
			final TextView reset_wifi = (TextView) layout.findViewById(R.id.reset_wifi);
			final TextView how_it_works = (TextView) layout.findViewById(R.id.how_it_works);
			change_email.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					getActionBar().hide();
					popupWindow.dismiss();
					newEmail.show();
				}
			});
			reset_wifi.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popupWindow.dismiss();
					if (isConnected()) {
						new WifiConnector(context).forgetCurrent();
						hideErrorPane();
						progressBar.setVisibility(View.VISIBLE);
						registerReceiver(wifiConnectionHandler.getReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
					}
				}
			});
			how_it_works.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popupWindow.dismiss();
					getActionBar().hide();
					howItWorks.show();
				}
			});
			popupWindow.showAsDropDown(view, 5, 5);
		} catch (Exception e) {
			e.printStackTrace();
		}		
//		PopupMenu popupMenu = new PopupMenu(this, v);
//		popupMenu.inflate(R.menu.popup_menu);
//		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//			@Override
//			public boolean onMenuItemClick(MenuItem item) {
//				newEmail.hide();
//				switch (item.getItemId()) {
//				case R.id.menu1:
//					newEmail.show();
//					return true;
//				case R.id.menu2:
//					if (isConnected()) {
//						new WifiConnector(context).forgetCurrent();
//						hideErrorPane();
//						progressBar.setVisibility(View.VISIBLE);
//						registerReceiver(wifiConnectionHandler.getReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
//					}
//					return true;
//				case R.id.menu3:
//					getActionBar().hide();
//					howItWorks.show();
//					return true;
//				default:
//					return false;
//				}
//			}
//		});
//		popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
//			@Override
//			public void onDismiss(PopupMenu menu) {
//				// TODO Auto-generated method stub
//			}
//		});
//		popupMenu.show();
	}
	
	protected void showHint(String hint) {
		hintText.setText(hint);
		messagePane.setVisibility(View.VISIBLE);
		messagePane.postDelayed(new Runnable() {
			@Override
			public void run() {
				messagePane.setVisibility(View.GONE);
			}
		}, HINT_TIME);
	}
	private String getUser() {
		String user = null;
		try {
			File dataFile = getUserDataFile();
			if (!dataFile.exists()) return null;
			BufferedReader br = new BufferedReader(new FileReader(dataFile));
			String line = br.readLine();
			if (line != null) {
				user = line;
			}
			br.close();
		} catch (Exception e) {
			Log.e("UserInfo", e.getMessage(), e);
		}
		return Normalizer.normalize(user, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
	}

	private File getUserDataFile() {
		File dataFile = new File(
			Environment.getExternalStorageDirectory(),
			context.getString(R.string.data_file)
		);
		return dataFile;
	}

	private void hideErrorPane() {
		if (errorPane.getVisibility() == View.VISIBLE) {
			runOnUiThread(hideErrorPane);
		}
	}

	private void rescheduleImageSwipe() {
		pager.removeCallbacks(swipeRunnable);
		pager.postDelayed(swipeRunnable, 8000);
	}
	
	private void showPaneError(String text) {
		errorText.setText(text);
		errorPane.setVisibility(View.VISIBLE);
	}
	
	private void showPaneError(int resId) {
		showPaneError(context.getString(resId));
	}
	
	private boolean isConnectedOrConnecting() {
		return getNetworkInfo().isConnectedOrConnecting();
	}

	public boolean isConnected() {
		return getNetworkInfo().isConnected();
	}

	private NetworkInfo getNetworkInfo() {
		ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		return connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
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