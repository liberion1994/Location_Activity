package com.example.apple.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import controllers.UtilThreads;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class StartActivity extends AppCompatActivity {

    private Date startDate;
    private Date endDate;

    ArrayList<String> rawRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        Date curDate = new Date(System.currentTimeMillis());
        startDate = new Date((curDate.getTime() / (60 * 60 * 1000)) * 60 * 60 * 1000);
        endDate = new Date(startDate.getTime() + 60 * 60 * 1000);
        loadRecords(startDate, endDate);
    }

    Handler simpleHandler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle bundle = new Bundle();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
            bundle.putString("startDate", formatter.format(startDate));
            bundle.putString("endDate", formatter.format(endDate));
            String errorCode = null;
            if (msg.what == UtilThreads.LOAD_FINISHED) {
                rawRecords = (ArrayList<String>) msg.obj;
                errorCode = "FINISHED";
            } else if (msg.what == UtilThreads.LOAD_FAILED) {
                errorCode = "FAILED";
            }
            bundle.putString("errorCode", errorCode);
            bundle.putStringArrayList("records", rawRecords);

            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
            StartActivity.this.finish();

        }
    };


    protected void loadRecords(Date startDate, Date endDate) {
        UtilThreads.LoadThread loadThread = new UtilThreads.LoadThread(simpleHandler, startDate, endDate);
        loadThread.start();
    }

}
