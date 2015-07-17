package odesk.johnlife.glimpse.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Timer;
import java.util.TimerTask;

import odesk.johnlife.glimpse.Constants;
import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.adapter.ImagePagerAdapter;
import odesk.johnlife.glimpse.adapter.ImagesGalleryAdapter;
import odesk.johnlife.glimpse.app.GlimpseApp;
import odesk.johnlife.glimpse.data.DatabaseHelper;
import odesk.johnlife.glimpse.data.PictureData;
import odesk.johnlife.glimpse.dialog.DeletingDialog;
import odesk.johnlife.glimpse.dialog.HelpDialog;
import odesk.johnlife.glimpse.ui.BlurActionBar;
import odesk.johnlife.glimpse.ui.BlurActionBar.OnActionClick;
import odesk.johnlife.glimpse.ui.BlurListView;
import odesk.johnlife.glimpse.ui.FreezeViewPager;
import odesk.johnlife.glimpse.util.MailConnector;
import odesk.johnlife.glimpse.util.MailConnector.OnItemDownloadListener;
import odesk.johnlife.glimpse.util.WifiConnector;
import odesk.johnlife.glimpse.util.WifiRedirectionTask;

public class PhotoActivity extends Activity implements Constants {

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
					String fullEmail = email + "@glimpseframe.com";
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

		@SuppressWarnings("unused")
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

	private class WifiConnectionHandler {
		private View wifiDialog;
		private View wifiDialogFrame;
		private TextView password;
		private CheckBox showPassword;
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

