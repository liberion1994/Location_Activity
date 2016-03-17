import models.LatLng;
import models.MyActivity;
import models.Period;
import models.Record;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by apple on 16/3/3.
 */
public class ActivityMiner {

    public static final int ONE_MINUTE = 60 * 1000;
    public static final int ONE_HOUR = 60 * ONE_MINUTE;
    public static final int ONE_DAY = 24 * ONE_HOUR;

    public static final int PERIOD_DURATION = 2 * ONE_MINUTE;

    public static final int MIN_RECORDS_PER_PERIOD = 12;

    private static final String AmapSearchUrl = "http://restapi.amap.com/v3/place/around";
    private static final String ApiKey = "73f88d58ee45d48af7fdec77b7d3a05c";

    public static void main(String[] args) {
        splitPeriods();
//        RoughMiner.start();
//        getLocationInfoThroughHttp(null);
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
//        try {
//            Date startDate = formatter.parse(args[0]);
//            Date endDate = new Date((System.currentTimeMillis() / ONE_DAY) * ONE_DAY - 4 * ONE_HOUR);
//            while (true) {
//                startMining(startDate, endDate);
//                startDate = endDate;
//                endDate = new Date(startDate.getTime() + ONE_DAY);
//                Thread.sleep(ONE_DAY);
//            }
//        } catch (Exception e) {
//            writeLog("ERROR: " + e.getMessage() + "\n");
//        }
    }

    public static void startRoughMining() {

    }

