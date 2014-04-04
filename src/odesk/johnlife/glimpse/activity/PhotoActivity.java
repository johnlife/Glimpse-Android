package odesk.johnlife.glimpse.activity;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.util.SystemUiHider;
import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

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
			if (progress.getVisibility() == View.VISIBLE) {
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

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo);

		contentView = findViewById(android.R.id.content);
		top = (ImageView) findViewById(R.id.top);
		base = (ImageView) findViewById(R.id.base);
		progress = (ProgressBar) findViewById(R.id.progress);
		progress.setRotation(-90);

		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.hide();
		contentView.post(hiderAction);
		contentView.setOnTouchListener(touchListener);
		swipeImage();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
}
