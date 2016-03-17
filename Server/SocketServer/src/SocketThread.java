import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;


public class SocketThread extends Thread {

    Socket socket;

    public SocketThread(Socket socket) {
            this.socket = socket;
    }

    private void putString(String target) {
            try {
                    socket.getOutputStream().write(target.getBytes("UTF-8"));
            } catch (Exception e) {
                    System.out.println(e.getMessage());
            }
    }

    @Override
    public void run() {
            // TODO Auto-generated method stub
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String content = br.readLine();
            if (content.equals("LOAD")) {
                String stDate = br.readLine();
                String enDate = br.readLine();
                System.out.println("GET REQUEST FROM " + stDate + "TO " + enDate + "\n");

                Connection con = null; //定义一个MYSQL链接对象
            	Class.forName("com.mysql.jdbc.Driver").newInstance(); //MYSQL驱动
            	con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/liberion_db", "root", "20120916");

                Statement stmt; //创建声明
                stmt = con.createStatement();
                String returnBack = "";
            	ResultSet rs = stmt.executeQuery("SELECT * FROM RECORDS WHERE timepoint >= \"" + stDate + "\" AND timepoint <= \"" + enDate + "\"");
                while (rs.next()) {
                    String ttime = rs.getString("timepoint");
                    returnBack += ttime.split(" ")[0] + "," + ttime.split(" ")[1] + " " + rs.getDouble("longitude") + " " +
                            rs.getDouble("latitude") + " " + rs.getDouble("accuracy") + "::" + 
                            rs.getString("country") + "=>" + rs.getString("province") + "=>" + rs.getString("city") + "=>" + 
                            rs.getString("district") + "=>" + rs.getString("street") + "=>" + rs.getString("street_num") + "=>" + 
                            rs.getString("poi_name") + "=>" + rs.getString("poi_code") + "=>" + rs.getString("provider") + "\n";
                 }
                con.close();
                System.out.println(returnBack + "<===================================>\n");
            	putString(returnBack + "END\n");
            } else if (content.equals("SAVE")) {
                ArrayList<String> lines = new ArrayList<>();
                String line =  br.readLine();
                while (!line.equals("END")) {
                    lines.add(line);
                    line = br.readLine();
                }
                Connection con = null; //定义一个MYSQL链接对象
                Class.forName("com.mysql.jdbc.Driver").newInstance(); //MYSQL驱动
                con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/liberion_db", "root", "20120916");

                Statement stmt; //创建声明
                stmt = con.createStatement();

                for (String rawText : lines) {
                    String[] text = rawText.split("::");
                    String[] words = text[0].split(" ");
                    String timepoint = words[0];
                    double longitude = Double.valueOf(words[1]);
                    double latitude = Double.valueOf(words[2]);
                    double accuracy = Double.valueOf(words[3]);
                    String desc;
                    if (text.length == 1)
                        desc = "null";
                    else
                        desc = text[1];
                    String []infos = desc.split("=>", 9);
                    String query = "INSERT INTO RECORDS (timepoint, latitude, longitude, accuracy, country, province, city, district, street, street_num, poi_name, poi_code, provider) values (\"" + 
                        words[0] + "\", " + latitude + ", " + longitude + ", " + accuracy + ", \"" + 
                        infos[0] + "\", \"" + infos[1] + "\", \"" + infos[2] + "\", \"" + infos[3] + "\", \"" + infos[4] + "\", \"" +
                        infos[5] + "\", \"" + infos[6] + "\", \"" + infos[7] + "\", \"" + infos[8] + "\")";
                    stmt.executeUpdate(query);
                    System.out.println("*EXCUTE QUERY: " + query);
                }
                putString("FINISHED\n");
                System.out.println("<===================================>\n");
                con.close();
            } else if (content.equals("NODE")) {
                String type =  br.readLine();
                String timepoint = br.readLine();
                String text = br.readLine();
                Connection con = null; //定义一个MYSQL链接对象
                Class.forName("com.mysql.jdbc.Driver").newInstance(); //MYSQL驱动
                con = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/liberion_db", "root", "20120916");
                String texts[] = text.split("=>", 7);
                Statement stmt; //创建声明
                stmt = con.createStatement();
                String query = "INSERT INTO ACT_NODES (timepoint, type, latitude, longitude, poi_id, name, poi_type, typecode, address) VALUES (\"" +
                        timepoint + "\", " + type + "," + Double.valueOf(texts[5]) + "," + Double.valueOf(texts[6]) + ",\"" +
                        texts[0] + "\", \"" + texts[1] + "\", \"" + texts[2] + "\", \"" + texts[3] + "\", \"" +
                        texts[4] + "\")";
                stmt.executeUpdate(query);
                System.out.println("*EXCUTE QUERY: " + query);
                putString("FINISHED\n");
                System.out.println("<===================================>\n");
                con.close();
            } else if (content.equals("FACTS")) {
                String[] params = new String[11];

            }

            br.close();
            socket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
