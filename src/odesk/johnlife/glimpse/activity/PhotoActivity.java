package odesk.johnlife.glimpse.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;
import android.widget.PopupWindow;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.Normalizer;
import java.util.List;
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
import odesk.johnlife.glimpse.dialog.EmailChangeDialog;
import odesk.johnlife.glimpse.dialog.HelpDialog;
import odesk.johnlife.glimpse.dialog.WifiDialog;
import odesk.johnlife.glimpse.ui.BlurActionBar;
import odesk.johnlife.glimpse.ui.BlurActionBar.OnActionClick;
import odesk.johnlife.glimpse.ui.BlurListView;
import odesk.johnlife.glimpse.ui.BlurTextView;
import odesk.johnlife.glimpse.ui.FreezeViewPager;
import odesk.johnlife.glimpse.ui.HintTextView;
import odesk.johnlife.glimpse.util.MailConnector;
import odesk.johnlife.glimpse.util.MailConnector.OnItemDownloadListener;
import odesk.johnlife.glimpse.util.WifiConnectionListener;
import odesk.johnlife.glimpse.util.WifiReceiver;

public class PhotoActivity extends Activity implements Constants, WifiConnectionListener {

//	private class WifiConnectionHandler {
//		private ScanResult activeNetwork;
//
//		private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
//			@Override
//			public void onReceive(Context c, Intent intent) {
//				progressBar.setVisibility(View.GONE);
//				wifiList.hide(false);
//				String action = intent.getAction();
//				if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
//					if (isConnectedOrConnecting() /* TODO || wifiDialogFrame.getVisibility() == View.VISIBLE*/) return;
//					wifiList.update(wifi.getScanResults());
//					if (!isConnectedOrConnecting()) return;
//					progressBar.setVisibility(View.GONE);
//					errorPane.setVisibility(View.GONE);
//					wifiList.hide(false);
//					//TODO
//					if (isAnimationNeeded) {
//						wifiList.setAlpha(0);
//						wifiList.setTranslationX(wifiList.getWidth());
//						wifiList.animate().translationX(0).alpha(1).start();
//						isAnimationNeeded = false;
//					}
//				} else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
//					final NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//					NetworkInfo.DetailedState details = info.getDetailedState();
//					boolean connected = info.getState() == NetworkInfo.State.CONNECTED;
//					boolean connecting = info.getState() == NetworkInfo.State.CONNECTING;
//					boolean visible = wifiList.getVisibility() == View.VISIBLE;
//					boolean isSuspended = info.getState() == NetworkInfo.State.SUSPENDED;
//					boolean unknown = info.getState() == NetworkInfo.State.UNKNOWN;
//					if (isSuspended || unknown) {
//						showHint(getResources().getString(R.string.hint_wifi_error));
//					} else if (details == NetworkInfo.DetailedState.DISCONNECTED) {
//						//		hideErrorPane();
//						registerScanReciver();
//						getActionBar().hide();
//						wifiDialog.postDelayed(new Runnable() {
//							@Override
//							public void run() {
//								if (wifi.isWifiEnabled()) {
//									if (info.getExtraInfo() != null && info.getExtraInfo().equals("<unknown ssid>")) {
//										new WifiConnector(context).forgetCurrent();
//									}
//									if (isDisconnectionHintNeeded) {
//										showHint(getResources().getString(R.string.hint_wifi_disconnected));
//									}
//								} else {
//									wifi.setWifiEnabled(true);
//								}
//								scanWifi();
//							}
//
//						}, 1000);
//					} else if (connected && details == NetworkInfo.DetailedState.CONNECTED) {
//						WifiRedirectionTask redirectionTask = new WifiRedirectionTask() {
//							@Override
//							protected void onPostExecute(Boolean result) {
//								if (result) {
//									onConnected();
//									wifiDialog.hide();;
//									isDisconnectionHintNeeded = true;
//									showHint(getResources().getString(R.string.hint_wifi_connected));
//								} else {
////									isDisconnectionHintNeeded = true;
//									showHint(getResources().getString(R.string.hint_failed_to_connecn));
//									resetWifi();
//								}
//							}
//						};
//						redirectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//						resetPass = 0;
//					} else if (!visible && !connected && !connecting) {
//						getActionBar().hide();
//						if (details != NetworkInfo.DetailedState.SCANNING) {
//							scanWifi();
//						}
//					}
//
//				}
//			}
//		};
//	}

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

