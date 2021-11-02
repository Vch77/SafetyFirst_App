package uk.ac.tees.aad.W9491015;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OtpVerificationActivity extends AppCompatActivity {

    View contextView;

    FirebaseAuth mAuth;
    DatabaseReference mUserDatabase;

    EditText otp1,otp2,otp3,otp4,otp5,otp6;
    TextView back,countDown,resentOtp;
    MaterialCardView submit;

    String type, name, phoneNumber;

    //countdown timer declaration
    long seconds,    minutes, millisRemaining;
    CountDownTimer mCountDownTimer = null;

    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        contextView = findViewById(R.id.context_view);

        type = getIntent().getStringExtra("Type");
        name = getIntent().getStringExtra("Name");
        phoneNumber = getIntent().getStringExtra("Phone");

        otp1 = findViewById(R.id.etOTP1); otp1.addTextChangedListener(new GenericTextWatcher(otp1));
        otp2 = findViewById(R.id.etOTP2); otp2.addTextChangedListener(new GenericTextWatcher(otp2));
        otp3 = findViewById(R.id.etOTP3); otp3.addTextChangedListener(new GenericTextWatcher(otp3));
        otp4 = findViewById(R.id.etOTP4); otp4.addTextChangedListener(new GenericTextWatcher(otp4));
        otp5 = findViewById(R.id.etOTP5); otp5.addTextChangedListener(new GenericTextWatcher(otp5));
        otp6 = findViewById(R.id.etOTP6); otp6.addTextChangedListener(new GenericTextWatcher(otp6));

        back = findViewById(R.id.btnBack);
        countDown = findViewById(R.id.countDownText);
        resentOtp = findViewById(R.id.tvResendOTP); resentOtp.setVisibility(View.INVISIBLE);
        submit = findViewById(R.id.btnSubmit);

        mProgress = new ProgressDialog(OtpVerificationActivity.this);

        mAuth = FirebaseAuth.getInstance();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        sendLoginOtp();

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        resentOtp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                resendVerificationCode(mResendToken);

                resentOtp.setVisibility(View.INVISIBLE);
                countDown.setVisibility(View.VISIBLE);
                startCountDown();

            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String eotp = otp1.getText().toString() + otp2.getText().toString() + otp3.getText().toString() + otp4.getText().toString()
                        + otp5.getText().toString() + otp6.getText().toString();
                verifyCode(eotp);

            }
        });

    }

    private void resendVerificationCode(PhoneAuthProvider.ForceResendingToken mResendToken) {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                mResendToken);

    }

    private void sendLoginOtp() {

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);
        startCountDown();
    }

    public void startCountDown(){
        mCountDownTimer = new CountDownTimer(60000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                millisRemaining = millisUntilFinished;

                seconds = (long) (millisUntilFinished / 1000);
                minutes = seconds / 60;
                seconds = seconds % 60;
                countDown.setText(String.format("%02d", minutes) + ":" + String.format("%02d", seconds));

            }

            @Override
            public void onFinish() {
                //Do what you want
                countDown.setVisibility(View.INVISIBLE);
                resentOtp.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void verifyCode(String code) {
        PhoneAuthCredential credentials = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithOtpCredentials(credentials);
    }

    private void signInWithOtpCredentials(PhoneAuthCredential credentials) {

        mProgress.setTitle("Checking the OTP");
        mProgress.setMessage("Please wait while we check the entered OTP code...");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mAuth.signInWithCredential(credentials).addOnCompleteListener(this,new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull final Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            mProgress.dismiss();
                            if(type.equals("Login")) {

                                Intent loginIntent = new Intent(OtpVerificationActivity.this,MainActivity.class);
                                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginIntent);
                                finish();

                            }
                            else {
                                registerUserWithDetails();
                            }
                        } else {
                            mProgress.dismiss();
                            Snackbar.make(contextView,"Kindly check the OTP you have entered",Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void registerUserWithDetails() {

        Map<String, Object> userRegistration = new HashMap<>();
        userRegistration.put("Name",name);
        userRegistration.put("Phone",phoneNumber);
        mUserDatabase.child(mAuth.getCurrentUser().getUid()).setValue(userRegistration).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Intent registerIntent = new Intent(OtpVerificationActivity.this,MainActivity.class);
                    registerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(registerIntent);
                    finish();
                }
                else{
                    Snackbar.make(contextView,"Error occurred. Try again later",Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {

            String sentCode = phoneAuthCredential.getSmsCode();

            if (sentCode != null) {
                verifyCode(sentCode);
            }
            else {
                signInWithOtpCredentials(phoneAuthCredential);
            }
        }

        @Override
        public void onCodeSent(@NonNull String s,@NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);

            mVerificationId = s;
            mResendToken = forceResendingToken;

        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            Toast.makeText(OtpVerificationActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                // Invalid request
                Toast.makeText(OtpVerificationActivity.this, "Invalid phone number", Toast.LENGTH_SHORT).show();
            } else if (e instanceof FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
                Toast.makeText(OtpVerificationActivity.this, "Quota exceeded", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public class GenericTextWatcher implements TextWatcher {
        private View view;
        private GenericTextWatcher(View view)
        {
            this.view = view;
        }
        @Override
        public void afterTextChanged(Editable editable) {
            // TODO Auto-generated method stub
            String text = editable.toString();
            switch (view.getId()) {

                case R.id.etOTP1:
                    if (text.length() > 1) {
                        otp1.setText(String.valueOf(text.charAt(0)));
                        otp2.setText(String.valueOf(text.charAt(1)));
                        otp2.requestFocus();
                        otp2.setSelection(otp2.getText().length());
                    }
                    break;

                case R.id.etOTP2:
                    if (text.length() > 1){
                        otp2.setText(String.valueOf(text.charAt(0)));
                        otp3.setText(String.valueOf(text.charAt(1)));
                        otp3.requestFocus();
                        otp3.setSelection(otp3.getText().length());
                    }
                    if (text.length() == 0){
                        otp1.requestFocus();
                        otp1.setSelection(otp1.getText().length());
                    }
                    break;

                case R.id.etOTP3:
                    if (text.length() > 1){
                        otp3.setText(String.valueOf(text.charAt(0)));
                        otp4.setText(String.valueOf(text.charAt(1)));
                        otp4.requestFocus();
                        otp4.setSelection(otp4.getText().length());
                    }
                    if (text.length() == 0){
                        otp2.requestFocus();
                        otp2.setSelection(otp2.getText().length());
                    }
                    break;

                case R.id.etOTP4:
                    if (text.length() > 1){
                        otp4.setText(String.valueOf(text.charAt(0)));
                        otp5.setText(String.valueOf(text.charAt(1)));
                        otp5.requestFocus();
                        otp5.setSelection(otp5.getText().length());
                    }
                    if (text.length() == 0){
                        otp3.requestFocus();
                        otp3.setSelection(otp3.getText().length());
                    }
                    break;

                case R.id.etOTP5:
                    if (text.length() > 1){
                        otp5.setText(String.valueOf(text.charAt(0)));
                        otp6.setText(String.valueOf(text.charAt(1)));
                        otp6.requestFocus();
                        otp6.setSelection(otp6.getText().length());
                    }
                    if (text.length() == 0){
                        otp4.requestFocus();
                        otp4.setSelection(otp4.getText().length());
                    }
                    break;

                case R.id.etOTP6:
                    if (text.length() == 0){
                        otp5.requestFocus();
                        otp5.setSelection(otp5.getText().length());
                    }
                    if (text.length() > 0){
                        String eotp = otp1.getText().toString() + otp2.getText().toString()
                                + otp3.getText().toString() + otp4.getText().toString()
                                + otp5.getText().toString() + otp6.getText().toString();

                        verifyCode(eotp);
                    }
                    break;
            }
        }
        @Override
        public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }

        @Override
        public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        }
    }

}
