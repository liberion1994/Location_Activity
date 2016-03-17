package space.liberion.locationfetchersimple.models;

import org.json.JSONException;

/**
 * Created by apple on 16/3/4.
 */
public class PoiInfo {

    public class LatLng {
        public double latitude;
        public double longitude;

        public LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    String id;
    String name;
    String type;
    String typecode;
    String address;
    LatLng location;

    public PoiInfo(String id, String name, String type, String typecode, String adress, LatLng location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.typecode = typecode;
        this.address = adress;
        this.location = location;
    }

    public PoiInfo(String jsonString) {
        try {
            org.json.JSONObject object = new org.json.JSONObject(jsonString);
            id = object.getString("id");
            name = object.getString("name");
            type = object.getString("type");
            typecode = object.getString("typecode");
            address = object.getString("address");
            location = new LatLng(Double.valueOf(object.getString("location").split(",")[1]),
                    Double.valueOf((object.getString("location").split(",")[0])));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String toDisplayText() {
        return name + "\n" + address + "\n" + type;
    }

    public String toTransText() {
        return id + "=>" + name + "=>" + type + "=>" + typecode + "=>" + address +
                "=>" + location.latitude + "=>" + location.longitude;
    }
}
