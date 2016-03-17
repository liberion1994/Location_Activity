package space.liberion.locationfetchersimple;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import space.liberion.locationfetchersimple.controllers.UtilThreads;
import space.liberion.locationfetchersimple.models.PoiInfo;
import space.liberion.locationfetchersimple.models.Record;


public class MainActivity extends AppCompatActivity {


    public static final int STATUS_STAY = 0;
    public static final int STATUS_TRAVEL = 1;
    private static final int MAX_UNSAVED_RECORDS = 100;
    private static final String AmapSearchUrl = "http://restapi.amap.com/v3/place/around";
    private static final String ApiKey = "73f88d58ee45d48af7fdec77b7d3a05c";

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
                    new AlertDialog.Builder(MainActivity.this).setTitle("Choose a location").setItems(
                            items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    progressDialog = ProgressDialog.show(MainActivity.this, "", "Saving...");
                                    if (activityStatus == STATUS_STAY) {
                                        makeNodeBtn.setText("END TRAVEL");
                                        new UtilThreads.SaveNodeThread(new Date(System.currentTimeMillis()),
                                                STATUS_STAY, poiInfos[which].toTransText(), mainHandler).start();
                                        activityStatus = STATUS_TRAVEL;
                                    } else {
                                        makeNodeBtn.setText("START TRAVEL");
                                        new UtilThreads.SaveNodeThread(new Date(System.currentTimeMillis()),
                                                STATUS_TRAVEL, poiInfos[which].toTransText(), mainHandler).start();
                                        activityStatus = STATUS_STAY;
                                    }

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

    ArrayList<Record> unsavedRecordList;
    Record curRecord;

    int locationsNewFound = 0;
    int statusCode = 0;

    int activityStatus = STATUS_STAY;

    boolean isConnecting = false;

    AMapLocationClient aMapLocationClient;

    TextView recordView;
    Button makeNodeBtn;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recordView = (TextView) findViewById(R.id.record_view);
        makeNodeBtn = (Button) findViewById(R.id.makeNodeBtn);

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
                recordView.setText(curRecord.toDisplayString());
                unsavedRecordList.add(curRecord);
                locationsNewFound ++;
                if (unsavedRecordList.size() >= MAX_UNSAVED_RECORDS) {
                    if (!isConnecting) {
                        isConnecting = true;
                        new UtilThreads.SaveThread(mainHandler, unsavedRecordList).start();
                    }
                }
            }
        };
        AMapLocationClientOption aMapLocationClientOption = new AMapLocationClientOption();
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        aMapLocationClientOption.setInterval(5000);

        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.setLocationListener(aMapLocationListener);

        unsavedRecordList = new ArrayList<>();

        Intent startIntent = new Intent(this, NotificationService.class);
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
        aMapLocationClient.startLocation();
    }

    private void endFetchRecords() {
        if (statusCode != 1)
            return;
        statusCode = 0;
        aMapLocationClient.stopLocation();
    }

    public void makeNode(View view) {
        if (curRecord == null)
            return;
        String param = "key=" + ApiKey + "&location=" + curRecord.getLongitude() + "," +
                curRecord.getLatitude() + "&radius=1000";
        String urlStr = AmapSearchUrl + "?" + param;
        progressDialog = ProgressDialog.show(this, "", "Searching...");
        new UtilThreads.GetSearchResultThread(mainHandler, urlStr).start();

    }

}
