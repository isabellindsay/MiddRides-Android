package com.middleendien.midd_rides.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.middleendien.midd_rides.R;
import com.middleendien.midd_rides.models.User;
import com.middleendien.midd_rides.utils.HardwareUtil;
import com.middleendien.midd_rides.utils.NetworkUtil;
import com.middleendien.midd_rides.Privacy;
import com.middleendien.midd_rides.utils.UserUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import cn.pedant.SweetAlert.SweetAlertDialog;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by Peter on 10/5/15.
 *
 * Activity where user registers
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private AutoCompleteTextView emailBox;
    private EditText passwordBox;
    private EditText passwordConfirmBox;

    private Button registerButton;

    public static final int REGISTER_SUCCESS_CODE = 0x101;
    public static final int REGISTER_FAILURE_CODE = 0x102;

    private SweetAlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_screen);

        initAnim();

        initView();

        initEvent();
    }

    private void initAnim() {
        if (Build.VERSION.SDK_INT > 21) {
            getWindow().setEnterTransition(new Slide(Gravity.END)
                    .excludeTarget(android.R.id.statusBarBackground, true)
                    .excludeTarget(android.R.id.navigationBarBackground, true));

            getWindow().setReturnTransition(new Slide(Gravity.END)
                    .excludeTarget(android.R.id.statusBarBackground, true)
                    .excludeTarget(android.R.id.navigationBarBackground, true));
        }
    }

    private void initView() {
        emailBox = (AutoCompleteTextView) findViewById(R.id.register_email);
        passwordBox = (EditText) findViewById(R.id.register_passwd);
        passwordConfirmBox = (EditText) findViewById(R.id.register_passwd_confirm);

        registerButton = (Button) findViewById(R.id.register_register);
    }

    private void initEvent() {
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = emailBox.getText().toString();
                final String password = passwordBox.getText().toString();
                String confirm = passwordConfirmBox.getText().toString();

                // check e-mail validity
                if (!UserUtil.isEmailValid(email)) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.incorrect_email_format), Toast.LENGTH_SHORT).show();
                    return;
                }

                // check password match
                if (!password.equals(confirm)) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.password_dont_match), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!HardwareUtil.isNetworkAvailable(getApplicationContext())){
                    Toast.makeText(RegisterActivity.this, getResources().getString(R.string.no_internet_warning), Toast.LENGTH_SHORT).show();
                    return;
                }

                setDialogShowing(true);
                NetworkUtil.getInstance().register(email, Privacy.encodePassword(password), RegisterActivity.this, new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        setDialogShowing(false);
                        try {
                            JSONObject body;
                            if (!response.isSuccessful()) {     // not successful
                                body = new JSONObject(response.errorBody().string());
                                Toast.makeText(RegisterActivity.this, body.getString(getString(R.string.res_param_error)), Toast.LENGTH_SHORT).show();
                                Log.d(TAG, body.toString());
                            } else {                            // register success
                                body = new JSONObject(response.body().string());
                                // save to local storage
                                UserUtil.setCurrentUser(RegisterActivity.this, new User(
                                        email,
                                        Privacy.encodePassword(password),
                                        body.getJSONObject(getString(R.string.res_param_user)).getBoolean(getString(R.string.res_param_verified))));
                                setResult(REGISTER_SUCCESS_CODE);
                                finish();
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        setDialogShowing(false);
                        t.printStackTrace();
                        Toast.makeText(RegisterActivity.this, getString(R.string.failed_to_talk_to_server), Toast.LENGTH_SHORT).show();
                    }
                });

                hideKeyboard();
            }
        });

        emailBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().length() > 0 && s.charAt(s.length() - 1) == '@') {             // ends with "@"
                    ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<String>(
                            RegisterActivity.this,
                            android.R.layout.simple_dropdown_item_1line, new String[]{s + "middlebury.edu"});
                    emailBox.setAdapter(autoCompleteAdapter);
                } else if (s.toString().length() > 2 && !s.toString().contains("@")) {         // "sth"
                    ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<String>(
                            RegisterActivity.this,
                            android.R.layout.simple_dropdown_item_1line, new String[]{s + "@middlebury.edu"});
                    emailBox.setAdapter(autoCompleteAdapter);
                } else if (s.toString().length() > 15 && s.toString().substring(s.length() - 15).equals("@middlebury.edu")) {
                    // completed format
                    emailBox.dismissDropDown();
                } else if (s.toString().length() == 0) {
                    // cleared everything or initial state, without @
                    emailBox.setAdapter(null);
                }   // else do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * Displays an animation while a background process is taking place
     * @param showing
     */
    private void setDialogShowing(boolean showing) {
        if (showing) {
            progressDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
                    .setTitleText(getString(R.string.dialog_registering));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.showCancelButton(false);
            progressDialog.getProgressHelper().setBarColor(ContextCompat.getColor(this, R.color.colorAccent));
            progressDialog.show();
        } else {
            if (progressDialog.isShowing())
                progressDialog.dismissWithAnimation();
        }
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            // again, don't worry
        }
    }

    /**
     * Automatically hide keyboard if touches background
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hideKeyboard();
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(REGISTER_FAILURE_CODE);
        super.onBackPressed();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
