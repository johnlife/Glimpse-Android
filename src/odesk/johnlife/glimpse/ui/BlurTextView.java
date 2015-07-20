package odesk.johnlife.glimpse.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;

public class BlurTextView extends BlurLayout {

    protected TextView textView;

    public BlurTextView(Context context) {
        super(context);
        createView(context);
    }

    public BlurTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        createView(context);
    }

    public BlurTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createView(context);
    }

    private void createView(Context context) {
        textView = (TextView) inflate(context, R.layout.text_view, this);
    }

    public void show(String text) {
        textView.setText(text);
        setVisibility(View.VISIBLE);
    }

    public void hide() {
        setVisibility(View.GONE);
    }

}
