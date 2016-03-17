package models;

import net.sf.ezmorph.array.DoubleArrayMorpher;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Created by apple on 16/3/4.
 */
public class PoiInfo {
    String id;
    String name;
    String type;
    String typecode;
    String adress;
    LatLng location;

    public PoiInfo(String id, String name, String type, String typecode, String adress, LatLng location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.typecode = typecode;
        this.adress = adress;
        this.location = location;
    }

    public PoiInfo(String jsonString) {
        JSONObject object = JSONObject.fromObject(jsonString);
        id = object.getString("id");
        name = object.getString("name");
        type = object.getString("type");
        typecode = object.getString("typecode");
        adress = object.getString("adress");
        location = new LatLng(Double.valueOf(object.getString("location").split(",")[1]),
                Double.valueOf((object.getString("location").split(",")[0])));
    }
}
