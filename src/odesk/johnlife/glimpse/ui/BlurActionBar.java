package odesk.johnlife.glimpse.ui;

import java.io.File;

import odesk.johnlife.glimpse.R;
import android.app.ActionBar;
import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class BlurActionBar {
	private ActionBar actionBar;
	private View customActionBar;
	private boolean isFreeze = false;
	private OnActionClick listener;
	
	public interface OnActionClick {
		void onClick(View v);
	}
	
	public void setOnActionClickListener(OnActionClick listener) {
		this.listener = listener;
	}
	
	public BlurActionBar(Activity activity) {
		this.actionBar = activity.getActionBar();
		customActionBar = activity.getLayoutInflater().inflate(R.layout.custom_bar, new LinearLayout(activity), false);
		actionBar.hide();
		actionBar.setCustomView(customActionBar);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		createActionButtons();
	}
	
	private void createActionButtons() {
		View deleteActionView = customActionBar.findViewById(R.id.action_delete);
		View freezeActionView = customActionBar.findViewById(R.id.action_freeze);
		View resetActionView = customActionBar.findViewById(R.id.action_reset_wifi);
		
		deleteActionView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				actionBar.hide();
				listener.onClick(v);
			}
		});
		freezeActionView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				actionBar.hide();
				chageFreezeFrame(isFreeze ? false : true, v);
				listener.onClick(v);
			}
		});
		resetActionView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				actionBar.hide();
				listener.onClick(v);
			}
		});
	}
	
	private void chageFreezeFrame(boolean isFreeze, View view) {
		this.isFreeze = isFreeze;
		TextView textFreeze = (TextView) view.findViewById(R.id.text_freeze);
		ImageView imageFreeze = (ImageView) view.findViewById(R.id.image_freeze);
		textFreeze.setText(isFreeze ? R.string.action_unfreeze : R.string.action_freeze);
		imageFreeze.setImageResource(isFreeze ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause);
	}
	
	public boolean isFreeze() {
		return isFreeze;
	}

}