    public static void splitPeriods() {
        try {
            Connection con;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
//            con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/liberion_db", "root", "20120916");
            con = DriverManager.getConnection("jdbc:mysql://115.28.20.139:3306/liberion_db", "liber", "20120916");
            Statement stmt;
            stmt = con.createStatement();

            // get last processed record
            ResultSet rs = stmt.executeQuery("SELECT end_time FROM FACTS ORDER BY end_time DESC LIMIT 0,1");
            String lastTime = null;
            while (rs.next()) {
                lastTime = rs.getString("end_time");
            }
            if (lastTime == null) {
                lastTime = "2016-1-1 00:00:00";
            }

            //get first record in the period
            rs = stmt.executeQuery("SELECT timepoint FROM RECORDS WHERE timepoint > \"" + lastTime +
                    "\" ORDER BY timepoint LIMIT 0,1");
            String startTime = null;
            while (rs.next()) {
                startTime = rs.getString("timepoint");
            }

            while (startTime != null) {
                //get a period
                ArrayList<Record> records = new ArrayList<>();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String endTime = formatter.format(new Date(formatter.parse(startTime).getTime() + PERIOD_DURATION));
                rs = stmt.executeQuery("SELECT * FROM RECORDS WHERE timepoint >= \"" + startTime +
                        "\" AND timepoint <= \"" + endTime + "\" ORDER BY timepoint");
                while (rs.next()) {
                    String ttime = rs.getString("timepoint");
                    String rawtext = ttime.split(" ")[0] + "," + ttime.split(" ")[1] + " " + rs.getDouble("longitude") + " " +
                            rs.getDouble("latitude") + " " + rs.getDouble("accuracy") + "::" +
                            rs.getString("country") + "=>" + rs.getString("province") + "=>" + rs.getString("city") + "=>" +
                            rs.getString("district") + "=>" + rs.getString("street") + "=>" + rs.getString("street_num") + "=>" +
                            rs.getString("poi_name") + "=>" + rs.getString("poi_code") + "=>" + rs.getString("provider");
                    records.add(Record.parseString(rawtext));
                }
                if (records.size() >= MIN_RECORDS_PER_PERIOD) {
                    Period period = new Period(records.get(0).getTimepoint(),
                            records.get(records.size() - 1).getTimepoint(), records);

                    String query = "INSERT INTO FACTS (type, start_time, end_time, average_speed, average_velocity," +
                            "average_swing_rate, center_latitude, center_longitude, coverage_radius, " +
                            "displacement_distance_ratio, label) VALUES (" + period.getType() +
                            ",\"" + formatter.format(period.getStartDate()) +
                            "\",\"" + formatter.format(period.getEndDate()) +
                            "\"," + period.getAverageSpeed() +
                            "," + period.getAverageVelocity() +
                            "," + period.getAverageSwingRate() +
                            "," + period.getCoverageCircle().center.latitude +
                            "," + period.getCoverageCircle().center.longitude +
                            "," + period.getCoverageCircle().radius +
                            "," + period.getDisplacementDistanceRatio() +
                            ",\"" + period.getLabel() + "\")";
                    System.out.println(formatter.format(period.getStartDate()));
                    stmt.executeUpdate(query);
                }

                //last time
                lastTime = formatter.format(records.get(records.size() - 1).getTimepoint());
                rs = stmt.executeQuery("SELECT timepoint FROM RECORDS WHERE timepoint > \"" + lastTime +
                        "\" ORDER BY timepoint LIMIT 0,1");
                startTime = null;
                while (rs.next()) {
                    startTime = rs.getString("timepoint");
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startMining(Date startDate, Date endDate) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
        String sd = formatter.format(startDate);
        String ed = formatter.format(endDate);

        writeLog("<================================================================>\n" +
                "MINING FROM " + sd + " TO " + ed + " STARTED AT " +
                formatter.format(new Date(System.currentTimeMillis())) + "\n");

        ArrayList<Record> records = new ArrayList<>();
        ArrayList<MyActivity> activities;
        try {
            Connection con;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/liberion_db", "root", "20120916");

            Statement stmt;
            stmt = con.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM RECORDS WHERE timepoint >= \"" + sd + "\" AND timepoint <= \"" + ed + "\"");
            while (rs.next()) {
                String ttime = rs.getString("timepoint");
                String rawtext = ttime.split(" ")[0] + "," + ttime.split(" ")[1] + " " + rs.getDouble("longitude") + " " +
                        rs.getDouble("latitude") + " " + rs.getDouble("accuracy") + "::" +
                        rs.getString("country") + "=>" + rs.getString("province") + "=>" + rs.getString("city") + "=>" +
                        rs.getString("district") + "=>" + rs.getString("street") + "=>" + rs.getString("street_num") + "=>" +
                        rs.getString("poi_name") + "=>" + rs.getString("poi_code") + "=>" + rs.getString("provider") + "\n";
                records.add(Record.parseString(rawtext));
            }

            writeLog("LOADING " + records.size() + " RECORDS FINISHED AT " +
                    formatter.format(new Date(System.currentTimeMillis())) + "\n");
            activities = MyActivity.splitActivities(records, 2 * 60 * 1000);

            writeLog("GETTING " + activities.size() + " ACTIVITIES FINISHED AT " +
                    formatter.format(new Date(System.currentTimeMillis())) + "\n");

            String query = null;
            for (MyActivity activity : activities) {
                if (activity.getActivityType() == MyActivity.TRAVEL) {
                    query = "INSERT INTO ACTIVITIES (start_time, end_time, type, latitude1, longitude1, latitude2, longitude2) VALUES (\"" +
                            formatter.format(activity.getStartDate()) + "\",\"" + formatter.format(activity.getEndDate()) + "\"," +
                            activity.getActivityType() + "," +
                            activity.getTrace().get(0).getLatitude() + "," + activity.getTrace().get(0).getLongitude() + "," +
                            activity.getTrace().get(activity.getTrace().size() - 1).getLatitude() + "," +
                            activity.getTrace().get(activity.getTrace().size() - 1).getLongitude() + ");";
                } else if (activity.getActivityType() == MyActivity.STAY) {
                    query = "INSERT INTO ACTIVITIES (start_time, end_time, type, latitude1, longitude1) VALUES (\"" +
                            formatter.format(activity.getStartDate()) + "\",\"" + formatter.format(activity.getEndDate()) + "\"," +
                            activity.getActivityType() + "," +
                            activity.getArea().center.latitude + "," + activity.getArea().center.longitude + ");";
                }
                writeLog("****GOING TO EXCUTE" + query + "\n");
                stmt.executeUpdate(query);
            }
            con.close();
            writeLog("MINING FROM " + sd + " TO " + ed + " FINISHED AT " +
                    formatter.format(new Date(System.currentTimeMillis())) + "\n");
        } catch (Exception e) {
            writeLog("ERROR: " + e.getMessage() + "\n");
        }
    }

    private static void writeLog(String log) {
        try {
            FileWriter fileWriter = new FileWriter("ACTIVITY_MINER_LOG", true);
            fileWriter.write(log);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getLocationInfoThroughHttp(LatLng point) {
        String param = "key=" + ApiKey + "&location=116.456299,39.960767&radius=100";
        String urlStr = AmapSearchUrl + "?" + param;
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(urlStr);
            HttpResponse httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String strResult = EntityUtils.toString(httpResponse.getEntity());
                JSONObject object = JSONObject.fromObject(strResult);
                String status = object.getString("status");
                JSONArray pois = object.getJSONArray("pois");
                System.out.println(pois.get(0).toString());
            }
        } catch (Exception e) {
            writeLog("ERROR: " + e.getMessage() + "\n");
        }
    }
}
