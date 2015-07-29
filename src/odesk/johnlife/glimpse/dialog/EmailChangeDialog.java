package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;

public class EmailChangeDialog extends BlurDialog {

    private View step1, step2;
    private EditText emailView;
    private TextView error;
    private OnClickListener listener;

    public EmailChangeDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public EmailChangeDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmailChangeDialog(Context context) {
        super(context);
    }

    @Override
    public void setPositiveButtonListener(OnClickListener listener) {
        this.listener = listener;
    }

    private void goToFirstStep() {
        setTitle(R.string.title_new_email_step1);
        step1.setVisibility(View.VISIBLE);
        step2.setVisibility(View.GONE);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSecondStep();
            }
        });
    }

    private void goToSecondStep() {
        setTitle(R.string.title_new_email_step2);
        step1.setVisibility(View.GONE);
        step2.setVisibility(View.VISIBLE);
        error.setVisibility(View.GONE);
        positiveButton.setOnClickListener(listener);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        View view = inflate(context, R.layout.dialog_change_email, container);
        step1 = content.findViewById(R.id.step1);
        step2 = content.findViewById(R.id.step2);
        emailView = (EditText) step2.findViewById(R.id.email);
        error = (TextView) step2.findViewById(R.id.error);
        negativeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (step2.getVisibility() == View.VISIBLE) {
                    goToFirstStep();
                } else {
                    hide();
                }
            }
        });
        goToFirstStep();
//        step2.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                error.setVisibility(View.GONE);
//                String email = emailView.getText().toString();
//                if (email.isEmpty()) {
//                    showError(R.string.error_email_empty);
//                    return;
//                }
//                String fullEmail = email + "@glimpseframe.com";
//                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(fullEmail).matches()) {
//                    showError(R.string.error_email_invalid);
//                    return;
//                }
////                try {
////                    FileWriter out = new FileWriter(getUserDataFile());
////                    out.write(fullEmail);
////                    out.close();
////                    restart();
////                } catch (IOException e) {
//////					PushLink.sendAsyncException(e);
////                }
//            }
//
//            void showError(int errorId) {
//                error.setText(errorId);
//                error.setVisibility(View.VISIBLE);
//            }
//        });
    }

//    private int getScreenWidth(double coefficient) {
//        return (int) (getScreenSize().x * coefficient);
//    }
//
//    private int getScreenHeight(double coefficient) {
//        return (int) (getScreenSize().y * coefficient);
//    }
//
//    private Point getScreenSize() {
//        Display display = getWindowManager().getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        return size;
//    }

}