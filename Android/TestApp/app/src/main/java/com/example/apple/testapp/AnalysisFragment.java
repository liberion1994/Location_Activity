package com.example.apple.testapp;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import models.MyActivity;
import models.Record;


/**
 * A simple {@link Fragment} subclass.
 */
public class AnalysisFragment extends Fragment implements MainActivity.MainActivityInterface {

    MainActivity fatherActivity;
    ListView activitiesView;

    private boolean isForeground = false;
    private boolean needRefresh = true;

    List<HashMap<String, Object>> activitiesViewList;
    SimpleAdapter activitiesAdapter;

    public AnalysisFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_analysis, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fatherActivity = (MainActivity)getActivity();
        activitiesView = (ListView) fatherActivity.findViewById(R.id.activity_list_view);
        activitiesView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int index = (int) ((HashMap<String, Object>) parent.getItemAtPosition(position))
                        .get("item_index");
                fatherActivity.showActivityInMap(index);
                return true;
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            this.onResume();
        } else {
            this.onPause();
        }
    }

    @Override
    public void onPause() {
        isForeground = false;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (needRefresh)
            refreshAll();
        needRefresh = false;
        isForeground = true;
    }

    @Override
    public void onNewValidRecordsGot() {

    }

    @Override
    public void onTimeDurationChanged() {
        if (!isForeground) {
            needRefresh = true;
        }
    }

    @Override
    public void onFetchingStatusChanged() {

    }

    private void refreshDateText() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy MM dd HH mm ss");
        String []fromDates = formatter.format(fatherActivity.startDate).split(" ");
        String []toDates = formatter.format(fatherActivity.endDate).split(" ");

        EditText fromYear = (EditText) fatherActivity.findViewById(R.id.from_year);
        EditText fromMonth = (EditText) fatherActivity.findViewById(R.id.from_month);
        EditText fromDay = (EditText) fatherActivity.findViewById(R.id.from_day);
        EditText fromHour = (EditText) fatherActivity.findViewById(R.id.from_hour);
        EditText fromMinute = (EditText) fatherActivity.findViewById(R.id.from_minute);
        EditText fromSecond = (EditText) fatherActivity.findViewById(R.id.from_second);
        EditText toYear = (EditText) fatherActivity.findViewById(R.id.to_year);
        EditText toMonth = (EditText) fatherActivity.findViewById(R.id.to_month);
        EditText toDay = (EditText) fatherActivity.findViewById(R.id.to_day);
        EditText toHour = (EditText) fatherActivity.findViewById(R.id.to_hour);
        EditText toMinute = (EditText) fatherActivity.findViewById(R.id.to_minute);
        EditText toSecond = (EditText) fatherActivity.findViewById(R.id.to_second);
        fromYear.setText(fromDates[0]);
        fromMonth.setText(fromDates[1]);
        fromDay.setText(fromDates[2]);
        fromHour.setText(fromDates[3]);
        fromMinute.setText(fromDates[4]);
        fromSecond.setText(fromDates[5]);
        toYear.setText(toDates[0]);
        toMonth.setText(toDates[1]);
        toDay.setText(toDates[2]);
        toHour.setText(toDates[3]);
        toMinute.setText(toDates[4]);
        toSecond.setText(toDates[5]);
    }

    private HashMap<String, Object> makeItem(MyActivity activity, int index) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        HashMap<String, Object> item = new HashMap<>();
        String type = "";
        if (activity.getActivityType() == MyActivity.STAY)
            type = "STAY";
        else if (activity.getActivityType() == MyActivity.TRAVEL)
            type = "TRAVEL";
        item.put("item_date", simpleDateFormat.format(activity.getStartDate()) + " -- " +
                simpleDateFormat.format(activity.getEndDate()));
        item.put("item_info", "" + activity.getTrace().size() + "条记录 " + type + "\n平均圆心距: " +
                activity.getAverageCenterDistance() + ", 平均标准化内积: " + activity.getAverageVectorProduct());
        item.put("item_area", "圆心: " + activity.getArea().center.longitude + "," + activity.getArea().center.latitude +
                ", 半径: " + activity.getArea().radius);
        item.put("item_index", index);
        return item;
    }

    public void refreshActivities() {
        if (fatherActivity.activities == null)
            return;
        activitiesViewList = new ArrayList<>();
        int ind = 0;
        for (MyActivity activity : fatherActivity.activities) {
            activitiesViewList.add(makeItem(activity, ind ++));
        }
        activitiesAdapter = new SimpleAdapter(fatherActivity, activitiesViewList, R.layout.activity_view_item,
                new String[] {"item_date", "item_info", "item_area"},
                new int[] {R.id.activity_date, R.id.activity_info, R.id.activity_area});
        activitiesView.setAdapter(activitiesAdapter);
    }

    public void refreshAll() {
        refreshDateText();
        refreshActivities();
    }
}
