package odesk.johnlife.glimpse.dialog;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import odesk.johnlife.glimpse.R;

public class WifiDialog extends BlurDialog {

    private EditText password;

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
        showPassword.setVisibility(View.INVISIBLE); //TODO
        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //TODO
//                    connectToNetwork(password.getText().toString());
//                    hideConnectionDialog();
//                    if (!isConnectedOrConnecting()) {
//                        showHint(getResources().getString(R.string.hint_wifi_error));
//                    }
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
        view.findViewById(R.id.connect).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO
//                        hideConnectionDialog();
//                        connectToNetwork(password.getText().toString());
//                        if (!isConnectedOrConnecting() && progressBar.getVisibility() == View.GONE) {
//                            showHint(getResources().getString(R.string.hint_wifi_error));
//                        }
                    }
                }
        );
    }

    public void show(String networkName) {
        setTitle(networkName);
        show();
    }

}