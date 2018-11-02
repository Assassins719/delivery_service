package com.courier.services.kohcw;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class ListArrayAdapter extends ArrayAdapter<String> {
    private final Context context;
    private ArrayList<String> values = new ArrayList<>();

    public ListArrayAdapter(Context context, ArrayList<String> values) {
        super(context, R.layout.listitem, values);
        this.context = context;
        this.values.addAll(values);
    }

    public void setData(ArrayList<String> values) {
        this.values.clear();
        this.values.addAll(values);
    }

    public ArrayList<String> getData() {
        return values;
    }

    public void addData(String strNo) {
        this.values.add(strNo);
    }
    public void removeData(int pos){
        this.values.remove(pos);
    }
    @Override
    public int getCount()
    {
        return values.size();
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if(listItemView == null){
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.listitemsignin,parent,false
            );
        }
        TextView tx_name;
        Button btn_remove;
        tx_name = listItemView.findViewById(R.id.tx_order);
        btn_remove = listItemView.findViewById(R.id.btn_remove);
        tx_name.setText(values.get(position));
        btn_remove.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                removeData(position);
                notifyDataSetChanged();
            }
        });
        if (position % 2 == 1) {
            // Set a background color for ListView regular row/item
            listItemView.setBackgroundColor(Color.parseColor("#ffffff"));
        } else {
            // Set the background color for alternate row/item
            listItemView.setBackgroundColor(Color.parseColor("#EdF3db"));
        }
        return listItemView;
    }
}