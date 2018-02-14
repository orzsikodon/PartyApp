package com.example.android.partyappfox;


import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class MapFragment extends Fragment {

    private static final int ZOOM_LEVEL = 14;
    private final double QUERY_RADIUS = 1.5;
    private static final String LATITUDE_KEY = "latKey";
    private static final String LONGITUDE_KEY = "longKey";
    private static final String EVENT_INTENT_KEY = "eventIntentKey";

    // view and context
    private View mView;
    private Context mContext;

    // Map instance variables
    private MapView mMapView;
    private GoogleMap mGoogleMap;
    private ProgressDialog mDialog;


    // Firebase authentication
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mCurrentUserID;

    // database reference and query
    private DatabaseReference mGeoFireRef;
    private GeoFire mGeoFire;
    private GeoQuery mGeoQuery;

    // hash map to store the user's id an location object
    private HashMap<String, GeoLocation> mEventLocationHashMap;

    // latitude and longitude of the users location saved in shared prefs
    private Double mCurrentLat;
    private Double mCurrentLon;


    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get the app context
        mContext = getActivity();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_map, container, false);

        // set up the google maps
        mMapView = mView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        // needed to get the map to display immediately
        mMapView.onResume();


        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize Firebase Auth and user
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mCurrentUserID = mFirebaseUser.getUid();

        // geofire ref
        mGeoFireRef = FirebaseDatabase.getInstance().getReference().child("geofireEvents");
        mGeoFire = new GeoFire(mGeoFireRef);

        // get the lat and long of the users location and name from shared prefs
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        mCurrentLat = Double.valueOf(sharedPref.getFloat(LATITUDE_KEY, 666));
        mCurrentLon = Double.valueOf(sharedPref.getFloat(LONGITUDE_KEY, 666));

        // location map
        mEventLocationHashMap = new HashMap<>();

        // query the events
        queryAndRenderEvents();
    }


    // show dialog while loading map
    private void showDialog(){
        mDialog = new ProgressDialog(getActivity());
        mDialog.setTitle("Setting up map...");
        mDialog.setMessage("Please wait.");
        mDialog.setCancelable(true);
        mDialog.setIndeterminate(true);
        mDialog.show();
    }

    // method to center the map
    public void centerMap(GoogleMap googleMap){

        if (mCurrentLon == 666 || mCurrentLon == 666){

            // dismiss the dialog
            if (mDialog != null) {
                mDialog.dismiss();
            }

            Toast.makeText(mContext, "Please update your interests first!", Toast.LENGTH_LONG).show();
            navigateToInterestFragment();
        }
        else {

            LatLng latLngCenter = new LatLng(mCurrentLat, mCurrentLon);
            Circle searchCircle = googleMap.addCircle(new CircleOptions().center(latLngCenter).radius(QUERY_RADIUS * 1000));
            searchCircle.setFillColor(Color.argb(40, 0, 0, 210));
            searchCircle.setStrokeColor(Color.argb(70, 0, 0, 0));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngCenter, ZOOM_LEVEL));

            // dismiss the dialog
            if (mDialog != null) {
                mDialog.dismiss();
            }
        }
    }

    // method that adds clickable markers for the events
    public void addMarkers(GoogleMap googleMap){

        // add a marker for every user
        for (String key : mEventLocationHashMap.keySet()){

            // put the marker at the users's position
            double lat = mEventLocationHashMap.get(key).latitude;
            double lon = mEventLocationHashMap.get(key).longitude;
            LatLng markerPosition = new LatLng(lat, lon);

            // add it to the map
            googleMap.addMarker(new MarkerOptions().position(markerPosition).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_calendar_final)));
        }
    }

    // method to start the Event detail activity
    public void sendIntent(String eventID){

        Intent mStartDetailActivity = new Intent(getActivity(), EventDetailActivity.class);

        // put the selected events id and start the activity
        mStartDetailActivity.putExtra(EVENT_INTENT_KEY, eventID);
        startActivity(mStartDetailActivity);

    }

    // navigate to interest fragment
    private void navigateToInterestFragment(){
        Fragment mFrag = new InterestFragment();
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mTransaction = mFragmentManager.beginTransaction();
        mTransaction.replace(R.id.grid_frag_container, mFrag);
        mTransaction.commit();
    }

    // query and render the events
    private void queryAndRenderEvents(){

        // show the dialog
        showDialog();

        if (mCurrentLon == 666 || mCurrentLon == 666){

            if (mDialog != null){
                mDialog.dismiss();
            }

            Toast.makeText(mContext, "Please update your interests first!", Toast.LENGTH_LONG).show();
            navigateToInterestFragment();
        }
        else {

            // create the query object and add the event listener
            mGeoQuery = mGeoFire.queryAtLocation(new GeoLocation(mCurrentLat, mCurrentLon), QUERY_RADIUS);
            mGeoQuery.addGeoQueryDataEventListener(new GeoQueryDataEventListener() {
                @Override
                public void onDataEntered(DataSnapshot dataSnapshot, GeoLocation location) {

                    // add the events key and location to the hash map
                    mEventLocationHashMap.put(dataSnapshot.getKey(), location);

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

                    // render the events on the map and set the marker listeners
                    mMapView.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            mGoogleMap = googleMap;

                            // center the map
                            centerMap(mGoogleMap);

                            // add markers
                            addMarkers(mGoogleMap);

                            // make the map listen for marker clicks
                            mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                                @Override
                                public boolean onMarkerClick(Marker marker) {
                                    LatLng LatLon = marker.getPosition();
                                    Double MarkerLatitude = LatLon.latitude;
                                    Double MarkerLongitude = LatLon.longitude;

                                    // iterate through the hash map and find that marker/key which corresponds to the marker's position
                                    for (String key : mEventLocationHashMap.keySet()){

                                        double UserLat = mEventLocationHashMap.get(key).latitude;
                                        double UserLon = mEventLocationHashMap.get(key).longitude;

                                        if ((MarkerLatitude == UserLat) && (MarkerLongitude == UserLon)){
                                            navigateToInterestFragment();
                                            sendIntent(key);
                                            break;
                                        }
                                    }
                                    return true;
                                }
                            });
                        }
                    });
                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                    if (mDialog != null){
                        mDialog.dismiss();
                    }

                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
