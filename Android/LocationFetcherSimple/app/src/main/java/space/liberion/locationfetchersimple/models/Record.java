package space.liberion.locationfetchersimple.models;

import android.location.Location;

import com.amap.api.location.AMapLocation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by apple on 16/2/11.
 */
public class Record {

    public class LocationInfo {
        public String country;
        public String province;
        public String city;
        public String district;
        public String street;
        public String streetNum;
        public String poiName;
        public String poiCode;
        public String provider;

        public LocationInfo(String rawText) {
            String []infos = rawText.split("=>", 9);
            country = infos[0];
            province = infos[1];
            city = infos[2];
            district = infos[3];
            street = infos[4];
            streetNum = infos[5];
            poiName = infos[6];
            poiCode = infos[7];
            provider = infos[8];
        }

        public String toDisplayText() {
            String ret = country + province + city + district + street + streetNum + poiName;
            if (ret.equals(""))
                return "无法从" + provider + "得到相关信息，长按以进行逆地理编码";
            return ret + " 来源:" + provider;
        }

        public String toRawText() {
            String ret = country + "=>" + province + "=>" + city + "=>" + district + "=>" + street +
                    "=>" + streetNum + "=>" + poiName + "=>" + poiCode + "=>" + provider;
            return ret;
        }
    }

    private Date timepoint;
    private double longitude;
    private double latitude;
    private double accuracy;
    private LocationInfo locationInfo;

    public Record(Date timepoint, double longitude, double latitude, double accuracy, String description) {
        this.timepoint = timepoint;
        this.longitude = longitude;
        this.latitude = latitude;
        this.accuracy = accuracy;
        this.locationInfo = new LocationInfo(description);
    }

    public Date getTimepoint() {
        return timepoint;
    }


    public double getLongitude() {
        return longitude;
    }


    public double getLatitude() {
        return latitude;
    }


    public double getAccuracy() {
        return accuracy;
    }

    public LocationInfo getLocationInfo() {
        return locationInfo;
    }

    public String getDescription() {
        return locationInfo.toDisplayText();
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
        return formatter.format(timepoint) + " " + longitude + " " + latitude + " " + accuracy + "::" + locationInfo.toRawText();
    }

    public String toDisplayString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        return formatter.format(timepoint) + " " + longitude + " " + latitude + " " + accuracy + "\n" +
                locationInfo.toDisplayText();
    }

    public static Record parseString(String rawText) {
        String[] text = rawText.split("::");
        String[] words = text[0].split(" ");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
        try {
            Date timepoint = formatter.parse(words[0]);
            double longitude = Double.valueOf(words[1]);
            double latitude = Double.valueOf(words[2]);
            double accuracy = Double.valueOf(words[3]);
            String desc;
            if (text.length == 1)
                desc = "null";
            else
                desc = text[1];
            Record res = new Record(timepoint, longitude, latitude, accuracy, desc);
            return res;
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
