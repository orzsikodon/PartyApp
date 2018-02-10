package com.example.android.partyappfox;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Created by odonorzsik on 2/6/18.
 */

public class StartActivity extends Activity {
    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Authentication
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null){
            // if user is not logged in take him to sign in screen
            Intent mSignInIntent = new Intent(this, SignInActivity.class);
            startActivity(mSignInIntent);
            finish();
        }
        else {
            // user is logged in take him to main activity
            Intent mSignInIntent = new Intent(this, MainActivity.class);
            startActivity(mSignInIntent);
            finish();
        }
    }
}
