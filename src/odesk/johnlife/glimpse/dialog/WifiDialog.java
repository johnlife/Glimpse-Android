package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;

public class WifiDialog extends BlurDialog {

    public interface OnButtonsClickListener {
        void onConnect(ScanResult network, String password);
        void onCancel();
    }

    private EditText password;
    private ScanResult network;
    private OnButtonsClickListener listener;

    public WifiDialog(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WifiDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WifiDialog(Context context) {
        super(context);
    }

    @Override
    protected void createView(Context context) {
        super.createView(context);
        View view = inflate(context, R.layout.wifi_dialog, container);
        password = (EditText) view.findViewById(R.id.password);
        CheckBox showPassword = (CheckBox) view.findViewById(R.id.is_password_visible);
        showPassword.setVisibility(View.GONE); //TODO
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    connect();
                    return true;
                }
                return false;
            }
        });
        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                password.setInputType(InputType.TYPE_CLASS_TEXT |
                        (isChecked ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            }
        });
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        negativeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });
        positiveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        setPositiveButtonText(R.string.button_connect);
    }

    @Override
    protected OnTouchListener getOutsideListener() {
        return new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                cancel();
                return true;
            }
        };
    }

    private void connect() {
        hide();
        listener.onConnect(network, password.getText().toString());
    }

    private void cancel() {
        hide();
        listener.onCancel();
    }

    public void setOnConnectClickListener(OnButtonsClickListener listener) {
        this.listener = listener;
    }

    public void show(ScanResult network) {
        this.network = network;
        password.setText("");
        setTitle(network.SSID);
        show();
    }

}