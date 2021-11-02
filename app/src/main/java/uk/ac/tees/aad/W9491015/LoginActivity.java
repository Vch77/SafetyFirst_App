package uk.ac.tees.aad.W9491015;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.iid.FirebaseInstanceId;
//import com.google.firebase.iid.InstanceIdResult;


public class LoginActivity extends AppCompatActivity {

    View contextView;

    String loginWith;

    TextInputEditText email;
    TextInputEditText phone;
    TextInputEditText password;
    MaterialCardView loginButton;
    TextView goToRegistration;

    FirebaseAuth mAuth;
    DatabaseReference mDatabaseReference;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        contextView = findViewById(R.id.context_view);

        email = findViewById(R.id.emailField);
        phone = findViewById(R.id.phoneField);
        password = findViewById(R.id.passwordField);
        loginButton = findViewById(R.id.btnLogin);
        goToRegistration = findViewById(R.id.register);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users");

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setTitle("Logging in");
        progressDialog.setMessage("Please wait while we check your credentials...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        email.addTextChangedListener(textWatcher);
        phone.addTextChangedListener(textWatcher);

        goToRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(getApplicationContext(),RegistrationActivity.class);
                startActivity(registerIntent);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(email.getText().toString()) && TextUtils.isEmpty(phone.getText().toString())) {
                    Snackbar.make(contextView,"Enter atleast email or phone", Snackbar.LENGTH_LONG).show();
                }

                else if (TextUtils.isEmpty(password.getText().toString())) {
                    password.requestFocus();
                }

                else {

                    progressDialog.show();

                    String dataToMatch = (loginWith.equals("Email")) ? email.getText().toString().trim() : phone.getText().toString().trim();

                    mDatabaseReference.orderByChild(loginWith).equalTo(dataToMatch)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() != null) {

                                        if(loginWith.equals("Email"))
                                            signinAccountWithDetails();
                                        else
                                            goToOTPVerification(dataToMatch);
                                    }
                                    else {
                                        progressDialog.dismiss();
                                        Snackbar.make(contextView,"Account does not exist",Snackbar.LENGTH_LONG).show();
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });
                }
            }
        });
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String emailInput = email.getText().toString().trim();
            String phoneInput = phone.getText().toString().trim();
            if(!emailInput.isEmpty()) {
                //phone.setText("");
                phone.setEnabled(false);
                loginWith = "Email";
            }
            else if(!phoneInput.isEmpty()) {
                //email.setText("");
                email.setEnabled(false);
                loginWith = "Phone";
            }
            else {
                phone.setEnabled(true);
                email.setEnabled(true);
                loginWith = "";
            }
            loginButton.setEnabled(!emailInput.isEmpty() || !phoneInput.isEmpty());
        }
        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private void signinAccountWithDetails(){
        mAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            progressDialog.dismiss();
                            startActivity(new Intent(LoginActivity.this,MainActivity.class));
                            finish();

                        } else {
                            progressDialog.dismiss();
                            Snackbar.make(getCurrentFocus(),"Sign-in Failed. Check credentials",Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void goToOTPVerification(String phone) {
        Intent otpVerification = new Intent(getApplicationContext(), OtpVerificationActivity.class);
        otpVerification.putExtra("Type","Login");
        otpVerification.putExtra("Phone",phone);
        startActivity(otpVerification);
    }
}
