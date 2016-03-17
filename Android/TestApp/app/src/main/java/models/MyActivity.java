package models;

import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import tools.GeometryUtil;

/**
 * Created by apple on 16/2/25.
 */
public class MyActivity {
    public static final int STAY = 0;
    public static final int TRAVEL = 1;

    public static final double PERCENTAGE = 0.9;
    public static final int MAX_TOLERANCE_ACTIVITY_INTERVAL = 10 * 60 * 1000;
    public static final double MIN_TRACE_POINT_RATE_IN_ACTIVITY = 1 / 8000;

    public static final double VECTOR_PRODUCT_TRAVEL_THRESHOLD = 0.4;
    public static final double ASSURANCE_OUTDOOR_THRESHOLD = 0.8;

    ArrayList<Record> trace = new ArrayList<>();
    GeometryUtil.MyCircle area;
    double averageCenterDistance;
    double averageVectorProduct;
    int activityType = 0;

    private class MyVector {
        double x;
        double y;

        public MyVector(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private double calcStandardizedVector(MyVector a, MyVector b) {
        double ac = Math.sqrt(a.x * a.x + a.y * a.y);
        if (ac == 0)
            return 0;
        double ax = a.x / ac;
        double ay = a.y / ac;
        double bc = Math.sqrt(b.x * b.x + b.y * b.y);
        if (bc == 0)
            return 0;
        double bx = b.x / bc;
        double by = b.y / bc;
        double product = ax * bx + ay * by;
        return product;
    }

    public MyActivity(ArrayList<Record> records) {
//        trace = cleanRecords(records);
        trace = records;
        averageVectorProduct = 0;
        Record lastRecord = null;
        MyVector lastVetor = null;

        int gpsSum = 0;

        ArrayList<LatLng> points = new ArrayList<>();
        for (Record record : trace) {
            if (record.getLocationInfo().provider.equals("GPS"))
                gpsSum ++;
            points.add(new LatLng(record.getLatitude(), record.getLongitude()));
            if (lastRecord != null) {
                MyVector thisVector = new MyVector(record.getLongitude() - lastRecord.getLongitude(),
                        record.getLatitude() - lastRecord.getLatitude());
                if (lastVetor != null) {
                    averageVectorProduct += calcStandardizedVector(thisVector, lastVetor);
                }
                lastVetor = thisVector;
            }
            lastRecord = record;
        }

        area = GeometryUtil.getMinCoverCircle(points);
        double totalDistance = 0;
        for (LatLng latLng : points) {
            totalDistance += AMapUtils.calculateLineDistance(latLng, area.center);
        }
        averageCenterDistance = totalDistance / points.size();
        averageVectorProduct /= points.size();

        if (((double)gpsSum) / ((double)trace.size()) > ASSURANCE_OUTDOOR_THRESHOLD) {
            if (averageVectorProduct < VECTOR_PRODUCT_TRAVEL_THRESHOLD) {
                activityType = STAY;
            } else {
                activityType = TRAVEL;
            }
        } else {
            activityType = STAY;
        }


    }

    public int getActivityType() {
        return activityType;
    }

    public GeometryUtil.MyCircle getArea() {
        return area;
    }

    public double getAverageCenterDistance() {
        return averageCenterDistance;
    }

    public double getAverageVectorProduct() {
        return averageVectorProduct;
    }

    public ArrayList<Record> getTrace() {
        return trace;
    }

    public Date getStartDate() {
        return trace.get(0).getTimepoint();
    }

    public Date getEndDate() {
        return trace.get(trace.size() - 1).getTimepoint();
    }

    private ArrayList<Record> cleanRecords(ArrayList<Record> records) {
        ArrayList<Record> ret = new ArrayList<>();
        ArrayList<Record> tmpList = new ArrayList<>();
        double totalAcc = 0;
        for (Record record : records) {
            totalAcc += 1.0 / record.getAccuracy();
            tmpList.add(record);
        }
        Collections.sort(tmpList, new Comparator<Record>() {
            @Override
            public int compare(Record lhs, Record rhs) {
                if (lhs.getAccuracy() - rhs.getAccuracy() < 0)
                    return -1;
                else if (lhs.getAccuracy() == rhs.getAccuracy())
                    return 0;
                return 1;
            }
        });
        totalAcc *= PERCENTAGE;
        for (Record record : tmpList) {
            totalAcc -= 1 / record.getAccuracy();
            if (totalAcc < 0)
                break;
            ret.add(record);
        }
        Collections.sort(ret, new Comparator<Record>() {
            @Override
            public int compare(Record lhs, Record rhs) {
                if (lhs.getTimepoint().getTime() - rhs.getTimepoint().getTime() < 0)
                    return -1;
                else if (lhs.getAccuracy() == rhs.getAccuracy())
                    return 0;
                return 1;
            }
        });
        return ret;
    }

    public String toDisplayString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        return formatter.format(getStartDate()) + " -- " + formatter.format(getEndDate()) + "\n" +
                trace.size() + "条记录\n圆心: " + area.center.longitude + ", " + area.center.latitude +
                ", 半径: " + area.radius + ", " + "\n平均圆心距: " + averageCenterDistance +
                "\n平均向量内积: " + averageVectorProduct;
    }

