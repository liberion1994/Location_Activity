package models;

import tools.GeometryUtil;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by apple on 16/3/15.
 */
public class Period {

    public static final int TYPE_INDOOR = 1;
    public static final int TYPE_OUTDOOR = 2;
    public static final double ASSURANCE_OUTDOOR_THRESHOLD = 0.5;

    private int type;
    private Date startDate;
    private Date endDate;
    private double averageSpeed;
    private double averageVelocity;
    private double averageSwingRate;
    private GeometryUtil.MyCircle coverageCircle ;
    private double displacementDistanceRatio;

    private String label;

    private class MyVector {
        double x;
        double y;

        public MyVector(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public Period(Date startDate, Date endDate, ArrayList<Record> targetRecords) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.label = null;
        calcParams(targetRecords);
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

    private void calcParams(ArrayList<Record> records) {

        int gpsSum = 0;
        ArrayList<LatLng> points = new ArrayList<>();
        Record lastRecord = null;
        MyVector lastVector = null;
        averageSwingRate = 0;
        double totalDistance = 0;

        for (Record record : records) {
            if (record.getLocationInfo().provider.equals("GPS")) {
                gpsSum ++;
            }

        }

        double target = (double)records.size() * ASSURANCE_OUTDOOR_THRESHOLD;
        if ((double)gpsSum >= target) {
            type = TYPE_OUTDOOR;
        } else {
            type = TYPE_INDOOR;
            for (Record record : records) {
                if (record.getLocationInfo().provider.equals("GPS")) {
                    continue;
                }
                points.add(new LatLng(record.getLatitude(), record.getLongitude()));
            }
            coverageCircle = GeometryUtil.getMinCoverCircle(points);
            return;
        }

        for (Record record : records) {
            if (!record.getLocationInfo().provider.equals("GPS")) {
                continue;
            }
            points.add(new LatLng(record.getLatitude(), record.getLongitude()));
            if (lastRecord != null) {
                totalDistance += GeometryUtil.calculateLineDistance(new LatLng(record.getLatitude(), record.getLongitude()),
                        new LatLng(lastRecord.getLatitude(), lastRecord.getLongitude()));
                MyVector thisVector = new MyVector(record.getLongitude() - lastRecord.getLongitude(),
                        record.getLatitude() - lastRecord.getLatitude());
                if (lastVector != null) {
                    averageSwingRate += calcStandardizedVector(thisVector, lastVector);
                }
                lastVector = thisVector;
            }
            lastRecord = record;
        }


        averageSwingRate /= (points.size() - 2);
        coverageCircle = GeometryUtil.getMinCoverCircle(points);
        averageVelocity = totalDistance / (endDate.getTime() - startDate.getTime()) * 1000;

        double displacement = GeometryUtil.calculateLineDistance(points.get(0), points.get(points.size() - 1));
        averageSpeed = displacement / (endDate.getTime() - startDate.getTime()) * 1000;
        if (totalDistance != 0) {
            displacementDistanceRatio = displacement / totalDistance;
        } else {
            displacementDistanceRatio = 0;
        }

    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getType() {
        return type;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public double getAverageVelocity() {
        return averageVelocity;
    }

    public double getAverageSwingRate() {
        return averageSwingRate;
    }

    public GeometryUtil.MyCircle getCoverageCircle() {
        return coverageCircle;
    }

    public double getDisplacementDistanceRatio() {
        return displacementDistanceRatio;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Period{" +
                "type=" + type +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", averageSpeed=" + averageSpeed +
                ", averageVelocity=" + averageVelocity +
                ", averageSwingRate=" + averageSwingRate +
                ", coverageCircle=" + coverageCircle +
                ", displacementDistanceRatio=" + displacementDistanceRatio +
                ", label='" + label + '\'' +
                '}';
    }
}
