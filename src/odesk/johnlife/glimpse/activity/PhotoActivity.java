package odesk.johnlife.glimpse.activity;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.R.id;
import odesk.johnlife.glimpse.R.layout;
import odesk.johnlife.glimpse.util.SystemUiHider;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class PhotoActivity extends Activity {
	private static final int[] pictures = {R.drawable.wp1, R.drawable.wp2, R.drawable.wp3, R.drawable.wp4, R.drawable.wp5, R.drawable.wp6};
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	private Bitmap activeImage = null;
	private int activeIndex = -1;
	private ImageView top;
	private ImageView base;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);
		final View contentView = findViewById(android.R.id.content);
		top = (ImageView) findViewById(R.id.top);
		base = (ImageView) findViewById(R.id.base);

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (mControlsHeight == 0) {
							mControlsHeight = controlsView.getHeight();
						}
						if (mShortAnimTime == 0) {
							mShortAnimTime = getResources().getInteger(
									android.R.integer.config_shortAnimTime);
						}
						controlsView
								.animate()
								.translationY(visible ? 0 : mControlsHeight)
								.setDuration(mShortAnimTime);
						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		contentView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);
		swipeImage();
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	private Runnable swipeRunnable = new Runnable() {
		@Override
		public void run() {
			swipeImage();
		}
	};

	
	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	
	private void swipeImage() {
		if (null != activeImage) {
			top.setImageBitmap(activeImage);
//			top.setTranslationX(0);
			top.setAlpha(1f);
			top.animate()/*.translationX(-top.getWidth())*/.alpha(0f).setDuration(600).start();
		}
		if (++activeIndex >= pictures.length) activeIndex = 0;
		Bitmap newBitmap = BitmapFactory.decodeResource(getResources(), pictures[activeIndex]);
		base.setImageBitmap(newBitmap);
		activeImage = newBitmap;
		base.postDelayed(swipeRunnable, 5000);
	}
}
