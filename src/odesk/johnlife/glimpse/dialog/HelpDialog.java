package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import odesk.johnlife.glimpse.R;

public class HelpDialog extends BlurDialog {
    public HelpDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public HelpDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HelpDialog(Context context) {
        super(context);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        setTitle(R.string.how_it_works);
        //TODO
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(getScreenWidth(0.75), getScreenHeight(0.75), Gravity.CENTER);
//        content.setLayoutParams(params);
        container.addView(inflate(context, R.layout.dialog_help, null));
        negativeButton.setVisibility(View.GONE);
        setPositiveButtonListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });
        //TODO
//        TextView textEmail = (TextView) findViewById(R.id.textEmail);
//        SpannableStringBuilder string = new SpannableStringBuilder(getString(R.string.how_it_works_email) + " " + getUser());
//        string.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), getString(R.string.how_it_works_email).length(), string.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        textEmail.setText(string);

    }
}
