package com.example.android.partyappfox;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mEmail;
    private EditText mPassword;
    private TextView mPrompt;
    private ProgressBar mProgBar;
    private FirebaseAuth mAuth;

    private final int LOCATION_PERMISSIONS_REQUEST = 111;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // get the edit texts and text prompt
        mEmail = (EditText) findViewById(R.id.email);
        mPassword = (EditText) findViewById(R.id.password);
        mPrompt = (TextView) findViewById(R.id.sign_in_prompt);
        mProgBar = (ProgressBar) findViewById(R.id.progress_bar);

        // set the onclick listeners
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        findViewById(R.id.email_create_account_button).setOnClickListener(this);

        // auth object initialization
        mAuth = FirebaseAuth.getInstance();

        // ask for permissions as soon as the app starts
        if ((ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSIONS_REQUEST);
        }
    }

    /*
    Very rudimentary check to validate the input into the email and password fields
    At this stage it only checks if the input is empty
     */
    private boolean validateInput(){
        boolean res = true;
        String password = mPassword.getText().toString();
        String email = mEmail.getText().toString();
        // set the error based on what's missing
        if (password.length() == 0 && email.length() == 0){
            mPrompt.setTextColor(getResources().getColor(R.color.errorColor));
            mPrompt.setText("Input an email and password!");
            res = false;
        }
        else if (password.length() == 0){
            mPrompt.setTextColor(getResources().getColor(R.color.errorColor));
            mPrompt.setText("Input a password!");
            res = false;
        }
        else if (email.length() == 0){
            mPrompt.setTextColor(getResources().getColor(R.color.errorColor));
            mPrompt.setText("Input an email!");
            res = false;
        }else {
            mPrompt.setTextColor(getResources().getColor(R.color.textColor));
            mPrompt.setText("Sign In or Create Account");
            res = true;
        }
        return res;
    }

    private void showDialog(){
        mProgBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        mProgBar.setVisibility(View.INVISIBLE);
    }

    // handle the button clicks
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.email_sign_in_button){
            signIn(mEmail.getText().toString(), mPassword.getText().toString());
        }
        else if (id == R.id.email_create_account_button){
            createAccount(mEmail.getText().toString(), mPassword.getText().toString());
        }
    }

    // create the new account
    private void createAccount(String email, String password) {
        // do not create an account if email or password is missing
        if (!validateInput()) {
            return;
        }

        showDialog();

        // register the new user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                // take the user to the main activity if the sign up was successful
                if(task.isSuccessful()){
                    Toast.makeText(SignInActivity.this, "Account created!", Toast.LENGTH_SHORT).show();
                    hideDialog();
                    Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                }else {
                    Toast.makeText(SignInActivity.this, "Account creation failed!", Toast.LENGTH_SHORT).show();
                    hideDialog();
                }
            }
        });
    }

    // sign in user
    private void signIn(String email, String password) {
        // do not create an account if email or password is missing
        if (!validateInput()) {
            return;
        }

        showDialog();

        // sign in an already existing user
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                // take the user to the main activity if the sign in was successful
                if(task.isSuccessful()){
                    Toast.makeText(SignInActivity.this, "Signed in!", Toast.LENGTH_SHORT).show();
                    hideDialog();
                    Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                }else {
                    Toast.makeText(SignInActivity.this, "Account creation failed!", Toast.LENGTH_SHORT).show();
                    hideDialog();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // restart everything
                    Toast.makeText(this, "Location services enabled", Toast.LENGTH_SHORT).show();

                } else {
                    // Permission Denied
                    Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }    }
}
