import models.MyActivity;
import models.Record;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by apple on 16/3/6.
 */
public class RoughMiner {

    public static final int ONE_MINUTE = 60 * 1000;
    public static final int ONE_HOUR = 60 * ONE_MINUTE;
    private static final int MIN_ACTIVITY_DURATION = 10 * ONE_MINUTE;

    public static void start() {
        ArrayList<Record> records = new ArrayList<>();
        ArrayList<MyActivity> activities;
        try {
            Connection con;
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection("jdbc:mysql://115.28.20.139:3306/liberion_db", "liber", "20120916");

            Statement statement;
            statement = con.createStatement();

            ResultSet rs = statement.executeQuery("SELECT * FROM RECORDS ORDER BY timepoint");
            while (rs.next()) {
                String ttime = rs.getString("timepoint");
                String rawtext = ttime.split(" ")[0] + "," + ttime.split(" ")[1] + " " + rs.getDouble("longitude") + " " +
                        rs.getDouble("latitude") + " " + rs.getDouble("accuracy") + "::" +
                        rs.getString("country") + "=>" + rs.getString("province") + "=>" + rs.getString("city") + "=>" +
                        rs.getString("district") + "=>" + rs.getString("street") + "=>" + rs.getString("street_num") + "=>" +
                        rs.getString("poi_name") + "=>" + rs.getString("poi_code") + "=>" + rs.getString("provider") + "\n";
                records.add(Record.parseString(rawtext));
            }
            activities = MyActivity.splitActivities(records, MIN_ACTIVITY_DURATION);
            for (MyActivity activity : activities) {
                System.out.println(activity.toDisplayString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
