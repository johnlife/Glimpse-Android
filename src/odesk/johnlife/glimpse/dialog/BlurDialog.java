package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.ui.BlurLayout;

public abstract class BlurDialog extends BlurLayout {

    protected TextView title;
    protected View buttons;
    protected FrameLayout container;
    protected View content;
    protected Button positiveButton, negativeButton;

    public BlurDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createView(context);
    }

    public BlurDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView(context);
    }

    public BlurDialog(Context context) {
        super(context);
        createView(context);
    }

    public void setPositiveButtonListener(OnClickListener listener) {
        positiveButton.setOnClickListener(listener);
    }

    protected void createView(Context context) {
        inflate(context, R.layout.dialog, this);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hide();
                return true;
            }
        });
        content = findViewById(R.id.content);
        container = (FrameLayout) content.findViewById(R.id.container);
        title = (TextView) content.findViewById(R.id.title);
        buttons = content.findViewById(R.id.buttons);
        positiveButton = (Button) buttons.findViewById(R.id.ok);
        negativeButton = (Button) buttons.findViewById(R.id.cancel);
        negativeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
    }

    protected void setTitle(String text) {
        title.setText(text);
    }

    protected void setTitle(int stringId) {
        title.setText(stringId);
    }

    protected void setPositiveButtonText(int stringId) {
        positiveButton.setText(stringId);
    }

    protected void setNegativeButtonText(int stringId) {
        negativeButton.setText(stringId);
    }

    public void show() {
        setVisibility(View.VISIBLE);
        content.setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
        content.setVisibility(View.GONE);
    }
}
