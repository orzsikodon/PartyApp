package com.example.android.partyappfox;


import android.app.DatePickerDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.app.Fragment;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class CreateEventFragment extends Fragment implements View.OnClickListener {

    private final double QUERY_RADIUS = 2.0;
    private static final String LATITUDE_KEY = "latKey";
    private static final String LONGITUDE_KEY = "longKey";
    private static final String EVENT_DATA_KEY = "eventData";
    private static final String NAME_KEY = "nameLocalKey";

    // views and widgets
    private View mView;
    private TextView mPrimaryTV;
    private TextView mSecondaryTV;
    private ProgressBar mProgBar;
    private EditText mTitleET;
    private EditText mDescriptionET;
    private Spinner mEventTypeSpinner;

    private Button mSetStartTime;
    private Button mSetStartDate;
    private Button mCreate;

    private String mEventType;

    // Firebase authentication
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    // geofire user refs
    private DatabaseReference mGeoFireRef;
    private GeoFire mGeoFire;
    public GeoQuery mGeoQuery;

    // geofire events refs
    private DatabaseReference mGeoFireEventsRef;
    private GeoFire mGeoFireEvents;

    // unique key consisting of UiD + system time
    private String mEventKey;

    // user db ref
    private DatabaseReference mDbRef;

    // latitude and longitude of the users location and name saved in shared prefs
    private Double mCurrentLat;
    private Double mCurrentLon;
    private String mCurrentName;

    // user Id
    private String mCurrentUserId;
    private Context mContext;

    private ArrayList<DataSnapshot> mUserDataList;
    private DataSnapshot mCurrentUserData;
    private String[] mActivityList;

    // current date
    private int mCurrentYear, mCurrentMonth, mCurrentDay;

    // date vars for setting start end date
    private int mEventStartYear = -1, mEventStartMonth = -1, mEventStartDay = -1, mEvenStartHour = -1, mEventStartMinute = -1;

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

        // current user id
        mCurrentUserId = mFirebaseUser.getUid();

        // geofire reference for the users
        mGeoFireRef = FirebaseDatabase.getInstance().getReference().child("geofire");
        mGeoFire = new GeoFire(mGeoFireRef);

        // geofire references for the event created
        mGeoFireEventsRef = FirebaseDatabase.getInstance().getReference().child("geofireEvents");
        mGeoFireEvents = new GeoFire(mGeoFireEventsRef);

        // db ref to add the event to the user
        mDbRef = FirebaseDatabase.getInstance().getReference().child("attendance");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_create_event, container, false);

        mActivityList = mView.getResources().getStringArray(R.array.activities_array);

        instantiateWidgets(mView);

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        populateSpinner();

        getEventStartDate();

        // get the lat and long of the users location and name from shared prefs
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);
        mCurrentLat = Double.valueOf(sharedPref.getFloat(LATITUDE_KEY, 666));
        mCurrentLon = Double.valueOf(sharedPref.getFloat(LONGITUDE_KEY, 666));
        mCurrentName = sharedPref.getString(NAME_KEY, "");

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
        if (mCurrentLon == 666 || mCurrentLon == 666 || mCurrentName.length() == 0){
            Toast.makeText(mContext, "Please update your interests first!", Toast.LENGTH_LONG).show();
            navigateToInterestFragment();
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
                    Toast.makeText(mContext, "Error loading data", Toast.LENGTH_LONG).show();
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

        // set spinner position
        List<String> activityList = Arrays.asList(mActivityList);
        int index = activityList.indexOf(primaryInterestString);
        mEventTypeSpinner.setSelection(index);
    }

    private void showDialog(){
        mProgBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        mProgBar.setVisibility(View.INVISIBLE);
    }

    // get access to the widgets in our view
    private void instantiateWidgets(View mView){

        mPrimaryTV = mView.findViewById(R.id.primary_interest_tv);
        mSecondaryTV = mView.findViewById(R.id.secondary_interest_tv);
        mProgBar = mView.findViewById(R.id.progress_bar_create);
        mTitleET = mView.findViewById(R.id.title_et);
        mEventTypeSpinner = mView.findViewById(R.id.creation_spinner);
        mSetStartTime = mView.findViewById(R.id.start_time_bt);
        mSetStartDate = mView.findViewById(R.id.start_date_bt);
        mCreate = mView.findViewById(R.id.create_button);
        mDescriptionET = mView.findViewById(R.id.description_et);

        // give the multi line edit text a done button
        mDescriptionET.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mDescriptionET.setRawInputType(InputType.TYPE_CLASS_TEXT);

        // set the on click listeners for the buttons
        mSetStartTime.setOnClickListener(this);
        mSetStartDate.setOnClickListener(this);
        mCreate.setOnClickListener(this);

    }

    private void populateSpinner(){

        // populate the spinner and set the listener
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, R.array.activities_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mEventTypeSpinner.setAdapter(adapter);
        mEventTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long l) {
                // save the selection
                mEventType = parent.getItemAtPosition(pos).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    // get current date
    private void getEventStartDate(){
        final Calendar c = Calendar.getInstance();
        mCurrentYear = c.get(Calendar.YEAR);
        mCurrentMonth = c.get(Calendar.MONTH);
        mCurrentDay = c.get(Calendar.DAY_OF_MONTH);
    }

    // get the event end date entered by the user
    private void getEventEndDate(){
        DatePickerDialog datePickerDialog = new DatePickerDialog(mContext, new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                       mEventStartYear = year;
                       mEventStartMonth = monthOfYear;
                       mEventStartDay = dayOfMonth;

                        Toast.makeText(mContext, "Year: " + mEventStartYear + " Month: " + mEventStartMonth + " Day: " + mEventStartDay, Toast.LENGTH_SHORT).show();

                    }
                }, mCurrentYear, mCurrentMonth, mCurrentDay);
        datePickerDialog.show();
    }

    // get the event start time specified by the user
    private void getEventStartTime(){
        // Get Current Time
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                        mEvenStartHour = hourOfDay;
                        mEventStartMinute = minute;
                        Toast.makeText(mContext, "Hour: " + mEvenStartHour + " Minute: " + mEventStartMinute, Toast.LENGTH_SHORT).show();
                    }
                }, hour, minute, true);
        timePickerDialog.show();
    }

    // basic input validation
    private boolean validateInput(){
        boolean res = true;
        String title =  mTitleET.getText().toString();
        String description = mDescriptionET.getText().toString();

        // check if user has given a title and description
        if (title.length() == 0 || description.length() == 00){
            res = false;
            Toast.makeText(mContext, "Please give a title and description!", Toast.LENGTH_SHORT).show();

        }

        if (mEventStartDay == -1 || mEvenStartHour == -1){
            res = false;
            Toast.makeText(mContext, "Please give event start time and date!", Toast.LENGTH_SHORT).show();
        }

        return res;
    }

    // create the event on the cloud
    private void create(){
        if (!validateInput()){
            return;
        }

        createEventCloud();

    }

    // create the queriable event on the cloud
    private void createEventCloud(){
        // firs get the users location
        SharedPreferences sharedPref = mContext.getSharedPreferences(getString(R.string.shared_pref_key), Context.MODE_PRIVATE);

        // create the event key
        long time = System.currentTimeMillis();

        mEventKey = mCurrentUserId + Long.toString(time);

        mGeoFireEvents.setLocation(mEventKey, new GeoLocation(mCurrentLat, mCurrentLon), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

                // set the values of the event
                createEventDataCloud(mEventKey);

                // add it to the user
                addEventToUserDB(mEventKey);

                Toast.makeText(mContext, "Event Created!", Toast.LENGTH_LONG).show();

                // navigate to interest fragment
                navigateToAroundMe();

            }
        });
    }

    // set the events data in queriable db
    private void createEventDataCloud(String key){
        DatabaseReference title = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("title");
        DatabaseReference type = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("type");
        DatabaseReference description = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("description");

        DatabaseReference year = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("date").child("year");
        DatabaseReference month = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("date").child("month");
        DatabaseReference day = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("date").child("day");
        DatabaseReference hour = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("date").child("hour");
        DatabaseReference minute = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("date").child("minute");

        DatabaseReference creatorID = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("creator").child("id");
        DatabaseReference creatorName = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("creator").child("name");

        DatabaseReference attendingID = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("attending").child("id").child(mCurrentUserId);
        DatabaseReference attendingName = mGeoFireEventsRef.child(key).child(EVENT_DATA_KEY).child("attending").child("name").child(mCurrentName);

        // set the values
        title.setValue(mTitleET.getText().toString());
        type.setValue(mEventType);
        description.setValue(mDescriptionET.getText().toString());

        year.setValue(mEventStartYear);
        month.setValue(mEventStartMonth);
        day.setValue(mEventStartDay);
        hour.setValue(mEvenStartHour);
        minute.setValue(mEventStartMinute);

        creatorID.setValue(mCurrentUserId);
        creatorName.setValue(mCurrentName);

        attendingID.setValue(true);
        attendingName.setValue(true);
    }

    // add the event id to the user's data
    private void addEventToUserDB(String key){
        DatabaseReference createdID = mDbRef .child(mCurrentUserId).child("events").child("created").child(key);
        DatabaseReference attendingID = mDbRef .child(mCurrentUserId).child("events").child("attending").child(key);
        createdID.setValue(true);
        attendingID.setValue(true);
    }

    // navigate to interest fragment
    private void navigateToInterestFragment(){
        Fragment mFrag = new InterestFragment();
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mTransaction = mFragmentManager.beginTransaction();
        mTransaction.replace(R.id.grid_frag_container, mFrag);
        mTransaction.commit();
    }

    // navigate to around me fragment
    private void navigateToAroundMe(){
        Fragment mFrag = new EventsFragment();
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mTransaction = mFragmentManager.beginTransaction();
        mTransaction.replace(R.id.grid_frag_container, mFrag);
        mTransaction.commit();
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.start_time_bt){
            getEventStartTime();
        }
        else if (id == R.id.start_date_bt){
            getEventEndDate();
        }
        else if (id == R.id.create_button){
            create();
        }

    }
}