	private TimerTask mailPollTask = new TimerTask() {
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
					//TODO runOnUiThread, scanWifi
					error.hide();
				}
			}
		}
	};

	private BlurActionBar actionBar;
	private ViewPager pager;
	private ProgressBar progressBar;
	private BlurTextView seeNewPhoto, error;
	private HintTextView hint;
	private BlurListView wifiList;
	private Gallery gallery;
	private DeletingDialog deletingDialog;
	private EmailChangeDialog emailChangeDialog;
	private HelpDialog helpDialog;
	private WifiDialog wifiDialog;

	private Context context;
	private DatabaseHelper databaseHelper;
	private WifiReceiver wifi;
	private ImagePagerAdapter pagerAdapter;
	private Timer mailTimer = new Timer();
	private boolean isFreeze = false;
	private boolean galleryHideSeeNewPhoto;

	@Override
	public void onBackPressed() {
		//TODO
//		if (wifiConnectionHandler.isConnectionDialogVisible()) {
//			wifiConnectionHandler.hideConnectionDialog();
//		} else {
		super.onBackPressed();
//		}
	}

	private void initViews() {
		createActionBar();
		pager = (ViewPager) findViewById(R.id.pager);
		progressBar = (ProgressBar) findViewById(R.id.progressLoading);
		seeNewPhoto = (BlurTextView) findViewById(R.id.see_new_photos);
		error = (BlurTextView) findViewById(R.id.error_pane);
		hint = (HintTextView) findViewById(R.id.hint);
		wifiList = (BlurListView) findViewById(R.id.wifi_list);
		gallery = (Gallery) findViewById(R.id.gallery1);
		deletingDialog = (DeletingDialog) findViewById(R.id.dialog_deleting);
		emailChangeDialog = (EmailChangeDialog) findViewById(R.id.dialog_change_email);
		helpDialog = (HelpDialog) findViewById(R.id.dialog_help);
		wifiDialog = (WifiDialog) findViewById(R.id.dialog_wifi);
	}

	private void init() {
		context = this;
		getActionBar().hide();
		mailTimer.scheduleAtFixedRate(mailPollTask, 0, REFRESH_RATE);
		databaseHelper = DatabaseHelper.getInstance(getApplicationContext());
		wifi = WifiReceiver.createInstance(this, this);
		wifi.registerWifiBroadcast(true);
		pagerAdapter = createAdapter();
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
		((FreezeViewPager)pager).setSwipeValidator(new FreezeViewPager.SwipeValidator() {
			@Override
			public boolean isSwipeBlocked() {
				return isBlocked();
			}
		});
		wifiList.setOnItemClickListener(new BlurListView.OnItemClickListener() {
			@Override
			public void onItemClick(ScanResult item) {
				wifi.connectToNetwork(item);
			}
		});
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
		deletingDialog.setPositiveButtonListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pagerAdapter.deleteCurrentItem(pager);
				deletingDialog.hide();
				checkForNoPhotos();
			}
		});
		seeNewPhoto.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				seeNewPhoto.hide();
				pagerAdapter.setHasNewPhotos(false);
				showNewPhotos();
			}
		});
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Crashlytics.start(this);
		setContentView(R.layout.activity_photo);
		initViews();
		/** uncomment if newEmailWizard is needed*/
