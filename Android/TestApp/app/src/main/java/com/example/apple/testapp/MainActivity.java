package com.example.apple.testapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.geocoder.RegeocodeRoad;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import controllers.UtilThreads;
import models.MyActivity;
import models.PoiInfo;
import models.Record;


public class MainActivity extends AppCompatActivity {

    protected interface MainActivityInterface {
        void onNewValidRecordsGot();
        void onTimeDurationChanged();
        void onFetchingStatusChanged();
    }

    private static final int NEW_RECORDS_GOT = 0;
    private static final int TIME_DURATION_CHANGED = 1;
    private static final int FETCHING_STATUS_CHANGED = 2;

    public static final int TYPE_START_TRAVEL = 0;
    public static final int TYPE_END_TRAVEL = 1;
    public static final int TYPE_START_ACTIVITY = 2;
    public static final int TYPE_END_ACTIVITY = 3;

    private static final String AmapSearchUrl = "http://restapi.amap.com/v3/place/around";
    private static final String ApiKey = "73f88d58ee45d48af7fdec77b7d3a05c";


    private static final int MAX_UNSAVED_RECORDS = 100;

    Handler mainHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == UtilThreads.TIME_INTERVAL_REACHED) {
                
            } else if (msg.what == UtilThreads.SAVE_FINISHED) {
                unsavedRecordList.clear();
                isConnecting = false;
            } else if (msg.what == UtilThreads.SAVE_FAILED) {
                isConnecting = false;
            } else if (msg.what == UtilThreads.SAVE_NODE_FINISHED) {
                progressDialog.dismiss();
            } else if (msg.what == UtilThreads.SAVE_NODE_FAILED) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "SAVE FAILED", Toast.LENGTH_SHORT).show();
            } else if (msg.what == UtilThreads.SEARCH_FINISHED) {
                String text = (String) msg.obj;
                progressDialog.dismiss();
                try {
                    JSONObject object = new JSONObject(text);
                    JSONArray pois = object.getJSONArray("pois");
                    String[] items = new String[pois.length()];
                    final PoiInfo[] poiInfos = new PoiInfo[pois.length()];
                    for (int i = 0; i < pois.length(); i ++) {
                        poiInfos[i] = new PoiInfo(pois.getString(i));
                        items[i] = poiInfos[i].toDisplayText();
                    }
                    new AlertDialog.Builder(MainActivity.this).setTitle("选择一个候选地点").setItems(
                            items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    progressDialog = ProgressDialog.show(MainActivity.this, "", "上传中...");
                                    new UtilThreads.SaveNodeThread(ChosenRecordDate, ActivityNodeType,
                                            poiInfos[which].toTransText(), mainHandler).start();
                                }
                            }
                    ).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (msg.what == UtilThreads.SEARCH_FAILED) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, "SEARCH FAILED", Toast.LENGTH_SHORT).show();
            }
        }
    };

    ArrayList<Record> recordList;
    ArrayList<Record> unsavedRecordList;
    Record curRecord;

    ArrayList<MyActivity> activities;
    int curActivityIndex = -1;

    Date startDate;
    Date endDate;

    int locationsNewFound = 0;
    int statusCode = 0;
    int curTab = 1;

    int ActivityNodeType;
    Date ChosenRecordDate;

    boolean isConnecting = false;

    ChartFragment chartFragment;
    MapFragment mapFragment;
    RawdataFragment rawdataFragment;
    AnalysisFragment analysisFragment;

    AMapLocationClient aMapLocationClient;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aMapLocationClient = new AMapLocationClient(getApplicationContext());
        AMapLocationListener aMapLocationListener = new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation.getErrorCode() != 0) {
                    return;
                }
                String desc = "" + aMapLocation.getCountry() + "=>" + aMapLocation.getProvince() + "=>" +
                        aMapLocation.getCity() + "=>" + aMapLocation.getDistrict() + "=>" +
                        aMapLocation.getStreet() + "=>" + aMapLocation.getStreetNum() + "=>" +
                        aMapLocation.getPoiName() + "=>" + aMapLocation.getAdCode() + "=>";
                if (aMapLocation.getLocationType() == 1) {
                    desc += "GPS";
                } else if (aMapLocation.getLocationType() == 2) {
                    desc += "PREV";
                } else if (aMapLocation.getLocationType() == 4) {
                    desc += "CACHED";
                } else if (aMapLocation.getLocationType() == 5) {
                    desc += "WIFI";
                } else if (aMapLocation.getLocationType() == 6) {
                    desc += "BS";
                }
                curRecord = new Record(new Date(System.currentTimeMillis()), aMapLocation.getLongitude(),
                        aMapLocation.getLatitude(), aMapLocation.getAccuracy(), desc);
                unsavedRecordList.add(curRecord);
                locationsNewFound ++;
                if (unsavedRecordList.size() >= MAX_UNSAVED_RECORDS) {
                    if (!isConnecting) {
                        isConnecting = true;
                        new UtilThreads.SaveThread(mainHandler, unsavedRecordList).start();
                    }
                }
                if (curRecord.getTimepoint().getTime() >= startDate.getTime() &&
                        curRecord.getTimepoint().getTime() <= endDate.getTime()) {
                    recordList.add(curRecord);
                    notifyChildFragments(NEW_RECORDS_GOT);
                }
                notifyChildFragments(FETCHING_STATUS_CHANGED);
            }
        };
        AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        aMapLocationClientOption.setInterval(5000);

        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.setLocationListener(aMapLocationListener);

        Bundle bundle = this.getIntent().getExtras();

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
            startDate = formatter.parse(bundle.getString("startDate"));
            endDate = formatter.parse(bundle.getString("endDate"));
        } catch (ParseException e) {
            Date curDate = new Date(System.currentTimeMillis());
            startDate = new Date(curDate.getTime() - 60 * 60 * 1000);
            endDate = new Date(curDate.getTime() + 60 * 60 * 1000);
        }

        if (bundle.getString("errorCode").equals("FAILED")) {
            Toast.makeText(this, "无法连接到服务器", Toast.LENGTH_SHORT).show();
        }
        unsavedRecordList = new ArrayList<>();
        recordList = new ArrayList<>();
        rawRecordsToRecords(bundle.getStringArrayList("records"));

        resetTimeView();
        setInitFragment();

        Intent startIntent = new Intent(this, FetchLocationService.class);
        startService(startIntent);
    }


    @Override
    protected void onStop() {
        if (!isConnecting) {
            isConnecting = true;
            new UtilThreads.SaveThread(mainHandler, unsavedRecordList).start();
        }
        super.onStop();
    }

    public void changeStatus(View view) {
        if (statusCode == 0) {
            ((FloatingActionButton) findViewById(R.id.float_btn)).setImageResource(R.drawable.btn_stop);
            startFetchRecords();
        } else if (statusCode == 1) {
            ((FloatingActionButton) findViewById(R.id.float_btn)).setImageResource(R.drawable.btn_start);
            endFetchRecords();
        }
    }

    public void startFetchRecords() {
        if (statusCode != 0)
            return;
        statusCode = 1;
        notifyChildFragments(FETCHING_STATUS_CHANGED);
        aMapLocationClient.startLocation();
    }

    private void endFetchRecords() {
        if (statusCode != 1)
            return;
        statusCode = 0;
        notifyChildFragments(FETCHING_STATUS_CHANGED);
        aMapLocationClient.stopLocation();
//        autoSaver.endLoop();
    }

    private void setInitFragment() {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        rawdataFragment = new RawdataFragment();
        mapFragment = new MapFragment();
        chartFragment = new ChartFragment();
        analysisFragment = new AnalysisFragment();
        transaction.replace(R.id.content_fragment, rawdataFragment);
        transaction.commit();
    }

    private void notifyChildFragments(int type) {
        switch (type) {
            case NEW_RECORDS_GOT:
                rawdataFragment.onNewValidRecordsGot();
                mapFragment.onNewValidRecordsGot();
                chartFragment.onNewValidRecordsGot();
                analysisFragment.onNewValidRecordsGot();
                break;
            case FETCHING_STATUS_CHANGED:
                rawdataFragment.onFetchingStatusChanged();
                mapFragment.onFetchingStatusChanged();
                chartFragment.onFetchingStatusChanged();
                analysisFragment.onFetchingStatusChanged();
                break;
            case TIME_DURATION_CHANGED:
                rawdataFragment.onTimeDurationChanged();
                mapFragment.onTimeDurationChanged();
                chartFragment.onTimeDurationChanged();
                analysisFragment.onTimeDurationChanged();
                break;
            default:
                break;
        }
    }

    public void changeFragment(View view) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        if (curTab == 1) {
            ((ImageView) findViewById(R.id.tab_main)).setImageResource(R.drawable.list_btn);
            transaction.hide(rawdataFragment);
        } else if (curTab == 2) {
            ((ImageView) findViewById(R.id.tab_map)).setImageResource(R.drawable.map_btn);
            transaction.hide(mapFragment);
        } else if (curTab == 3) {
            ((ImageView) findViewById(R.id.tab_chart)).setImageResource(R.drawable.chart_btn);
            transaction.hide(chartFragment);
        } else if (curTab == 4) {
            ((ImageView) findViewById(R.id.tab_analysis)).setImageResource(R.drawable.activity_btn);
            transaction.hide(analysisFragment);
        }

        switch (view.getId()) {
            case R.id.tab_main:
                if (rawdataFragment == null)
                    rawdataFragment = new RawdataFragment();
                if (!rawdataFragment.isAdded()) {
                    transaction.add(R.id.content_fragment, rawdataFragment);
                } else {
                    transaction.show(rawdataFragment);
                }
                curTab = 1;
                ((ImageView)view).setImageResource(R.drawable.list_btn_2);
                break;
            case R.id.tab_map:
                if (mapFragment == null)
                    mapFragment = new MapFragment();
                if (!mapFragment.isAdded()) {
                    transaction.add(R.id.content_fragment, mapFragment);
                } else {
                    transaction.show(mapFragment);
                }
                curTab = 2;
                ((ImageView)view).setImageResource(R.drawable.map_btn_2);
                break;
            case R.id.tab_chart:
                if (chartFragment == null)
                    chartFragment = new ChartFragment();
                if (!chartFragment.isAdded()) {
                    transaction.add(R.id.content_fragment, chartFragment);
                } else {
                    transaction.show(chartFragment);
                }
                curTab = 3;
                ((ImageView)view).setImageResource(R.drawable.chart_btn_2);
                break;
            case R.id.tab_analysis:
                if (analysisFragment == null)
                    analysisFragment = new AnalysisFragment();
                if (!analysisFragment.isAdded()) {
                    transaction.add(R.id.content_fragment, analysisFragment);
                } else {
                    transaction.show(analysisFragment);
                }
                curTab = 4;
                ((ImageView)view).setImageResource(R.drawable.activity_btn_2);
        }
        transaction.commit();
    }

    public void updateTimeDuration(View view) {
        EditText fromYear = (EditText) findViewById(R.id.from_year);
        EditText fromMonth = (EditText) findViewById(R.id.from_month);
        EditText fromDay = (EditText) findViewById(R.id.from_day);
        EditText fromHour = (EditText) findViewById(R.id.from_hour);
        EditText fromMinute = (EditText) findViewById(R.id.from_minute);
        EditText fromSecond = (EditText) findViewById(R.id.from_second);
        EditText toYear = (EditText) findViewById(R.id.to_year);
        EditText toMonth = (EditText) findViewById(R.id.to_month);
        EditText toDay = (EditText) findViewById(R.id.to_day);
        EditText toHour = (EditText) findViewById(R.id.to_hour);
        EditText toMinute = (EditText) findViewById(R.id.to_minute);
        EditText toSecond = (EditText) findViewById(R.id.to_second);

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
            String fromDateString = fromYear.getText().toString() + "-" + fromMonth.getText().toString() + "-" +
                    fromDay.getText().toString() + "," + fromHour.getText().toString() + ":" +
                    fromMinute.getText().toString() + ":" + fromSecond.getText().toString();
            Date newFromDate = formatter.parse(fromDateString);

            String toDateString = toYear.getText().toString() + "-" + toMonth.getText().toString() + "-" +
                    toDay.getText().toString() + "," + toHour.getText().toString() + ":" +
                    toMinute.getText().toString() + ":" + toSecond.getText().toString();
            Date newToDate = formatter.parse(toDateString);
            makeTimeDurationUpdate(newFromDate, newToDate);
        } catch (ParseException e) {
        }
    }

    protected void addActivityNode(int type, Record record) {
        ActivityNodeType = type;
        ChosenRecordDate = record.getTimepoint();
        String param = "key=" + ApiKey + "&location=" + record.getLongitude() + "," +
                record.getLatitude() + "&radius=1000";
        String urlStr = AmapSearchUrl + "?" + param;
        progressDialog = ProgressDialog.show(this, "", "获取候选地点...");
        new UtilThreads.GetSearchResultThread(mainHandler, urlStr).start();
    }

    protected void showInMap(Record record) {
        if (curTab != 1)
            return;
        ((ImageView) findViewById(R.id.tab_main)).setImageResource(R.drawable.list_btn);
        ((ImageView) findViewById(R.id.tab_map)).setImageResource(R.drawable.map_btn_2);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.hide(rawdataFragment);
        if (mapFragment == null)
            mapFragment = new MapFragment();
        mapFragment.setHighlightRecord(record);
        if (!mapFragment.isAdded()) {
            transaction.add(R.id.content_fragment, mapFragment);
        } else {
            transaction.show(mapFragment);
        }
        curTab = 2;
        transaction.commit();
    }

    protected void showActivityInMap(int index) {
        if (curTab != 4)
            return;
        this.curActivityIndex = index;

        ((ImageView) findViewById(R.id.tab_analysis)).setImageResource(R.drawable.activity_btn);
        ((ImageView) findViewById(R.id.tab_map)).setImageResource(R.drawable.map_btn_2);

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.hide(analysisFragment);
        if (mapFragment == null)
            mapFragment = new MapFragment();
        mapFragment.setDrawContent(MapFragment.CURRENT_ACTIVITY);
        if (!mapFragment.isAdded()) {
            transaction.add(R.id.content_fragment, mapFragment);
        } else {
            transaction.show(mapFragment);
        }
        curTab = 2;
        transaction.commit();
    }

    private void rawRecordsToRecords(ArrayList<String> rawRecords) {
        recordList.clear();
        for (String rawRecord : rawRecords) {
            Record record = Record.parseString(rawRecord);
            if (record != null)
                recordList.add(record);
        }
    }

    public void changeNextHour(View view) {
        Date newEndDate = new Date(endDate.getTime() + 60 * 60 * 1000);
        makeTimeDurationUpdate(endDate, newEndDate);
    }

    public void changePrevHour(View view) {
        Date newStartDate = new Date(startDate.getTime() - 60 * 60 * 1000);
        makeTimeDurationUpdate(newStartDate, startDate);
    }

    public void makeTimeDurationUpdate(final Date newStartDate, final Date newEndDate) {

        progressDialog = ProgressDialog.show(this, "", "正在应用更改...");

        Handler saveAndLoadHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == UtilThreads.SAVE_FINISHED) {
                    unsavedRecordList.clear();
                    new UtilThreads.LoadThread(this, newStartDate, newEndDate).start();
                    return;
                } else if (msg.what == UtilThreads.LOAD_FINISHED) {
                    startDate = newStartDate;
                    endDate = newEndDate;
                    resetTimeView();
                    rawRecordsToRecords((ArrayList<String>) msg.obj);
                    notifyChildFragments(TIME_DURATION_CHANGED);
                } else if (msg.what == UtilThreads.SAVE_FAILED) {
                    Toast.makeText(MainActivity.this, "无法连接到服务器，更改区间失败", Toast.LENGTH_SHORT).show();
                } else if (msg.what == UtilThreads.LOAD_FAILED) {
                    Toast.makeText(MainActivity.this, "无法连接到服务器，更改区间失败", Toast.LENGTH_SHORT).show();
                }
                isConnecting = false;
                progressDialog.dismiss();
            }
        };
        while (isConnecting) {}
        isConnecting = true;
        if (!unsavedRecordList.isEmpty())
            new UtilThreads.SaveThread(saveAndLoadHandler, unsavedRecordList).start();
        else
            new UtilThreads.LoadThread(saveAndLoadHandler, newStartDate, newEndDate).start();
    }

    protected static void showRegeocodeInfoDialog(double longitude, double latitude, final Context context) {
        GeocodeSearch geocodeSearch = new GeocodeSearch(context);
        geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                String target;
                if (regeocodeResult == null) {
                    target = "无法得到逆地理编码信息.";
                } else {
                    RegeocodeAddress add = regeocodeResult.getRegeocodeAddress();
                    target = formatRegeocodeAddr(add);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(target);
                builder.setTitle("逆地理编码信息");
                builder.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

            }
        });
        RegeocodeQuery regeocodeQuery = new RegeocodeQuery(new LatLonPoint(latitude, longitude),
                100, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(regeocodeQuery);
    }


    private static String formatRegeocodeAddr(RegeocodeAddress addr) {
        String ret = "";
        ret += addr.getProvince() + addr.getCity() + addr.getDistrict() +
                addr.getStreetNumber().getStreet() + addr.getStreetNumber().getNumber() +
                addr.getNeighborhood() + addr.getBuilding() + "\n附近兴趣点:";
        for (PoiItem poiItem : addr.getPois()) {
            ret += "\n    " + poiItem.getTitle();
        }
        ret += "\n附近道路: ";
        for (RegeocodeRoad road : addr.getRoads()) {
            ret += "\n    " + road.getName();
        }

        return ret;
    }

    private void resetTimeView() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ((TextView) this.findViewById(R.id.time_view)).setText(
                formatter.format(startDate) + " -- " + formatter.format(endDate));
    }

    public void getActivities(View view) {
        activities = MyActivity.splitActivities(recordList, 2*60*1000);
        curActivityIndex = -1;
        mapFragment.setDrawContent(MapFragment.ALL_RECORDS);
        analysisFragment.refreshActivities();
    }

    public MyActivity getCurActivity() {
        if (curActivityIndex == -1)
            return null;
        return activities.get(curActivityIndex);
    }

    public void getNextActivity() {
        if (curActivityIndex == -1)
            return;
        if (curActivityIndex == activities.size() - 1)
            return;
        curActivityIndex ++;
    }

    public void getPrevActivity() {
        if (curActivityIndex <= 0)
            return;
        curActivityIndex --;
    }

}