		private final BroadcastReceiver supplicantStateReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
					boolean connected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
					SupplicantState supplicantState = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
					if (supplicantState == (SupplicantState.COMPLETED)) {
						// do something
					} else if (supplicantState == (SupplicantState.DISCONNECTED)) {
						resetPass++;
						if (resetPass >= 3) {
							Editor editor = preferences.edit();
							editor.putString(PREF_WIFI_PASSWORD, "");
							editor.apply();
							resetPass = 0;
							showHint(getResources().getString(R.string.hint_failed_to_connecn));
						}
					}
				}
			}
		};
		private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				progressBar.setVisibility(View.GONE);
				wifiList.hide(false);
				String action = intent.getAction();
				if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
					if (isConnectedOrConnecting() || wifiDialogFrame.getVisibility() == View.VISIBLE) return;
					wifiList.update(wifi.getScanResults());
					if (!isConnectedOrConnecting()) return;
					progressBar.setVisibility(View.GONE);
					errorPane.setVisibility(View.GONE);
					wifiList.hide(false);
					//TODO
					if (isAnimationNeeded) {
						wifiList.setAlpha(0);
						wifiList.setTranslationX(wifiList.getWidth());
						wifiList.animate().translationX(0).alpha(1).start();
						isAnimationNeeded = false;
					}
				} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
					final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					NetworkInfo.DetailedState details = info.getDetailedState();
					boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
					boolean connecting = info.getState() == NetworkInfo.State.CONNECTING;
					boolean visible = wifiList.getVisibility() == View.VISIBLE;
					boolean isSuspended = info.getState() == NetworkInfo.State.SUSPENDED;
					boolean unknown = info.getState() == NetworkInfo.State.UNKNOWN;
					if (isSuspended || unknown) {
						showHint(getResources().getString(R.string.hint_wifi_error));
					} else if (details == NetworkInfo.DetailedState.DISCONNECTED) {
						//		hideErrorPane();
						registerScanReciver();
						getActionBar().hide();
						wifiDialog.postDelayed(new Runnable() {
							@Override
							public void run() {
								if (wifi.isWifiEnabled()) {
									if (info.getExtraInfo() != null && info.getExtraInfo().equals("<unknown ssid>")) {
										new WifiConnector(context).forgetCurrent();
									}
									if (isDisconnectionHintNeeded) {
										showHint(getResources().getString(R.string.hint_wifi_disconnected));
									}
								} else {
									wifi.setWifiEnabled(true);
								}
								scanWifi();
							}

						}, 1000);

					} else if (connected && details == NetworkInfo.DetailedState.CONNECTED) {
						WifiRedirectionTask redirectionTask = new WifiRedirectionTask() {

							@Override
							protected void onPostExecute(Boolean result) {
								if (result) {
									onConnected();
									hideConnectionDialog();
									isDisconnectionHintNeeded = true;
									showHint(getResources().getString(R.string.hint_success));
								} else {
//									isDisconnectionHintNeeded = true;
									showHint(getResources().getString(R.string.hint_failed_to_connecn));
									resetWifi();
								}
							}
						};
						redirectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
						resetPass = 0;
					} else if (!visible && !connected && !connecting) {
						getActionBar().hide();
						if (details != NetworkInfo.DetailedState.SCANNING) {
							scanWifi();
						}
					}

				}
			}
		};

		public  BroadcastReceiver getReceiver() {
			return wifiScanReceiver;
		}

		public void createUi(Bundle savedInstanceState) {
			wifiList = (BlurListView) findViewById(R.id.wifi_list);
			wifiDialog = findViewById(R.id.wifi_pane);
			wifiDialogFrame = findViewById(R.id.wifi_pane_frame);
			wifiDialogFrame.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					hideConnectionDialog();
					wifiList.show();
					scanWifi();
				}
			});
			password = (TextView) wifiDialog.findViewById(R.id.password);
			showPassword = (CheckBox) wifiDialog.findViewById(R.id.is_password_vivsible);
			password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_DONE) {
						connectToNetwork(password.getText().toString());
						hideConnectionDialog();
						if (!isConnectedOrConnecting()) {
							showHint(getResources().getString(R.string.hint_wifi_error));
						}
						return true;
					}
					return false;
				}
			});
			//((EditText)password).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			showPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						((EditText)password).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					} else {
						((EditText)password).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					}

				}
			});
			((EditText)password).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			showPassword.setVisibility(View.INVISIBLE);
			networkName = (TextView) wifiDialog.findViewById(R.id.title);
			wifiDialog.findViewById(R.id.connect).setOnClickListener(
					new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							hideConnectionDialog();
							connectToNetwork(password.getText().toString());
							if (!isConnectedOrConnecting() && progressBar.getVisibility() == View.GONE) {
								showHint(getResources().getString(R.string.hint_wifi_error));
							}
						}
					});
			wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			if (!wifi.isWifiEnabled()) {
				wifi.setWifiEnabled(true);
				registerScanReciver();
				//registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				scanWifi();
			}
			if (isConnectedOrConnecting()) {
				wifiList.hide(false);
			} else {
				registerScanReciver();
				//registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				scanWifi();
			}
			registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
			registerReceiver(supplicantStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
		}

		public void unregisterBroadcast() {
			unregisterReceiver(wifiScanReceiver);
			unregisterReceiver(supplicantStateReceiver);
		}

		public void connectToNetwork(String pass) {
			wifiList.hide(false);
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
			if (activeNetwork != null) {
				progressBar.setVisibility(View.VISIBLE);
				WifiConnector wifiConnector = new WifiConnector(PhotoActivity.this);
				wifiConnector.connectTo(activeNetwork, pass); //password.getText().toString()
				if (wifiConnector.getConnectionResult() != -1) {
					Editor editor = preferences.edit();
					editor.putString(PREF_WIFI_BSSID, activeNetwork.BSSID);
					editor.putString(PREF_WIFI_PASSWORD, pass);
					editor.apply();
				} else {
					progressBar.setVisibility(View.GONE);
					showHint(getResources().getString(R.string.hint_wifi_error));
					isConnectErrorVisible = true;
					wifiList.show();
					wifi.startScan();
				}
			}
		}

		public void scanWifi() {
			wifi.startScan();
		}

		public View getView() {
			return wifiList;
		}

		public boolean isConnectionDialogVisible() {
			return (wifiDialogFrame != null && wifiDialogFrame.getVisibility() == View.VISIBLE);
		}

		public void hideConnectionDialog() {
			wifiDialog.setVisibility(View.GONE);
			wifiDialogFrame.setVisibility(View.GONE);
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
		}
	}

	private Runnable swipeRunnable = new Runnable() {
		@Override
		public void run() {
			boolean blocked = actionBar.isFreeze() || getActionBar().isShowing() || isBlocked();
			if (blocked || GlimpseApp.getFileHandler().isLocked()) {
				pager.postDelayed(swipeRunnable, 50);
			} else {
				int idx = pager.getCurrentItem() + 1;
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
			errorPane.setVisibility(View.GONE);
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
			String user = getUser();
			if (isConnected() && null != user) {
				MailConnector mailer = new MailConnector(user, "HPgqL2658P", new OnItemDownloadListener() {
					@Override
					public void onItemDownload() {
						pagerAdapter.setHasNewPhotos(true);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								recreateSeeNewPhoto();
								showNewPhotos();
							}
						});
					}
				});
				mailer.connect();
				if (!GlimpseApp.getFileHandler().isEmpty()) {
					hideErrorPane();
				}
			}
		}
	};

	private ProgressBar progressBar;
	private BlurListView wifiList;
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
	private DeletingDialog deletingDialog;
	private HelpDialog helpDialog;
	@SuppressWarnings("unused")
	private NewEmailWizard newEmail;
	private View seeNewPhoto;
	private TextView seeNewPhotoBtn;
	private View messagePane;
	private TextView hintText;
	private Gallery gallery;
	private SharedPreferences preferences;
	private boolean isAnimationNeeded = true;
	private boolean isDisconnectionHintNeeded = false;
	private boolean isFreeze = false;
	private boolean galleryHideSeeNewPhoto;
	private boolean isScanRegisted;
	private int resetPass = 0;

	@Override
	public void onBackPressed() {
		if (wifiConnectionHandler.isConnectionDialogVisible()) {
			wifiConnectionHandler.hideConnectionDialog();
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		setContentView(R.layout.activity_photo);
		isScanRegisted = false;
		preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		final String tag = "StartUp";
		/** uncomment if newEmailWizard is needed*/
//		newEmail = new NewEmailWizard();
		progressBar = (ProgressBar) findViewById(R.id.progressLoading);
		seeNewPhoto = findViewById(R.id.see_new_photos_layout);
		seeNewPhotoBtn =  (TextView) findViewById(R.id.see_new_photos);
		boolean sdcardReady = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && GlimpseApp.getPicturesDir().canWrite();
		if (!sdcardReady) {
			progressBar.setVisibility(View.VISIBLE);
			IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
			filter.addDataScheme("file");
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					restart();
				}
			}, filter);
			return;
		}
		createActionBar();
		getActionBar().hide();
		context = this;
		helpDialog = (HelpDialog) findViewById(R.id.dialog_help);
		databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
		pager = (ViewPager) findViewById(R.id.pager);
		pagerAdapter = createAdapter();
		((FreezeViewPager)pager).setSwipeValidator(new FreezeViewPager.SwipeValidator() {
			@Override
			public boolean isSwipeBlocked() {
				return isBlocked();
			}
		});
		pagerAdapter.checkNewPhotos();
		if (pagerAdapter.hasNewPhotos() && isConnected()) {
			recreateSeeNewPhoto();
		}
		seeNewPhotoBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				seeNewPhoto.setVisibility(View.GONE);
				seeNewPhotoBtn.setVisibility(View.GONE);
				pagerAdapter.setHasNewPhotos(false);
				showNewPhotos();
			}
		});
		pager.setAdapter(pagerAdapter);
		pager.setOffscreenPageLimit(SCREEN_PAGE_LIMIT);
		pager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				rescheduleImageSwipe();
				pagerAdapter.setImageShown(position);
				if (actionBar.isFreeze()) {
					actionBar.unFreeze();
				}
				if (pagerAdapter.hasNewPhotos()) {
					recreateSeeNewPhoto();
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				if (state == 2 || state == 1) {
					seeNewPhoto.clearAnimation();
					if (getActionBar() != null && getActionBar().isShowing()) {
						getActionBar().hide();
					}
					seeNewPhoto.setVisibility(View.GONE);
					pager.setAlpha(0);
				} else if (state == 0) {
					Animation animation = AnimationUtils.loadAnimation(PhotoActivity.this, R.anim.image_alpha);
					pager.startAnimation(animation);
					pager.setAlpha(1);
				}
			}
		});
		gallery = (Gallery) findViewById(R.id.gallery1);
		gallery.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				gallery.setVisibility(View.GONE);
				pagerAdapter = createAdapter((PictureData) gallery.getItemAtPosition(position));
				pager.setAdapter(pagerAdapter);
				if (galleryHideSeeNewPhoto) {
					recreateSeeNewPhoto();
				}
			}
		});
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		errorPane = findViewById(R.id.error_pane);
		messagePane = findViewById(R.id.message_pane);
		hintText = ((TextView) messagePane.findViewById(R.id.hint_text));
		errorText = ((TextView) errorPane.findViewById(R.id.error_text));
		wifiConnectionHandler.createUi(savedInstanceState);
		deletingDialog = (DeletingDialog) findViewById(R.id.dialog_deleting);
		deletingDialog.setPositiveButtonListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pagerAdapter.deleteCurrentItem(pager);
				deletingDialog.hide();
				if (GlimpseApp.getFileHandler().isEmpty() && getUser() != null) {
					showPaneError(getString(R.string.error_no_foto, getUser()));
				}
			}
		});
		final String user = getUser();
		mailTimer.scheduleAtFixedRate(mailPollTask, 0, REFRESH_RATE);
		if (user == null) {
			showPaneError(R.string.error_no_user_data);
			return;
		}
		if (pagerAdapter.getCount() >= SCREEN_PAGE_LIMIT) rescheduleImageSwipe();
	}


	private void registerScanReciver() {
		if (!isScanRegisted) {
			registerReceiver(wifiConnectionHandler.getReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			isScanRegisted = true;
		}
	}

	private void recreateSeeNewPhoto() {
		if (gallery != null && gallery.getVisibility() == View.VISIBLE) return;
		seeNewPhotoBtn.setVisibility(View.VISIBLE);
		final Animation animation = AnimationUtils.loadAnimation(PhotoActivity.this, R.anim.image_alpha);
		seeNewPhoto.postDelayed(new Runnable() {
			@Override
			public void run() {
				seeNewPhoto.setVisibility(View.VISIBLE); // need to invalidate background
				Animation anim = animation;
				anim.setDuration(anim.getDuration()/2);
				seeNewPhoto.startAnimation(anim);
			}
		}, animation.getDuration());
	}

	@Override
	protected void onDestroy() {
		wifiConnectionHandler.unregisterBroadcast();
		mailTimer.cancel();
		super.onDestroy();
	}

	private ImagePagerAdapter createAdapter() {
		return new ImagePagerAdapter(this, databaseHelper, createOnCkickListener());
	}

	private ImagePagerAdapter createAdapter(PictureData pictureData) {
		return new ImagePagerAdapter(this, pictureData, databaseHelper, createOnCkickListener());
	}

	private OnClickListener createOnCkickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isBlocked()) return;
				createActionBar();
				ActionBar actionBar = getActionBar();
				if (actionBar.isShowing()) {
					actionBar.hide();
				} else {
					actionBar.show();
				}
				if (gallery != null && gallery.getVisibility() == View.VISIBLE) {
					gallery.setVisibility(View.GONE);
				}
			}
		};
	}

	private void onConnected() {
		progressBar.setVisibility(View.GONE);
		wifiList.hide(true);
		isAnimationNeeded = true;
		if (GlimpseApp.getFileHandler().isEmpty() && getUser() != null) {
			showPaneError(getString(R.string.error_no_foto, getUser()));
		}
	}

	private void showNewPhotos() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean old = pagerAdapter.hasNewPhotos();
				pagerAdapter = createAdapter();
				pagerAdapter.setHasNewPhotos(old);
				pager.setAdapter(pagerAdapter);
				rescheduleImageSwipe();
			}
		});
	}

	private boolean isBlocked() {
		View[] swipeBlockers = {
				wifiConnectionHandler.getView(),
				errorPane,
				deletingDialog,
				messagePane,
				/** uncomment if newEmail is needing*/
//			newEmail.dialog,
				helpDialog,
				progressBar
		};
		if (!isConnectedOrConnecting()) return true;
		for (View blocker : swipeBlockers) {
			if (blocker.getVisibility() == View.VISIBLE) return true;
		}
		return false;
	}

	private void restart() {
		Activity activity = PhotoActivity.this;
		activity.startActivity(new Intent(activity, activity.getClass()));
		activity.finish();
	}

	private void createActionBar() {
		if (actionBar == null) {
			actionBar = new BlurActionBar(this, isFreeze);
		} else {
			actionBar = new BlurActionBar(this, actionBar.isFreeze());
		}
		actionBar.setOnActionClickListener(new OnActionClick() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
					case R.id.action_delete:
						deletingDialog.show();
						break;
					case R.id.action_setting:
						showPopupMenu(v);
						break;
					case R.id.action_freeze:
						if (pagerAdapter.hasNewPhotos()) {
							recreateSeeNewPhoto();
						}
						break;
					case R.id.action_gallery:
						gallery.setAdapter(new ImagesGalleryAdapter(PhotoActivity.this));
						gallery.setVisibility(View.VISIBLE);
						if (seeNewPhoto.getVisibility() == View.VISIBLE) {
							galleryHideSeeNewPhoto = true;
						}
						break;
				}
				seeNewPhoto.setVisibility(View.GONE);
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
			/**
			 * uncomment if the item "change email" is needed */
//			layout.findViewById(R.id.change_email).setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					getActionBar().hide();
//					popupWindow.dismiss();
//					newEmail.show();
//				}
//			});
			layout.findViewById(R.id.reset_wifi).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popupWindow.dismiss();
					resetWifi();
				}
			});
			layout.findViewById(R.id.how_it_works).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popupWindow.dismiss();
					getActionBar().hide();
					helpDialog.show();
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
//
//			}
//		});
//		popupMenu.show();
	}

	public void showHint(int resId) {
		showHint(getResources().getString(resId));
	}

	public void resetWifi() {
		if (isConnected()) {
			isDisconnectionHintNeeded = false;
			new WifiConnector(context).forgetCurrent();
			hideErrorPane();
			progressBar.setVisibility(View.VISIBLE);
			registerScanReciver();
			//	registerReceiver(wifiConnectionHandler.getReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}
	}

	public void showHint(String hint) {
		hintText.setText(hint);
		messagePane.setVisibility(View.VISIBLE);
		messagePane.postDelayed(new Runnable() {
			@Override
			public void run() {
				messagePane.setVisibility(View.GONE);
			}
		}, HINT_TIME);
	}

	public String getUser() {
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
		pager.postDelayed(swipeRunnable, RESCHEDULE_REFRESH_RATE);
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

	private boolean isConnected() {
		return getNetworkInfo().isConnected();
	}

	private NetworkInfo getNetworkInfo() {
		ConnectivityManager connectionManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		return connectionManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	}

	private Point getScreenSize() {
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		return size;
	}

	private int getScreenWidth(double coefficient) {
		return (int) (getScreenSize().x * coefficient);
	}

	private int getScreenHeight(double coefficient) {
		return (int) (getScreenSize().y * coefficient);
	}

	/*
	//TODO
	private interface ConnectedListener {
		public void onConnected();
	}

	*/
}