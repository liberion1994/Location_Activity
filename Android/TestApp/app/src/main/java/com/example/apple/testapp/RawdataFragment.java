package com.example.apple.testapp;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import models.Record;


/**
 * A simple {@link Fragment} subclass.
 */
public class RawdataFragment extends Fragment implements MainActivity.MainActivityInterface {

    TextView statusView;
    ListView recordsView;

    MainActivity fatherActivity;

    private boolean isForeground = false;
    private boolean needRefresh = true;

    List<HashMap<String, Object>> recordsViewList;
    SimpleAdapter recordsAdapter;

    private Date newStartDate;
    private Date newEndDate;

    public RawdataFragment() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rawdata, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fatherActivity = (MainActivity)getActivity();
        statusView = (TextView) fatherActivity.findViewById(R.id.status);
        recordsView = (ListView) fatherActivity.findViewById(R.id.records_view);

        this.registerForContextMenu(recordsView);
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
    public void onResume() {
        super.onResume();
        if (needRefresh)
            refreshAll();
        needRefresh = false;
        isForeground = true;
    }

    @Override
    public void onPause() {
        isForeground = false;
        super.onPause();
    }

    private HashMap<String, Object> makeItem(Record record) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        HashMap<String, Object> item = new HashMap<>();
        item.put("item_date", simpleDateFormat.format(record.getTimepoint()));
        item.put("item_location", "" + record.getLatitude() + " " + record.getLongitude() + " " + record.getAccuracy());
        item.put("item_description", record.getDescription());
        item.put("item_self", record);
        return item;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, 0, Menu.NONE, "在地图中显示");
        menu.add(0, 1, Menu.NONE, "设置为开始时间");
        menu.add(0, 2, Menu.NONE, "设置为截止时间");
        menu.add(0, 3, Menu.NONE, "逆地理编码");
        menu.add(0, 4, Menu.NONE, "设置为TRAVEL开始节点");
        menu.add(0, 5, Menu.NONE, "设置为TRAVEL结束节点");
        menu.add(0, 6, Menu.NONE, "设置为STAY开始节点");
        menu.add(0, 7, Menu.NONE, "设置为STAY结束节点");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        HashMap<String, Object> chosenItem = (HashMap<String, Object>) recordsView.getItemAtPosition(info.position);
        Record record = (Record) chosenItem.get("item_self");
        double latitude = record.getLatitude();
        double longitude = record.getLongitude();
        Date date = record.getTimepoint();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

        switch (item.getItemId()) {
            case 0:
                fatherActivity.showInMap(record);
                break;
            case 1:
                newStartDate = date;
                Date tmpEndDate = fatherActivity.endDate;
                if (newEndDate != null) {
                    tmpEndDate = newEndDate;
                }
                String messsage = "从" + simpleDateFormat.format(newStartDate) + "\n到" +
                        simpleDateFormat.format(tmpEndDate) + "?";
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(messsage);
                builder.setTitle("提交更改");
                builder.setPositiveButton("立刻", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (newEndDate == null) {
                            fatherActivity.makeTimeDurationUpdate(newStartDate, fatherActivity.endDate);
                        } else {
                            fatherActivity.makeTimeDurationUpdate(newStartDate, newEndDate);
                        }
                        newStartDate = newEndDate = null;
                    }
                });
                builder.setNegativeButton("稍后", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.show();
                break;
            case 2:
                newEndDate = date;
                Date tmpStartDate = fatherActivity.startDate;
                if (newStartDate != null) {
                    tmpStartDate = newStartDate;
                }
                String messsage2 = "从" + simpleDateFormat.format(tmpStartDate) + "\n到" +
                        simpleDateFormat.format(newEndDate) + "?";
                AlertDialog.Builder builder2 = new AlertDialog.Builder(getContext());
                builder2.setMessage(messsage2);
                builder2.setTitle("提交更改");
                builder2.setPositiveButton("立刻", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (newStartDate == null) {
                            fatherActivity.makeTimeDurationUpdate(fatherActivity.startDate, newEndDate);
                        } else {
                            fatherActivity.makeTimeDurationUpdate(newStartDate, newEndDate);
                        }
                        newStartDate = newEndDate = null;
                    }
                });
                builder2.setNegativeButton("稍后", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder2.show();
                break;
            case 3:
                MainActivity.showRegeocodeInfoDialog(longitude, latitude, getContext());
                break;
            case 4:
                fatherActivity.addActivityNode(MainActivity.TYPE_START_TRAVEL, record);
                break;
            case 5:
                fatherActivity.addActivityNode(MainActivity.TYPE_END_TRAVEL, record);
                break;
            case 6:
                fatherActivity.addActivityNode(MainActivity.TYPE_START_ACTIVITY, record);
                break;
            case 7:
                fatherActivity.addActivityNode(MainActivity.TYPE_END_ACTIVITY, record);
                break;
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    public void refreshRecords() {
        recordsViewList = new ArrayList<>();
        for (Record record : fatherActivity.recordList) {
            recordsViewList.add(0, makeItem(record));
        }
        recordsAdapter = new SimpleAdapter(fatherActivity, recordsViewList, R.layout.records_view_item,
                new String[] {"item_date", "item_location", "item_description"},
                new int[] {R.id.item_date, R.id.item_location, R.id.item_description});
        recordsView.setAdapter(recordsAdapter);
    }

    public void refreshStatus() {
        statusView.setText("新获取: " + fatherActivity.locationsNewFound + "  总数: " + recordsViewList.size());
    }

    public void refreshAll() {
        refreshRecords();
        refreshStatus();
    }


    @Override
    public void onNewValidRecordsGot() {
        if (isForeground) {
            Record record = fatherActivity.curRecord;
            recordsViewList.add(0, makeItem(record));
            recordsAdapter.notifyDataSetChanged();
        } else {
            needRefresh = true;
        }
    }

    @Override
    public void onTimeDurationChanged() {
        if (isForeground) {
            refreshAll();
        } else {
            needRefresh = true;
        }
    }

    @Override
    public void onFetchingStatusChanged() {
        if (isForeground) {
            refreshStatus();
        } else {
            needRefresh = true;
        }
    }
}
