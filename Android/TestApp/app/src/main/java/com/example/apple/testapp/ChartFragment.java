package com.example.apple.testapp;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import models.MyActivity;
import models.Record;
import tools.GeometryUtil;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChartFragment extends Fragment implements MainActivity.MainActivityInterface {


    MainActivity fatherActivity;
    private TextView speed_btn;
    private TextView direction_btn;
    private LineChart chartArea;
    private TextView detailView;

    @Override
    public void onNewValidRecordsGot() {

    }

    @Override
    public void onTimeDurationChanged() {

    }

    @Override
    public void onFetchingStatusChanged() {

    }

    enum DataType { SPEED_DATA, DIRECTION_DATA };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fatherActivity = (MainActivity) getActivity();
        speed_btn = (TextView) fatherActivity.findViewById(R.id.speed_btn);
        direction_btn = (TextView) fatherActivity.findViewById(R.id.direction_btn);
        chartArea = (LineChart) fatherActivity.findViewById(R.id.speed_chart);
        detailView = (TextView) fatherActivity.findViewById(R.id.detail_view);

        speed_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speed_btn.setTextColor(Color.parseColor("#0088cc"));
                direction_btn.setTextColor(Color.parseColor("#666666"));
                setupChart(chartArea, getData(DataType.SPEED_DATA));
            }
        });

        direction_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                speed_btn.setTextColor(Color.parseColor("#666666"));
                direction_btn.setTextColor(Color.parseColor("#0088cc"));
                setupChart(chartArea, getData(DataType.DIRECTION_DATA));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        detailView.setText(getDetails());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            this.onResume();
        } else {
            this.onPause();
        }
    }

    private LineData getData(DataType dataType) {

        ArrayList<Entry> dataList = new ArrayList<>();
        Record lastRecord = null;
        Date firstTime = null;
        Date lastTime = null;
        for (Record record : fatherActivity.recordList) {
            if (lastRecord == null) {
                lastRecord = record;
                firstTime = record.getTimepoint();
                dataList.add(new Entry((float)0, 0));
                lastTime = firstTime;
                continue;
            }

            int ind = (int) ((record.getTimepoint().getTime() - firstTime.getTime()) / 1000);
            float yValue = 0;
            switch (dataType) {
                case SPEED_DATA:
                    yValue = (float)getSpeed(record, lastRecord);
                    break;
                case DIRECTION_DATA:
                    yValue = (float)getDirection(record, lastRecord);
                    break;
            }
            dataList.add(new Entry(yValue, ind));
            lastRecord = record;
            lastTime = record.getTimepoint();
        }

        // if no data, return null
        if (firstTime == null) {
            return null;
        }

        ArrayList<String> xValues = getXDateAxis(firstTime, lastTime);

        LineDataSet lineDataSet = new LineDataSet(dataList, dataType.name() /*显示在比例图上*/);
        lineDataSet.setLineWidth(3f); // 线宽
//        lineDataSet.setCircleSize(3f);// 显示的圆形大小
        lineDataSet.setColor(Color.parseColor("#0066cc"));// 显示颜色
        lineDataSet.setCircleColor(Color.parseColor("#0066cc"));// 圆形的颜色
//        lineDataSet.setHighLightColor(Color.YELLOW); // 高亮的线的颜色
//        lineDataSet.setDrawCubic(true);

        ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();
        lineDataSets.add(lineDataSet); // add the datasets

        // create a data object with the datasets
        LineData lineData = new LineData(xValues, lineDataSets);
        return lineData;
    }

    private void setupChart(LineChart chart, LineData data) {
        if (data == null)
            return;
        chart.setData(data);

        Legend mLegend = chart.getLegend();
        mLegend.setForm(Legend.LegendForm.CIRCLE);// 样式
        mLegend.setFormSize(6f);// 字体
        mLegend.setTextColor(Color.BLUE);// 颜色


        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);

        chart.setDescription("");
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisRight().setEnabled(false);
        chart.invalidate();
    }

    private double getDistance(Record record, Record lastRecord) {
        return AMapUtils.calculateLineDistance(new LatLng(record.getLatitude(), record.getLongitude()),
                new LatLng(lastRecord.getLatitude(), lastRecord.getLongitude()));
    }

    private double getSpeed(Record record, Record lastRecord) {
        double secondInterval = (record.getTimepoint().getTime() - lastRecord.getTimepoint().getTime()) / 1000;
        return getDistance(record, lastRecord) / secondInterval;
    }

    private double getDirection(Record record, Record lastRecord) {
        GeometryUtil.MyLatLng myLatLng = new GeometryUtil.MyLatLng(record.getLatitude(), record.getLongitude());
        GeometryUtil.MyLatLng myLatLng2 = new GeometryUtil.MyLatLng(lastRecord.getLatitude(), lastRecord.getLongitude());
        return GeometryUtil.getAngle(myLatLng, myLatLng2);
    }


    private ArrayList<String> getXDateAxis(Date firstTime, Date lastTime) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        int intSec = (int) ((lastTime.getTime() - firstTime.getTime()) / 1000) + 1;
        ArrayList<String> xValues = new ArrayList<>();
        for (int i = 0; i <= intSec; i ++) {
            Date date = new Date(firstTime.getTime() + i * 1000);
            xValues.add(simpleDateFormat.format(date));
        }
        return xValues;
    }

    private String getDetails() {
        String res = "";
        Record lastRecord = null;
        Record stRecord = null;
        Record enRecord = null;
        double totalDistance = 0;
        for (Record record : fatherActivity.recordList) {
            if (lastRecord == null) {
                enRecord = stRecord = lastRecord = record;
                continue;
            }
            totalDistance += getDistance(record, lastRecord);
            enRecord = lastRecord = record;
        }
        if (stRecord == null || stRecord == enRecord)
            return "No data.";

        MyActivity activity = new MyActivity(fatherActivity.recordList);
        res += activity.toDisplayString();
        double timeInterval = (enRecord.getTimepoint().getTime() - stRecord.getTimepoint().getTime()) / 1000;
        double averageSpeed1 = totalDistance / timeInterval;
        double averageSpeed2 = getDistance(enRecord, stRecord) / timeInterval;
        res += "\n平均速率: " + averageSpeed1 + "\n平均速度: " + averageSpeed2;
        return res;
    }
}
