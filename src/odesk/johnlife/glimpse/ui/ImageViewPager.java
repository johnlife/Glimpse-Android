package odesk.johnlife.glimpse.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ImageViewPager extends ViewPager {
	private boolean swipeable = true;
	
	public ImageViewPager(Context context) {
		super(context);
	}
	
	public ImageViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	// Call this method in your motion events when you want to disable or enable
    // It should work as desired.
    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        return (this.swipeable) ? super.onTouchEvent(arg0) : false; 
    }

}
