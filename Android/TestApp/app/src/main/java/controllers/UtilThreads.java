package controllers;

import android.os.Handler;
import android.os.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import models.Record;




/**
 * Created by apple on 16/2/21.
 */
public class UtilThreads {


    public static final int TIME_INTERVAL_REACHED = 0;
    public static final int LOAD_FINISHED = 1;
    public static final int LOAD_FAILED = 2;
    public static final int SAVE_FINISHED = 3;
    public static final int SAVE_FAILED = 4;
    public static final int RETRY_TIMES = 5;
    public static final int SAVE_NODE_FINISHED = 6;
    public static final int SAVE_NODE_FAILED = 7;
    public static final int SEARCH_FINISHED = 8;
    public static final int SEARCH_FAILED = 9;

    static final int RETRY_INTERVAL = 1000;

    static final int AUTO_SAVE_INTERVAL = 5 * 60 * 1000;


    public static class AutoSaveThread implements Runnable {

        private Handler handler;
        private boolean doLoop = true;

        public AutoSaveThread(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            while (doLoop){
                try {
                    Thread.sleep(AUTO_SAVE_INTERVAL);
                    if (!doLoop)
                        break;
                    Message msg = handler.obtainMessage();
                    msg.what = TIME_INTERVAL_REACHED;
                    handler.sendMessage(msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        public void endLoop() {
            doLoop = false;
        }
    };

    public static class LoadThread extends Thread {

        private Date startDate;
        private Date endDate;
        private Handler handler;

        private boolean connectFailed = false;
        private int triedTimes = 0;

        public LoadThread(Handler handler, Date startDate, Date endDate) {
            this.handler = handler;
            this.endDate = endDate;
            this.startDate = startDate;
        }

        @Override
        public void run() {
            try {
                makeLoad();
            } catch (IOException e) {
                connectFailed = true;
                while (++ triedTimes < RETRY_TIMES && connectFailed) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                        makeLoad();
                        connectFailed = false;
                    } catch (IOException e1) {
                        connectFailed = true;
                        e1.printStackTrace();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if (connectFailed) {
                    Message message = handler.obtainMessage();
                    message.what = LOAD_FAILED;
                    handler.sendMessage(message);
                }
            }
        }

        private void makeLoad() throws IOException {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
            String target = "LOAD\n" + simpleDateFormat.format(startDate) + "\n" + simpleDateFormat.format(endDate) + "\n";

            Socket socket = new Socket("115.28.20.139", 12345);
            socket.getOutputStream().write(target.getBytes("UTF-8"));

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ArrayList<String> lines = new ArrayList<>();
            String line = br.readLine();
            while (!line.equals("END")) {
                lines.add(line);
                line = br.readLine();
            }

            Message message = handler.obtainMessage();
            message.obj = lines;
            message.what = LOAD_FINISHED;
            handler.sendMessage(message);

            br.close();
            socket.close();
        }
    }

    public static class SaveNodeThread extends Thread {

        int type;
        Date timepoint;
        String text;

        private Handler handler;
        private boolean connectFailed = false;
        private int triedTimes = 0;

        public SaveNodeThread(Date timepoint, int type, String text, Handler handler) {
            this.timepoint = timepoint;
            this.handler = handler;
            this.type = type;
            this.text = text;
        }

        @Override
        public void run() {
            try {
                makeSave();
            } catch (IOException e) {
                connectFailed = true;
                while (++ triedTimes < RETRY_TIMES && connectFailed) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                        makeSave();
                        connectFailed = false;
                    } catch (IOException e1) {
                        connectFailed = true;
                        e1.printStackTrace();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if (connectFailed) {
                    Message message = handler.obtainMessage();
                    message.what = SAVE_NODE_FAILED;
                    handler.sendMessage(message);
                }
            }
        }

        private void makeSave() throws IOException{
            String target = "NODE\n" + type + "\n";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss");
            target += simpleDateFormat.format(timepoint) + "\n" + text + "\n";

            Socket socket = new Socket("115.28.20.139", 12345);
            socket.getOutputStream().write(target.getBytes("UTF-8"));

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = br.readLine();
            Message message = handler.obtainMessage();
            if (line == null)
                message.what = SAVE_NODE_FAILED;
            else if (line.equals("FINISHED"))
                message.what = SAVE_NODE_FINISHED;
            else
                message.what = SAVE_NODE_FAILED;
            handler.sendMessage(message);

            br.close();
            socket.close();
        }
    }

    public static class SaveThread extends Thread {

        private Handler handler;
        private ArrayList<Record> newRecords;

        private boolean connectFailed = false;
        private int triedTimes = 0;

        public SaveThread(Handler handler, ArrayList<Record> newRecords) {
            this.handler = handler;
            this.newRecords = newRecords;
        }

        @Override
        public void run() {
            try {
                makeSave();
            } catch (IOException e) {
                connectFailed = true;
                while (++ triedTimes < RETRY_TIMES && connectFailed) {
                    try {
                        Thread.sleep(RETRY_INTERVAL);
                        makeSave();
                        connectFailed = false;
                    } catch (IOException e1) {
                        connectFailed = true;
                        e1.printStackTrace();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                if (connectFailed) {
                    Message message = handler.obtainMessage();
                    message.what = SAVE_FAILED;
                    handler.sendMessage(message);
                }
            }
        }

        private void makeSave() throws IOException{
            String target = "SAVE\n";
            for (Record record : newRecords) {
                target += record.toString() + "\n";
            }
            target += "END\n";

            Socket socket = new Socket("115.28.20.139", 12345);
            socket.getOutputStream().write(target.getBytes("UTF-8"));

            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = br.readLine();
            Message message = handler.obtainMessage();
            if (line == null)
                message.what = SAVE_NODE_FAILED;
            else if (line.equals("FINISHED"))
                message.what = SAVE_FINISHED;
            else
                message.what = SAVE_FAILED;
            handler.sendMessage(message);

            br.close();
            socket.close();
        }
    }

    public static class GetSearchResultThread extends Thread {

        String urlStr;
        private Handler handler;

        public GetSearchResultThread(Handler handler, String urlStr) {
            this.handler = handler;
            this.urlStr = urlStr;
        }

        @Override
        public void run() {
            try {
                makeSearch();
            } catch (IOException e) {
                Message message = handler.obtainMessage();
                message.what = SEARCH_FAILED;
                handler.sendMessage(message);
            }
        }

        private void makeSearch() throws IOException {
            URL url = new URL(urlStr);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            InputStream is = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String response = "";
            String readLine;
            while((readLine = br.readLine()) != null){
                response = response + readLine;
            }
            is.close();
            br.close();
            urlConnection.disconnect();
            Message message = handler.obtainMessage();
            message.obj = response;
            message.what = SEARCH_FINISHED;
            handler.sendMessage(message);
        }
    }
}
