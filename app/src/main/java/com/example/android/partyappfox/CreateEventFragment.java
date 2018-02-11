package com.example.android.partyappfox;


import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class CreateEventFragment extends Fragment {

    private final double QUERY_RADIUS = 1.5;
    private static final String LATITUDE_KEY = "latKey";
    private static final String LONGITUDE_KEY = "longKey";

    // views and widgets
    private View mView;
    private TextView mPrimaryTV;
    private TextView mSecondaryTV;
    private ProgressBar mProgBar;

    private String mMostPopular;

    // Firebase authentication
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    // geofire refs
    private DatabaseReference mGeoFireRef;
    private GeoFire mGeoFire;
    public GeoQuery mGeoQuery;

    // user db ref
    private DatabaseReference mBaseDB;

    // latitude and longitude of the users location saved in shared prefs
    public Double mCurrentLat;
    public Double mCurrentLon;

    // user Id
    private String mCurrentUserId;
    private Context mContext;

    private ArrayList<DataSnapshot> mUserDataList;
    private DataSnapshot mCurrentUserData;


    public CreateEventFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the app context
        mContext = getActivity();

        // set up firebase
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        // geofire reference for the users
        mGeoFireRef = FirebaseDatabase.getInstance().getReference("/geofire");
        mGeoFire = new GeoFire(mGeoFireRef);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_create_event, container, false);

        mPrimaryTV = mView.findViewById(R.id.primary_interest_tv);
        mSecondaryTV = mView.findViewById(R.id.secondary_interest_tv);
        mProgBar = mView.findViewById(R.id.progress_bar_create);

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get the lat and long of the users location from shared prefs
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        mCurrentLat = Double.valueOf(sharedPref.getFloat(LATITUDE_KEY, 666));
        mCurrentLon = Double.valueOf(sharedPref.getFloat(LONGITUDE_KEY, 666));

        // initialize the UiD array list
        mUserDataList = new ArrayList();

        // current user id
        mCurrentUserId = mFirebaseUser.getUid();

        // query the nearby users
        queryNearbyUsers();


    }

    // query the user id's of users in a 1.5km radius circle
    private void queryNearbyUsers(){
        // check if there are location coordinates
        if (mCurrentLon == 666 || mCurrentLon == 666){
            // take user to interest fragment
            Toast.makeText(mContext, "Please update your interests first!", Toast.LENGTH_LONG).show();
        } else {

            showDialog();


            // create the query object
            mGeoQuery = mGeoFire.queryAtLocation(new GeoLocation(mCurrentLat, mCurrentLon), QUERY_RADIUS);
            mGeoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
                @Override
                public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {

                    // loop through all the users we retrieve
                    if (!dataSnapshot.getKey().equals(mCurrentUserId)) {
                        mUserDataList.add(dataSnapshot);
                    }else {
                        mCurrentUserData = dataSnapshot;
                    }
                }

                @Override
                public void onDataExited(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onDataMoved(DataSnapshot dataSnapshot, GeoLocation location) {

                }

                @Override
                public void onDataChanged(DataSnapshot dataSnapshot, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {

                    // find the most biggest number of preferences from among nearby users
                    HashMap<String, Integer> map = createHashMap(mUserDataList);
                    calculateAndUpdateFunction(map);
                    hideDialog();
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    hideDialog();
                }
            });

        }

    }

    // method to calculate most popular user preferences and update the UI accordingly
    private HashMap<String, Integer> createHashMap(ArrayList<DataSnapshot> dataArray){

        // put the interests in a hashmap
        HashMap<String, Integer> interestMap = new HashMap<>();
        for (DataSnapshot userData : dataArray) {
            String interest1 = (String) userData.child("data").child("interests").child("primary").getValue();
            String interest2 = (String) userData.child("data").child("interests").child("secondary").getValue();

            // put interest1 in the hash map if it doesn't exist
            if (!interestMap.containsKey(interest1)){
                interestMap.put(interest1, 1);
            }
            // if it already exists increment it
            else {
                interestMap.put(interest1, interestMap.get(interest1)+1);
            }

            // do this for the second interest
            if (!interestMap.containsKey(interest2)){
                interestMap.put(interest2, 1);
            }
            // if it already exists increment it
            else {
                interestMap.put(interest2, interestMap.get(interest2)+1);
            }

        }

        return interestMap;

    }

    private void calculateAndUpdateFunction(HashMap<String, Integer> map){

        // now find the highest and second highest and update the UI
        int primary = 0;
        String primaryInterestString = null;
        int secondary = 0;
        String secondaryInterestString = null;

        for(HashMap.Entry<String, Integer> pair : map.entrySet()) {
            if(pair.getValue() >= primary) {
               primary = pair.getValue();
               primaryInterestString = pair.getKey();
            }
        }

        map.remove(primaryInterestString);

        for(HashMap.Entry<String, Integer> pair : map.entrySet()) {
            if(pair.getValue() >= secondary) {
                secondary = pair.getValue();
                secondaryInterestString = pair.getKey();
            }
        }

        // set the ui
        mMostPopular = primaryInterestString;
        if (primary == 1) {
            mPrimaryTV.setText(primary + " person is interested in " + primaryInterestString);
        }else {
            mPrimaryTV.setText(primary + " people are interested in " + primaryInterestString);
        }

        if (secondary == 1){
            mSecondaryTV.setText(secondary + " person is interested in " + secondaryInterestString);
        }else {
            mSecondaryTV.setText(secondary + " people are interested in " + secondaryInterestString);
        }
    }

    private void showDialog(){
        mProgBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        mProgBar.setVisibility(View.INVISIBLE);
    }
}
