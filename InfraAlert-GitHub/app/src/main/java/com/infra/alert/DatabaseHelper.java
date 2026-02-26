package com.infra.alert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) { super(context, "infra_alert.db", null, 1); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE downtime_types (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, message TEXT)");
        db.execSQL("CREATE TABLE engineers (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, downtime_id INTEGER)");
        db.execSQL("CREATE TABLE logs (id INTEGER PRIMARY KEY AUTOINCREMENT, engineer TEXT, phone TEXT, downtime TEXT, message TEXT, time DATETIME DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        db.execSQL("DROP TABLE IF EXISTS downtime_types");
        db.execSQL("DROP TABLE IF EXISTS engineers");
        db.execSQL("DROP TABLE IF EXISTS logs");
        onCreate(db);
    }

    public void addDowntimeType(String name, String message) {
        ContentValues cv = new ContentValues();
        cv.put("name", name); cv.put("message", message);
        getWritableDatabase().insert("downtime_types", null, cv);
    }

    public void addEngineer(String name, String phone, String dtId) {
        ContentValues cv = new ContentValues();
        cv.put("name", name); cv.put("phone", phone); cv.put("downtime_id", dtId);
        getWritableDatabase().insert("engineers", null, cv);
    }

    public void deleteDowntimeType(int id) {
        getWritableDatabase().delete("downtime_types", "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteEngineer(int id) {
        getWritableDatabase().delete("engineers", "id=?", new String[]{String.valueOf(id)});
    }

    public void addLog(String engineer, String phone, String downtime, String message) {
        ContentValues cv = new ContentValues();
        cv.put("engineer", engineer); cv.put("phone", phone);
        cv.put("downtime", downtime); cv.put("message", message);
        getWritableDatabase().insert("logs", null, cv);
    }

    public String getDowntimesJson() {
        JSONArray arr = new JSONArray();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM downtime_types", null);
        while (c.moveToNext()) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", c.getInt(0)); o.put("name", c.getString(1)); o.put("message", c.getString(2));
                arr.put(o);
            } catch (Exception ignored) {}
        }
        c.close(); return arr.toString();
    }

    public String getEngineersJson(String dtId) {
        JSONArray arr = new JSONArray();
        String q = "SELECT e.id,e.name,e.phone,d.name FROM engineers e LEFT JOIN downtime_types d ON e.downtime_id=d.id";
        if (dtId != null && !dtId.isEmpty()) q += " WHERE e.downtime_id=" + dtId;
        Cursor c = getReadableDatabase().rawQuery(q, null);
        while (c.moveToNext()) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", c.getInt(0)); o.put("name", c.getString(1));
                o.put("phone", c.getString(2)); o.put("downtime_name", c.getString(3));
                arr.put(o);
            } catch (Exception ignored) {}
        }
        c.close(); return arr.toString();
    }

    public String getLogsJson() {
        JSONArray arr = new JSONArray();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM logs ORDER BY id DESC LIMIT 100", null);
        while (c.moveToNext()) {
            try {
                JSONObject o = new JSONObject();
                o.put("id", c.getInt(0)); o.put("engineer", c.getString(1));
                o.put("phone", c.getString(2)); o.put("downtime", c.getString(3));
                o.put("message", c.getString(4)); o.put("time", c.getString(5));
                arr.put(o);
            } catch (Exception ignored) {}
        }
        c.close(); return arr.toString();
    }
}
