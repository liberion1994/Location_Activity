package com.example.apple.testapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import models.MyActivity;
import models.Record;
import tools.GeometryUtil;

/**
 * A simple {@link Fragment} subclass.
 */
public class MapFragment extends Fragment implements MainActivity.MainActivityInterface {

    protected static final int CURRENT_ACTIVITY = 0;
    protected static final int ALL_RECORDS = 1;

    private MapView mapView;
    private AMap aMap;
    private TextView infoView;
    private FloatingActionButton drawCoverBtn;
    private Button showAllBtn;
    private Button showActBtn;
    private Button showPrevBtn;
    private Button showNextBtn;
    private Button showTraceBtn;

    private ArrayList<Marker> markers = new ArrayList<>();
    private Polyline polyline;
    private Circle accuracyCircle;
    private Circle minCoverCircle;

    private boolean isForeground = false;
    private boolean needRefresh = true;

    private Record highlightRecord;

    private LatLng centerLatLng;
    private LatLng chosenLatLng;
    private float zoom = 12;

    private int drawContent = 1;

    MainActivity fatherActivity;

    public MapFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fatherActivity = (MainActivity) getActivity();
        mapView = (MapView) fatherActivity.findViewById(R.id.my_map);
        mapView.onCreate(savedInstanceState);
        infoView = (TextView) fatherActivity.findViewById(R.id.point_info);
        drawCoverBtn = (FloatingActionButton) fatherActivity.findViewById(R.id.draw_cover_btn);
        drawCoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawMinCoverCircle();
            }
        });
        showAllBtn = (Button) fatherActivity.findViewById(R.id.show_all_btn);
        showActBtn = (Button) fatherActivity.findViewById(R.id.show_act_btn);
        showPrevBtn = (Button) fatherActivity.findViewById(R.id.show_prev_btn);
        showNextBtn = (Button) fatherActivity.findViewById(R.id.show_next_btn);
        showTraceBtn = (Button) fatherActivity.findViewById(R.id.show_trace_btn);
        showAllBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawContent == ALL_RECORDS)
                    return;
                drawContent = ALL_RECORDS;
                refreshMarkers();
            }
        });
        showActBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawContent == CURRENT_ACTIVITY)
                    return;
                drawContent = CURRENT_ACTIVITY;
                refreshMarkers();
            }
        });

        showPrevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawContent == ALL_RECORDS)
                    return;
                fatherActivity.getPrevActivity();
                refreshMarkers();
            }
        });
        showNextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawContent == ALL_RECORDS)
                    return;
                fatherActivity.getNextActivity();
                refreshMarkers();
            }
        });
        showTraceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fatherActivity.getCurActivity() == null)
                    return;
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                String target = "";
                for (Record record : fatherActivity.getCurActivity().getTrace()) {
                    target += record.toDisplayString() + "\n";
                }
                builder.setMessage(target);
                builder.setTitle("轨迹列表");
                builder.setPositiveButton("我知道了", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }
        });

        aMap = mapView.getMap();
        aMap.setOnMarkerClickListener(new AMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                centerLatLng = chosenLatLng = marker.getPosition();
                infoView.setText(marker.getSnippet());
                double radius = Double.valueOf(marker.getSnippet().split("\n")[0].split(" ")[4]);
                drawAccuracyCircle(centerLatLng, radius);
                aMap.animateCamera(CameraUpdateFactory.changeLatLng(centerLatLng));
                return true;
            }
        });
        infoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (chosenLatLng == null)
                    return true;
                MainActivity.showRegeocodeInfoDialog(chosenLatLng.longitude, chosenLatLng.latitude, getContext());
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (needRefresh) {
            if (centerLatLng != null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLatLng, zoom));
            }
            refreshMarkers();
        }
        needRefresh = false;
        isForeground = true;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            this.onResume();
        }  else {
            this.onPause();
        }
    }

    @Override
    public void onPause() {
        zoom = aMap.getCameraPosition().zoom;
        centerLatLng = aMap.getCameraPosition().target;
        chosenLatLng = null;
        isForeground = false;
        super.onPause();
    }

    public void refreshMarkers() {
        for (Marker marker : markers) {
            marker.remove();
        }
        if (polyline != null)
            polyline.remove();
        if (accuracyCircle != null)
            accuracyCircle.remove();
        if (minCoverCircle != null) {
            minCoverCircle.remove();
            minCoverCircle = null;
        }

        markers = new ArrayList<>();
        PolylineOptions options = new PolylineOptions();
        options.color(Color.parseColor("#880066cc"));

        ArrayList<Record> recordSet = getCurRecords();
        if (drawContent == CURRENT_ACTIVITY && fatherActivity.getCurActivity() != null) {
            infoView.setText(fatherActivity.getCurActivity().toDisplayString());
        }
        if (recordSet == null)
            return;

        boolean highlightRecordInSet = false;
        for (Record record : recordSet) {
            if (record == highlightRecord) {
                makeRecordMarker(record, true);
                highlightRecordInSet = true;
            } else {
                makeRecordMarker(record, false);
            }
            options.add(new LatLng(record.getLatitude(), record.getLongitude()));
        }
        if (!highlightRecordInSet && highlightRecord != null) {
            makeRecordMarker(highlightRecord, true);
        }

        polyline = aMap.addPolyline(options);
    }

    private void makeRecordMarker(Record record, boolean highlighted) {
        MarkerOptions options = new MarkerOptions();
        options.position(new LatLng(record.getLatitude(), record.getLongitude()));
        if (highlighted) {
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.highlight_marker));
        } else {
            if (record.getAccuracy() >= 25)
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.bad_marker));
            else if (record.getAccuracy() >= 10)
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.good_marker));
            else
                options.icon(BitmapDescriptorFactory.fromResource(R.drawable.nice_marker));
        }
        Marker marker = aMap.addMarker(options);
        marker.setSnippet(record.toDisplayString());
        markers.add(marker);
    }


    public void setHighlightRecord(Record targetRecord) {
        highlightRecord = targetRecord;
        centerLatLng = new LatLng(targetRecord.getLatitude(), targetRecord.getLongitude());
        needRefresh = true;
    }


    private void drawAccuracyCircle(LatLng center, double radius) {
        if (accuracyCircle != null)
            accuracyCircle.remove();
        CircleOptions coptions = new CircleOptions();
        coptions.center(center);
        coptions.radius(radius);
        coptions.fillColor(Color.parseColor("#88ffff00"));
        coptions.strokeColor(Color.parseColor("#ddffff00"));
        accuracyCircle = aMap.addCircle(coptions);
    }

    private void drawMinCoverCircle() {
        if (minCoverCircle != null) {
            minCoverCircle.remove();
            minCoverCircle = null;
            return;
        }
        ArrayList<LatLng> latLngs = new ArrayList<>();

        ArrayList<Record> recordSet = getCurRecords();
        if (recordSet == null)
            return;

        for (Record record : recordSet) {
            latLngs.add(new LatLng(record.getLatitude(), record.getLongitude()));
        }
        GeometryUtil.MyCircle circle = GeometryUtil.getMinCoverCircle(latLngs);
        if (circle == null)
            return;
        CircleOptions coptions = new CircleOptions();
        coptions.center(circle.center);
        coptions.radius(circle.radius);
        coptions.fillColor(Color.parseColor("#4400ff00"));
        coptions.strokeColor(Color.parseColor("#dd00ff00"));
        minCoverCircle = aMap.addCircle(coptions);
    }

    @Override
    public void onNewValidRecordsGot() {
        if (drawContent != ALL_RECORDS)
            return;
        if (isForeground) {
            Record record = fatherActivity.curRecord;
            makeRecordMarker(record, false);
            List<LatLng> latLngs = polyline.getPoints();
            latLngs.add(new LatLng(record.getLatitude(), record.getLongitude()));
            polyline.setPoints(latLngs);
        } else {
            needRefresh = true;
        }
    }

    @Override
    public void onTimeDurationChanged() {
        if (isForeground) {
            if (centerLatLng != null) {
                aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLatLng, zoom));
            }
            refreshMarkers();
        } else {
            needRefresh = true;
        }
    }

    @Override
    public void onFetchingStatusChanged() {

    }

    private ArrayList<Record> getCurRecords() {
        ArrayList<Record> recordSet = null;
        switch (drawContent) {
            case ALL_RECORDS:
                recordSet = fatherActivity.recordList;
                break;
            case CURRENT_ACTIVITY:
                if (fatherActivity.getCurActivity() == null)
                    return null;
                recordSet = fatherActivity.getCurActivity().getTrace();
                break;
            default:
                break;
        }
        return recordSet;
    }

    public void setDrawContent(int content) {
        drawContent = content;
        if (isForeground)
            refreshMarkers();
        else
            needRefresh = true;
    }

}
