package com.example.android.partyappfox;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;


public class InterestFragment extends Fragment implements View.OnClickListener{

    private static final String PRIMARY_INTEREST_KEY = "primaryInterestLocalKey";
    private static final String SECONDARY_INTEREST_KEY = "secondaryInterestLocalKey";
    private static final String NAME_KEY = "nameLocalKey";
    private static final String LATITUDE_KEY = "latKey";
    private static final String LONGITUDE_KEY = "longKey";


    private Spinner mPrimarySpinner;
    private Spinner mSecondarySpinner;
    private EditText mEditName;
    private Button mUpdateButton;
    private View mView;

    private Context mContext;

    private String mPrimaryInterest;
    private String mSecondaryInterest;

    // Firebase authentication
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    // database references
    private DatabaseReference mDbRef;
    private DatabaseReference mGeoFireRef;

    // geofire object
    private GeoFire mGeoFire;

    // user Id
    private String mCurrentUserId;

    // location manager
    private LocationManager mLocManager;

    // location longitude and latitude
    private Double mLat;
    private Double mLon;


    public InterestFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the app context
        mContext = getActivity();

        // set up firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        // current user id
        mCurrentUserId = mFirebaseUser.getUid();

        // db references
        mDbRef = FirebaseDatabase.getInstance().getReference("/users" + "/" + mCurrentUserId);
        mGeoFireRef = FirebaseDatabase.getInstance().getReference("/geofire");
        mGeoFire = new GeoFire(mGeoFireRef);

        // the location manager from which we will call the location updates
        mLocManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_interest, container, false);

        // get access to the UI fields
        mPrimarySpinner = mView.findViewById(R.id.primary_spinner);
        mSecondarySpinner = mView.findViewById(R.id.secondary_spinner);
        mEditName = mView.findViewById(R.id.name_edit_text);
        mUpdateButton = mView.findViewById(R.id.update_interest_button);

        // set the on click listener for the button
        mUpdateButton.setOnClickListener(this);

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // populate the spinners and set the listeners
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(mContext, R.array.activities_array, android.R.layout.simple_spinner_item);
        adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPrimarySpinner.setAdapter(adapter1);
        mPrimarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long l) {
                // save the selection
                mPrimaryInterest = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(mContext, R.array.activities_array, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSecondarySpinner.setAdapter(adapter2);
        mSecondarySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long l) {
                // save the selection
                mSecondaryInterest = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // initialize the values if already saved before
        setAlreadySavedValues();

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.update_interest_button){
            update();
        }
    }

    // check if the user has given a name and doesn't select the same interest
    private boolean validateInput(){
        boolean res = true;
        String name = mEditName.getText().toString();

        // check if the name is null
        if (name.length() == 0){
            res = false;
            Toast.makeText(mContext, "Please input your name", Toast.LENGTH_SHORT).show();
        }

        // check if the two interests are different
        if (mPrimaryInterest == mSecondaryInterest){
            res = false;
            Toast.makeText(mContext, "Don't select the same interests", Toast.LENGTH_SHORT).show();
        }

        return res;
    }

    // when the fragment launches set the values to what they've been previously saved to in shared prefs
    private void setAlreadySavedValues(){
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        String interest1 = sharedPref.getString(PRIMARY_INTEREST_KEY, "");
        String interest2 = sharedPref.getString(SECONDARY_INTEREST_KEY, "");
        String name = sharedPref.getString(NAME_KEY, "");

        // get the activities array
        String[] list = getResources().getStringArray(R.array.activities_array);
        List<String> activityList = Arrays.asList(list);

        // if there was a value already specified, set that in our UI
        if (name.length() > 0){
            mEditName.setText(name);
        }

        // initialize spinner positions
        if (interest1.length() > 0){
            int index1 = activityList.indexOf(interest1);
            mPrimarySpinner.setSelection(index1);
        }

        if (interest2.length() > 0){
            int index2 = activityList.indexOf(interest2);
            mSecondarySpinner.setSelection(index2);

        }

    }

    // update the user preferences locally
    private void updateLocally(){

        // get the shared preferences and save the values
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PRIMARY_INTEREST_KEY, mPrimaryInterest);
        editor.putString(SECONDARY_INTEREST_KEY, mSecondaryInterest);
        editor.putString(NAME_KEY, mEditName.getText().toString());
        editor.commit();

    }

    // update the user preferences in the firebase cloud storage
    private void updateCloud(){
        // locations
        DatabaseReference interest1 = mDbRef.child("interests").child("primary");
        DatabaseReference interest2 = mDbRef.child("interests").child("secondary");
        DatabaseReference name = mDbRef.child("name");

        // set value
        name.setValue(mEditName.getText().toString());
        interest1.setValue(mPrimaryInterest);
        interest2.setValue(mSecondaryInterest);

    }

    // return the best last location
    private Location getLastKnownLocation() {
        List<String> providers = mLocManager.getProviders(true);
        Location bestLocation = null;

        /* check for permission
        this will always evaluate to true, because the app can't start if the user doesn't allow location
        during sign up */
        if ((ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(mContext, "Location was already enabled", Toast.LENGTH_SHORT).show();
        }

        for (String provider : providers) {
            Location loc = mLocManager.getLastKnownLocation(provider);
            if (loc == null) {
                continue;
            }
            if (bestLocation == null || loc.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = loc;
            }
        }
        return bestLocation;
    }

    // when the update button is clicked save the last known location of the user
    private void updateLocationCloud(){

        // update the location
        try{
            final Location location = getLastKnownLocation();

            if (location != null) {
                mLat = location.getLatitude();
                mLon = location.getLongitude();
            } else {
                Toast.makeText(mContext, "Location is null", Toast.LENGTH_SHORT).show();
            }
            // do the saving in the cloud
            if (mLat != null && mLon != null) {
                mGeoFire.setLocation(mCurrentUserId, new GeoLocation(mLat,mLon));

                // do the saving locally in shared prefs
                SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putFloat(LATITUDE_KEY, mLat.floatValue());
                editor.putFloat(LONGITUDE_KEY, mLon.floatValue());
                editor.commit();

            }
        }
        catch (SecurityException e){

        }

    }

    private void update(){

        if (!validateInput()){
            return;
        }

        // update shared prefs
        updateLocally();

        // update in the cloud
        updateCloud();

        // update the location in the cloud
        updateLocationCloud();
    }

}
