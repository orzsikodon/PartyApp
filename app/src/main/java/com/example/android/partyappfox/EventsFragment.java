package com.example.android.partyappfox;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;


public class EventsFragment extends Fragment {
    private final double QUERY_RADIUS = 1.5;
    private static final String LATITUDE_KEY = "latKey";
    private static final String LONGITUDE_KEY = "longKey";
    private static final String NAME_KEY = "nameLocalKey";
    private static final String EVENT_INTENT_KEY = "eventIntentKey";

    // context and view and adapter
    private Context mContext;
    private View mView;
    private static CustomEventAdapter customEventAdapter;

    // widgets
    private ProgressDialog mDialog;
    private ListView mListView;
    private TextView mText;

    // Firebase auth
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mCurrentUserId;

    // db ref and geofire
    private DatabaseReference mGeoFireRef;
    private GeoFire mGeoFire;
    public GeoQuery mGeoQuery;

    // a list for the adapter holding all the queried events
    // and distance
    private ArrayList<DataSnapshot> mDataEventList;
    private ArrayList<Double> mDistanceList;

    // user fields from shared prefs
    private double mCurrentLat;
    private double mCurrentLon;
    private String mCurrentName;

    public EventsFragment() {
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
        mCurrentUserId = mFirebaseUser.getUid();

        // db and query geofire query
        mGeoFireRef = FirebaseDatabase.getInstance().getReference().child("geofireEvents");
        mGeoFire = new GeoFire(mGeoFireRef);

        // initialize the empty array lists
        mDataEventList = new ArrayList<>();
        mDistanceList = new ArrayList<>();

        // get the lat and long of the users location and name from shared prefs
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        mCurrentLat = Double.valueOf(sharedPref.getFloat(LATITUDE_KEY, 666));
        mCurrentLon = Double.valueOf(sharedPref.getFloat(LONGITUDE_KEY, 666));
        mCurrentName = sharedPref.getString(NAME_KEY, "");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_events, container, false);
        mListView = mView.findViewById(R.id.main_list);
        mText = mView.findViewById(R.id.events_around_tv);
        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // load the data
        loadQueriedEvents();
    }

    // show dialog while loading list
    public void showDialog(){
        mDialog = new ProgressDialog(getActivity());
        mDialog.setTitle("Findig events near you...");
        mDialog.setMessage("Please wait...");
        mDialog.setCancelable(true);
        mDialog.setIndeterminate(true);
        mDialog.show();
    }

    private void loadQueriedEvents(){
        // check if there are location coordinates
        if (mCurrentLon == 666 || mCurrentLon == 666 || mCurrentName.length() == 0){
            Toast.makeText(mContext, "Please update your interests first!", Toast.LENGTH_LONG).show();
            navigateToInterestFragment();
        }
        else {
            // loading is starting show the dialog
            showDialog();

            // setup the query around the user's position and add the listener
            mGeoQuery = mGeoFire.queryAtLocation(new GeoLocation(mCurrentLat, mCurrentLon), QUERY_RADIUS);
            mGeoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
                @Override
                public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {

                    // add the event snapshot that's in the vicinity to the list
                    mDataEventList.add(dataSnapshot);
                    mDistanceList.add(mCalculateDistance(mCurrentLat, mCurrentLon, location));
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

                    // populate the list view
                    populateListView(mDataEventList, mDistanceList);
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                    if (mDialog != null){
                        mDialog.dismiss();
                    }
                    Toast.makeText(mContext, "Error loading data", Toast.LENGTH_LONG).show();

                }
            });
        }
    }

    // navigate to interest fragment
    // TODO if time, create utils for duplicate code
    private void navigateToInterestFragment(){
        Fragment mFrag = new InterestFragment();
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mTransaction = mFragmentManager.beginTransaction();
        mTransaction.replace(R.id.grid_frag_container, mFrag);
        mTransaction.commit();
    }

    // populate the custom array adapter for the list view
    private void populateListView(final ArrayList<DataSnapshot> eventList, final ArrayList<Double> distanceList){
        if (eventList.size() > 0){
            // dismiss the dialog, loading finished
            if (mDialog != null){
                mDialog.dismiss();
            }
            // set the adapter
            customEventAdapter = new CustomEventAdapter(mContext, eventList, distanceList);
            mListView.setAdapter(customEventAdapter);

            // set the on click listener
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    String eventID = eventList.get(position).getKey();
                    // fire the send picture intent
                    Intent mIntent = new Intent(getActivity(), EventDetailActivity.class);
                    mIntent.putExtra(EVENT_INTENT_KEY, eventID);
                    startActivity(mIntent);
                }
            });
        }else {
            // if the event list is empty tell the user
            mText.setText("No Events Around You");

            if (mDialog != null){
                mDialog.dismiss();
            }
        }
    }

    // method that calculates each event's location from the user in meters
    private double mCalculateDistance(double currentLat, double currentLon, GeoLocation eventLocation){

        double dist;

        Location currentLoc = new Location("");
        currentLoc.setLatitude(currentLat);
        currentLoc.setLongitude(currentLon);

        Location eventLoc = new Location("");
        eventLoc.setLatitude(eventLocation.latitude);
        eventLoc.setLongitude(eventLocation.longitude);

        dist = currentLoc.distanceTo(eventLoc);

        // round it to 2 decimal places
        double rounded = Math.round(dist * 100.0) / 100.0;

        return rounded;
    }

    @Override
    public void onStop() {
        super.onStop();

        if (customEventAdapter != null) {
            customEventAdapter.clearAdapter();
        }

        if (mDialog != null){
            mDialog.dismiss();
        }
    }
}
