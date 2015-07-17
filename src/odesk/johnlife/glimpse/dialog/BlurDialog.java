package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;
import odesk.johnlife.glimpse.ui.BlurLayout;

public class BlurDialog extends BlurLayout {

    private TextView title;
    private FrameLayout container;

    public BlurDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BlurDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurDialog(Context context) {
        super(context);
    }

    private void createView(Context context) {
        inflate(context, R.layout.dialog, this);
    }

    public void show() {
        setVisibility(View.VISIBLE);
    }

    public void hide() { setVisibility(View.GONE) ;}
}
