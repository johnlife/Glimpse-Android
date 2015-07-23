package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import odesk.johnlife.glimpse.R;

public class DeletingDialog extends BlurDialog {

    public DeletingDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DeletingDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DeletingDialog(Context context) {
        super(context);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        setTitle(R.string.dialog_delete);
        setPositiveButtonText(R.string.button_delete);
        container.setVisibility(View.GONE);
        setDrawable(positiveButton, android.R.drawable.ic_menu_delete);
        setDrawable(negativeButton, android.R.drawable.ic_menu_revert);
    }

}