//		newEmail = new NewEmailWizard();
		if (!(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && GlimpseApp.getPicturesDir().canWrite())) {
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
		init();
		pagerAdapter.checkNewPhotos();
		if (pagerAdapter.hasNewPhotos() && isConnected()) recreateSeeNewPhoto();
		if (getUser() == null) {
			error.show(R.string.error_no_user_data);
			return;
		}
		if (pagerAdapter.getCount() >= SCREEN_PAGE_LIMIT) rescheduleImageSwipe();
	}

	private void checkForNoPhotos() {
		if (GlimpseApp.getFileHandler().isEmpty() && getUser() != null) {
			error.show(getString(R.string.error_no_photo, getUser()));
		}
	}

	//TODO
	private void recreateSeeNewPhoto() {
		if (gallery != null && gallery.getVisibility() == View.VISIBLE) return;
		seeNewPhoto.setVisibility(View.VISIBLE);
		final Animation animation = AnimationUtils.loadAnimation(PhotoActivity.this, R.anim.image_alpha);
		seeNewPhoto.postDelayed(new Runnable() {
			@Override
			public void run() {
				seeNewPhoto.setVisibility(View.VISIBLE); // need to invalidate background
				Animation anim = animation;
				anim.setDuration(anim.getDuration() / 2);
				seeNewPhoto.startAnimation(anim);
			}
		}, animation.getDuration());
	}

	@Override
	protected void onDestroy() {
		wifi.registerWifiBroadcast(false);
		mailTimer.cancel();
		super.onDestroy();
	}

	private ImagePagerAdapter createAdapter() {
		return new ImagePagerAdapter(this, databaseHelper, createOnClickListener());
	}

	private ImagePagerAdapter createAdapter(PictureData pictureData) {
		return new ImagePagerAdapter(this, pictureData, databaseHelper, createOnClickListener());
	}

	private OnClickListener createOnClickListener() {
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
				error,
				hint,
				/** uncomment if newEmail is needing*/
//			newEmail.dialog,
				helpDialog,
				deletingDialog,
				wifiDialog,
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
//					popupWindow.dismiss();
//					getActionBar().hide();
//					emailChangeDialog.show();
//				}
//			});
			layout.findViewById(R.id.reset_wifi).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					popupWindow.dismiss();
					wifi.resetCurrentWifi();
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

	private void rescheduleImageSwipe() {
		pager.removeCallbacks(swipeRunnable);
		pager.postDelayed(swipeRunnable, RESCHEDULE_REFRESH_RATE);
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

	public void showHint(String text) {
		hint.show(text);
	}

	@Override
	public void onConnecting() {
		progressBar.setVisibility(View.VISIBLE);
		wifiList.hide(false);
		wifiDialog.hide();
		error.hide();
		hint.hide();
	}

	@Override
	public void onConnected() {
		progressBar.setVisibility(View.GONE);
		wifiList.hide(false);
		wifiDialog.hide();
		error.hide();
		hint.show(R.string.hint_wifi_connected);
		checkForNoPhotos();
	}

	@Override
	public void onDisconnected(WifiReceiver.WifiError wifiError) {
		progressBar.setVisibility(View.GONE);
		error.hide();
		hint.hide();
		getActionBar().hide();
		if (WifiReceiver.WifiError.NEED_PASSWORD.equals(wifiError)) {
			wifiDialog.show();
		} else {
			wifiList.show();
			wifi.scanWifi();
			if (WifiReceiver.WifiError.CONNECT_ERROR.equals(wifiError)) {
				hint.show(R.string.hint_failed_to_connect);
			} else if (WifiReceiver.WifiError.DISCONNECTED.equals(wifiError)) {
				hint.show(R.string.hint_wifi_disconnected);
			} else {
				hint.show(R.string.hint_wifi_error);
			}
		}
	}

	@Override
	public void onScanning() {
		if (wifi.isConnectedOrConnecting() || wifi.isConnected()) return;
		progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void onScansResultReceive(List<ScanResult> scanResults) {
		wifiList.update(scanResults);
		if (wifi.isConnectedOrConnecting() || wifi.isConnected() || wifiDialog.getVisibility() == View.VISIBLE) return;
		progressBar.setVisibility(View.GONE);
//		hint.hide();
//		error.hide();
		wifiList.show();
	}

}