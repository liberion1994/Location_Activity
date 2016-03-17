package models;

import com.amap.api.location.AMapLocation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by apple on 16/2/11.
 */
public class Event {
    private String title;
    private Date timepoint;
    private double longitude;
    private double latitude;

    public Event(String title, Date timepoint, double longitude, double latitude) {
        this.title = title;
        this.timepoint = timepoint;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(Date timepoint) {
        this.timepoint = timepoint;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
        return formatter.format(timepoint) + " " + longitude + "," + latitude + " " + title.trim();
    }

    public static Event parseString(String rawText) {
        String[] words = rawText.split(" ");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
        try {
            Date timepoint = formatter.parse(words[0]);
            double longitude = Double.valueOf(words[1].split(",")[0]);
            double latitude = Double.valueOf(words[1].split(",")[1]);
            String title = words[2];
            Event res = new Event(title, timepoint, longitude, latitude);
            return res;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