    /*
     * return true if two merged into one
     * the postActivity will be set null if the two are totally merges to one activity
     * won't merge if the interval between two activities is too long since some unknwon activity
     * could happen in the mean time
     */
    public static boolean mergeActivity(MyActivity prevActivity, MyActivity postActivity) {
        if (postActivity.getStartDate().getTime() - prevActivity.getEndDate().getTime() > MAX_TOLERANCE_ACTIVITY_INTERVAL)
            return false;
        if (postActivity.activityType == STAY && prevActivity.activityType == TRAVEL ||
                postActivity.activityType == TRAVEL && prevActivity.activityType == STAY )
            return false;
        if (postActivity.activityType == STAY && prevActivity.activityType == STAY) {
            prevActivity.getTrace().addAll(postActivity.getTrace());
            prevActivity = new MyActivity(prevActivity.getTrace());
            return true;
        }
        if (postActivity.activityType == TRAVEL && prevActivity.activityType == TRAVEL) {
            prevActivity.getTrace().addAll(postActivity.getTrace());
            prevActivity = new MyActivity(prevActivity.getTrace());
            return true;
        }

        return false;
    }


    //duration should be ms
    public static ArrayList<MyActivity> splitActivities(ArrayList<Record> records, int duration) {
        Date startDate = records.get(0).getTimepoint();
        Date endDate = new Date(startDate.getTime() + duration);
        ArrayList<MyActivity> activities = new ArrayList<>();

        ArrayList<Record> list = new ArrayList<>();
        for (Record record: records) {
            if (record.getTimepoint().getTime() <= endDate.getTime()) {
                list.add(record);
            } else {
                if (list.size() >= MIN_TRACE_POINT_RATE_IN_ACTIVITY * duration) {
                    activities.add(new MyActivity(list));
                }
                startDate = record.getTimepoint();
                endDate = new Date(startDate.getTime() + duration);
                list = new ArrayList<>();
            }
        }
        if (list.size() >= MIN_TRACE_POINT_RATE_IN_ACTIVITY * duration) {
            activities.add(new MyActivity(list));
        }
        ArrayList<MyActivity> mergedActivities = new ArrayList<>();
        MyActivity lastActivity = null;
        for (MyActivity activity : activities) {
            if (lastActivity == null) {
                lastActivity = activity;
                continue;
            }
            boolean merged = mergeActivity(lastActivity, activity);
            if (!merged) {
                mergedActivities.add(lastActivity);
                lastActivity = activity;
            }
        }
        if (lastActivity != null)
            mergedActivities.add(lastActivity);
        return mergedActivities;
    }
}
