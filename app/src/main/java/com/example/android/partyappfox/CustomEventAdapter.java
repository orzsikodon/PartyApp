package com.example.android.partyappfox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;

/**
 * Created by odonorzsik on 2/13/18.
 */

public class CustomEventAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<DataSnapshot> mDataEventList;
    private double mUserLat;
    private double mUserLon;


    public CustomEventAdapter(Context context, ArrayList<DataSnapshot> list, double lat, double lon){
        this.mDataEventList = list;
        this.mUserLat = lat;
        this.mUserLon = lon;
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mDataEventList.size();
    }

    @Override
    public Object getItem(int i) {
        return mDataEventList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        myContainer cont;

        if (convertView == null){
            convertView = mInflater.inflate(R.layout.custom_cell, null);
            cont = new myContainer();
            cont.titleTV = convertView.findViewById(R.id.top_row_title);
            cont.typeTV = convertView.findViewById(R.id.bottom_row_type);
            cont.creatorTV = convertView.findViewById(R.id.top_row_creator);
            cont.distanceTV = convertView.findViewById(R.id.bottom_row_distance);

            convertView.setTag(cont);
        }
        else {
            cont = (myContainer) convertView.getTag();
        }

        // set the values from the DataSnapshot list
        String title = (String) mDataEventList.get(i).child("eventData").child("title").getValue();
        String type = (String) mDataEventList.get(i).child("eventData").child("type").getValue();
        String creator = (String) mDataEventList.get(i).child("eventData").child("creator").child("name").getValue();
        String distance = "???";

        // set them
        cont.titleTV.setText(title);
        cont.typeTV.setText("Type: " + type);
        cont.creatorTV.setText("Organizer: " + creator);
        cont.distanceTV.setText("Distance: " + distance);

        return convertView;
    }

    // inside class for the text views
    static class myContainer {
        TextView titleTV;
        TextView typeTV;
        TextView creatorTV;
        TextView distanceTV;
    }

    protected void clearAdapter(){
        mDataEventList.clear();
        notifyDataSetChanged();
    }
}
