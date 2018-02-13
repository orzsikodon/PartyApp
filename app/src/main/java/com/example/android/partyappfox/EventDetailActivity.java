package com.example.android.partyappfox;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class EventDetailActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String EVENT_INTENT_KEY = "eventIntentKey";
    private static final String NAME_KEY = "nameLocalKey";

    // widgets
    private TextView mTitleTV;
    private TextView mTypeTV;
    private TextView mDescriptionTV;
    private TextView mAttendingTV;
    private TextView mDateTV;
    private TextView mTimeTV;
    private Button mButton;
    private Toolbar mToolbar;

    private ProgressDialog mDialog;

    // vars
    private String mEventID;
    private String mCurrentID;
    private String mCurrentName;

    // event attributes
    private String mEventTitle;
    private String mEventType;
    private String mEvenDescription;
    private String mCreatorID;
    private long mYear;
    private long mMonth;
    private long mDay;
    private long mHour;
    private long mMinute;
    private ArrayList<String> mEventAttending;

    // firbase, geoquery and auth
    private DatabaseReference mDbRef;
    private DatabaseReference mAttendingRefID;
    private DatabaseReference mAttendingRefName;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // get the toolbar
        mToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);

        // Get a support ActionBar corresponding to this toolbar to navigate back
        ActionBar ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        mEventAttending = new ArrayList<>();

        // get the widgets
        mTitleTV = findViewById(R.id.detail_title_tv);
        mTypeTV = findViewById(R.id.detail_type_tv);
        mDescriptionTV = findViewById(R.id.detail_description_tv);
        mAttendingTV = findViewById(R.id.detail_attending_view);
        mDateTV =  findViewById(R.id.detail_date_tv);
        mTimeTV = findViewById(R.id.detail_time_tv);
        mButton = findViewById(R.id.detail_button);

        // set the on click listener for the button
        mButton.setOnClickListener(this);

        // firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mCurrentID = mFirebaseUser.getUid();

        // get the intent
        Intent startingIntent = getIntent();
        mEventID = startingIntent.getStringExtra(EVENT_INTENT_KEY);

        // shared prefs name
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        mCurrentName = sharedPref.getString(NAME_KEY, "");


        // dbRef
        mDbRef = FirebaseDatabase.getInstance().getReference().child("geofireEvents").child(mEventID);
        mAttendingRefID = mDbRef.child("eventData").child("attending").child("id").child(mCurrentID);
        mAttendingRefName = mDbRef.child("eventData").child("attending").child("name").child(mCurrentName);

        // load the data
        loadData();
    }

    // show dialog while loading list
    private void showDialog(){
        mDialog = new ProgressDialog(this);
        mDialog.setTitle("Loading data...");
        mDialog.setMessage("Please wait...");
        mDialog.setCancelable(true);
        mDialog.setIndeterminate(true);
        mDialog.show();
    }

    // load the data at the event node
    private void loadData(){
        // show the dialog first
        showDialog();

        // load the data
        mDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                // set the data from the event snapshot
                getValues(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (mDialog != null){
                    mDialog.dismiss();
                }
            }
        });
    }

    // set the values of the event and update the UI
    private void getValues(DataSnapshot dataSnapshot){
        if (dataSnapshot != null) {
            mEventTitle = (String) dataSnapshot.child("eventData").child("title").getValue();
            mEventType = (String) dataSnapshot.child("eventData").child("type").getValue();
            mEvenDescription = (String) dataSnapshot.child("eventData").child("description").getValue();
            mCreatorID = (String) dataSnapshot.child("eventData").child("creator").child("id").getValue();
            mYear = (long) dataSnapshot.child("eventData").child("date").child("year").getValue();
            mMonth = (long) dataSnapshot.child("eventData").child("date").child("month").getValue();
            mDay = (long) dataSnapshot.child("eventData").child("date").child("day").getValue();
            mHour = (long) dataSnapshot.child("eventData").child("date").child("hour").getValue();
            mMinute = (long) dataSnapshot.child("eventData").child("date").child("minute").getValue();

            // populate the list with the people attending
            for (DataSnapshot id : dataSnapshot.child("eventData").child("attending").child("name").getChildren()) {
                mEventAttending.add(id.getKey());
            }
            // dismiss the dialog
            if (mDialog != null) {
                mDialog.dismiss();
            }

            // update the ui
            updateUI();
        }
    }

    private void updateUI(){
        String attending = "Attending:" + '\n';
        mTitleTV.setText(mEventTitle);
        mTypeTV.setText(mEventType);
        mDescriptionTV.setText("Description" + '\n' + mEvenDescription);

        // populate the view with the people attending
        for (String name : mEventAttending){
            attending = attending + name + '\n';
        }
        mAttendingTV.setText(attending);
        mDateTV.setText("Date: " + mYear + "." + mMonth + "." + mDay);
        mTimeTV.setText("Time: " + mHour + ":" + mMinute);

        // button is either delete or attend
        if (mCurrentID.equals(mCreatorID)){
            mButton.setText("Delete");
        }else {
            mButton.setText("Attend");
        }
    }


    @Override
    public void onClick(View view) {

        // delete or attend
        if (mCurrentID.equals(mCreatorID)){
            deleteEvent();
        }else {
            attendEvent();
        }
    }

    // delete the event if you are the creator
    private void deleteEvent(){
        mDbRef.removeValue();
        Toast.makeText(this, "Event deleted!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void attendEvent(){
        mAttendingRefID.setValue(true);
        mAttendingRefName.setValue(true);
        Toast.makeText(this, "You are attending this event!", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDialog != null){
            mDialog.dismiss();
        }
    }
}
