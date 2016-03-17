package tools;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Event;
import models.Record;

/**
 * Created by apple on 16/2/11.
 */
public class RecordsHandler {

    public static ArrayList<Record> loadRecords(Activity activity) {
        ArrayList<Record> records = new ArrayList<>();
        try {
            FileInputStream fileInputStream = activity.openFileInput("Records.dat");
            byte[] s = new byte[fileInputStream.available() + 100];
            fileInputStream.read(s);
            String content = new String(s);
            String[] lines = content.split("\n");
            for (String line : lines) {
                Record record = Record.parseString(line);
                if (record != null) {
                    records.add(record);
                }
            }
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static ArrayList<Event> loadEvents(Activity activity) {
        ArrayList<Event> events = new ArrayList<>();
        try {
            FileInputStream fileInputStream = activity.openFileInput("Events.dat");
            byte[] s = new byte[fileInputStream.available() + 100];
            fileInputStream.read(s);
            String content = new String(s);
            String[] lines = content.split("\n");
            for (String line : lines) {
                Event event = Event.parseString(line);
                if (event != null)
                    events.add(event);
            }
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return events;
    }

    public static boolean saveRecords(ArrayList<Record> records, Activity activity) {
        boolean res = true;
        try {
            FileOutputStream outputStream = activity.openFileOutput("Records.dat", Context.MODE_APPEND);
            String target = "";
            for (Record record : records) {
                target += record.toString() + '\n';
            }
            byte []bytes = (target).getBytes("UTF-8");
            outputStream.write(bytes);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            res = false;
        } catch (IOException e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    public static boolean saveEvents(ArrayList<Event> events, Activity activity) {
        boolean res = true;
        try {
            FileOutputStream outputStream = activity.openFileOutput("Events.dat", Context.MODE_PRIVATE);
            String target = "";
            for (Event event : events) {
                target += event.toString() + '\n';
            }
            byte []bytes = (target).getBytes("UTF-8");
            outputStream.write(bytes);
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            res = false;
        } catch (IOException e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    public static void copyRecords(ArrayList<Record> records, Activity activity) {
        ClipboardManager clipboardManager = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
        String target = "";
        for (Record record : records) {
            target += record.toString() + '\n';
        }
        ClipData clipData = ClipData.newPlainText("records", target);
        clipboardManager.setPrimaryClip(clipData);
    }

    public static void copyEvents(ArrayList<Event> events, Activity activity) {
        ClipboardManager clipboardManager = (ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
        String target = "";
        for (Event event : events) {
            target += event.toString() + '\n';
        }
        ClipData clipData = ClipData.newPlainText("events", target);
        clipboardManager.setPrimaryClip(clipData);
    }
}
