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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class RegistrationActivity extends AppCompatActivity {

    String registerWith;

    View contextView;

    TextInputEditText name;
    TextInputEditText email;
    TextInputEditText phone;
    TextInputEditText password, confirmpassword;
    MaterialCardView registerButton;
    TextView goToLogin;
    MaterialTextView passwordwarning;

    FirebaseAuth mAuth;
    DatabaseReference mDatabaseReference;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        contextView = findViewById(R.id.context_view);

        name = findViewById(R.id.nameField);
        email = findViewById(R.id.emailField);
        phone = findViewById(R.id.phoneField);
        password = findViewById(R.id.passwordField);
        confirmpassword = findViewById(R.id.confirmpasswordField);
        registerButton = findViewById(R.id.btnRegister);
        goToLogin = findViewById(R.id.login);
        passwordwarning = findViewById(R.id.passwordWarning);

        email.addTextChangedListener(textWatcher);
        phone.addTextChangedListener(textWatcher);

        mAuth = FirebaseAuth.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users");

        goToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(loginIntent);
                finish();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(name.getText().toString())) {
                    name.setError("Enter Name");
                    name.requestFocus();
                }

                else if (TextUtils.isEmpty(email.getText().toString()) && TextUtils.isEmpty(phone.getText().toString())) {
                    Snackbar.make(contextView,"Enter atleast email or phone", Snackbar.LENGTH_LONG).show();
                }

                else if (TextUtils.isEmpty(password.getText().toString())) {
                    password.requestFocus();
                }

                else if (password.getText().toString().length() < 6) {
                    passwordwarning.setVisibility(View.VISIBLE);
                }

                else if (!password.getText().toString().equals(confirmpassword.getText().toString())) {
                    Snackbar.make(contextView,"Passwords not matching. Kindly re-check", Snackbar.LENGTH_LONG).show();
                }

                else {

                    String dataToMatch = (registerWith.equals("Email")) ? email.getText().toString().trim() : phone.getText().toString().trim();

                    mDatabaseReference.orderByChild(registerWith).equalTo(dataToMatch)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.getValue() != null) {
                                        Snackbar.make(contextView,"Account with entered email exist",Snackbar.LENGTH_LONG).show();
                                    }
                                    else {
                                        progressDialog = new ProgressDialog(RegistrationActivity.this);
                                        progressDialog.setTitle("Creating Account");
                                        progressDialog.setMessage("Please Wait...");
                                        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                        progressDialog.setCancelable(false);
                                        progressDialog.show();

                                        if(registerWith.equals("Email")) {
                                            createAccountWithDetails();
                                        }
                                        else
                                            goToOTPVerification(dataToMatch);

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
                phone.setEnabled(false);
                registerWith = "Email";
            }
            else if(!phoneInput.isEmpty()) {
                email.setEnabled(false);
                registerWith = "Phone";
            }
            else {
                phone.setEnabled(true);
                email.setEnabled(true);
                registerWith = "";
            }
            registerButton.setEnabled(!emailInput.isEmpty() || !phoneInput.isEmpty());
        }
        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private void createAccountWithDetails(){

        mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(RegistrationActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        progressDialog.dismiss();

                        if (task.isSuccessful()) {

                            HashMap<String,Object> userDetials= new HashMap<>();
                            userDetials.put("Name",name.getText().toString());
                            userDetials.put("Email",email.getText().toString());
                            userDetials.put("LastLat",0.0);
                            userDetials.put("LastLng",0.0);
                            userDetials.put("token_id","token_id_here");

                            mDatabaseReference.child(mAuth.getCurrentUser().getUid()).setValue(userDetials).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                    finish();
                                }
                            });

                        } else {
                            // If sign in fails, display a message to the user.
                            Snackbar.make(contextView,"Account creation failed",Snackbar.LENGTH_LONG).show();
                        }
                        // ...
                    }
                });
    }

    private void goToOTPVerification(String phone) {
        Intent otpVerification = new Intent(getApplicationContext(), OtpVerificationActivity.class);
        otpVerification.putExtra("Type","Register");
        otpVerification.putExtra("Name",name.getText().toString().trim());
        otpVerification.putExtra("Phone",phone);
        startActivity(otpVerification);
    }
}
