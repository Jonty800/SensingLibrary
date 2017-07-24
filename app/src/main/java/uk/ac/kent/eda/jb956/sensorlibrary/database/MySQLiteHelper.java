package uk.ac.kent.eda.jb956.sensorlibrary.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import au.com.bytecode.opencsv.CSVWriter;
import uk.ac.kent.eda.jb956.sensorlibrary.config.Settings;
import uk.ac.kent.eda.jb956.sensorlibrary.data.AccelerometerSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.ActivityData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.GyroSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.LightSensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.PositionsData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.ProximitySensorData;
import uk.ac.kent.eda.jb956.sensorlibrary.data.WifiData;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

/*

You can find your created database, named <your-database-name> in
//data/data/<Your-Application-Package-Name>/databases/<your-database-name>

- Pull it out using File explorer and rename it to have .db3 extension to use it in SQLiteExplorer
- Use File explorer of DDMS to navigate to emulator directory.
- You don't have access to the /data folder on a real phone. It's chmoded 700. You need root privileges to see it

*/
public class MySQLiteHelper extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = Settings.DATABASE_VERSION;
    // Database Name
    private static final String DATABASE_NAME = Settings.databaseName;
    private static MySQLiteHelper instance;
    private final Context context;

    private MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        Log.i("MySQLiteHelper", "Database size: " + getSize());
    }

    public static MySQLiteHelper getInstance(Context c) {
        if (instance == null)
            instance = new MySQLiteHelper(c);
        return instance;
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private static String getStringFromFile(File file) throws Exception {
        FileInputStream fin = new FileInputStream(file);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    //Happens when database is created
    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL statement to create table
        String CREATE_TABLE_GYRO = "CREATE TABLE gyro ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp BIGINT NOT NULL, " +
                "x FLOAT NOT NULL, " +
                "y FLOAT NOT NULL, " +
                "z FLOAT NOT NULL)";
        db.execSQL(CREATE_TABLE_GYRO);

        String CREATE_TABLE_ACC = "CREATE TABLE acc ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp BIGINT NOT NULL, " +
                "x FLOAT NOT NULL, " +
                "y FLOAT NOT NULL, " +
                "z FLOAT NOT NULL)";
        db.execSQL(CREATE_TABLE_ACC);

        String CREATE_TABLE_POSITIONS = "CREATE TABLE positions ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp BIGINT NOT NULL, " +
                "x FLOAT NOT NULL, " +
                "y FLOAT NOT NULL)";
        db.execSQL(CREATE_TABLE_POSITIONS);

        String CREATE_TABLE_ACT = "CREATE TABLE act ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp BIGINT NOT NULL, " +
                "activityCode INT NOT NULL, " +
                "confidence FLOAT NOT NULL)";
        db.execSQL(CREATE_TABLE_ACT);

        String CREATE_TABLE_WIFI = "CREATE TABLE wifi ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bssid VARCHAR(50) NOT NULL, " +
                "rssi FLOAT NOT NULL, " +
                "timestamp BIGINT NOT NULL, " +
                "distanceEstimate FLOAT NOT NULL)";
        db.execSQL(CREATE_TABLE_WIFI);

        String CREATE_TABLE_PROXIMITY = "CREATE TABLE proximity ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "proximity FLOAT NOT NULL, " +
                "timestamp BIGINT NOT NULL)";
        db.execSQL(CREATE_TABLE_PROXIMITY);

        String CREATE_TABLE_LIGHT = "CREATE TABLE light ( " +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "illuminance FLOAT NOT NULL, " +
                "timestamp BIGINT NOT NULL)";
        db.execSQL(CREATE_TABLE_LIGHT);
    }

    public void exportWifiDB() {
        File exportDir = new File(Settings.SAVE_PATH);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportWifiDB"+System.currentTimeMillis()+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM wifi", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3),curCSV.getString(4) };
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportGyroDB() {
        File exportDir = new File(Settings.SAVE_PATH);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportGyroDB"+System.currentTimeMillis()+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM gyro", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportAccDB() {
        File exportDir = new File(Settings.SAVE_PATH);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportAccDB-"+System.currentTimeMillis()+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM acc", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportAccDBWithRange(long timestampAInSeconds, long timestampBInSeconds) {
        File exportDir = new File(Settings.SAVE_PATH +"/" + timestampAInSeconds + "/sensorDataCSV/");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportAccDBWithRange-"+timestampAInSeconds+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            long start_ts = timestampAInSeconds;
            long end_ts = timestampBInSeconds;
            Cursor curCSV = db.rawQuery("SELECT * FROM acc where timestamp >=" + start_ts +" and timestamp <=" + end_ts, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportGyroDBWithRange(long timestampAInSeconds, long timestampBInSeconds) {
        File exportDir = new File(Settings.SAVE_PATH +"/" + timestampAInSeconds + "/sensorDataCSV/");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportGyroDBWithRange-"+timestampAInSeconds+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            long start_ts = timestampAInSeconds;
            long end_ts = timestampBInSeconds;
            Cursor curCSV = db.rawQuery("SELECT * FROM gyro where timestamp >=" + start_ts +" and timestamp <=" + end_ts, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3), curCSV.getString(4)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportLightDBWithRange(long timestampAInSeconds, long timestampBInSeconds) {
        File exportDir = new File(Settings.SAVE_PATH +"/" + timestampAInSeconds + "/sensorDataCSV/");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportLightDBWithRange-"+timestampAInSeconds+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            long start_ts = timestampAInSeconds;
            long end_ts = timestampBInSeconds;
            Cursor curCSV = db.rawQuery("SELECT * FROM light where timestamp >=" + start_ts +" and timestamp <=" + end_ts, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportPositionsDBWithRange(long timestampAInSeconds, long timestampBInSeconds) {
        File exportDir = new File(Settings.SAVE_PATH +"/" + timestampAInSeconds + "/sensorDataCSV/");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportPositionsDBWithRange-"+timestampAInSeconds+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            long start_ts = timestampAInSeconds;
            long end_ts = timestampBInSeconds;
            Cursor curCSV = db.rawQuery("SELECT * FROM positions where timestamp >=" + start_ts +" and timestamp <=" + end_ts, null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportActDB() {
        File exportDir = new File(Settings.SAVE_PATH);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportActDB"+System.currentTimeMillis()+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM act", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3) };
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void exportProximityDB() {
        File exportDir = new File(Settings.SAVE_PATH);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "exportProximityDB"+System.currentTimeMillis()+".csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM proximity", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to export
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2) };
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
            refreshFiles(file);
        } catch (Exception sqlEx) {
            Log.e("MySQLHelper", sqlEx.getMessage(), sqlEx);
        }
    }

    public void clearGyroDatabase(int limit) {
        SQLiteDatabase database = this.getWritableDatabase();
        Log.i("MySQLiteHelper", "Database size before delete (clearGyroDatabase): " + getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM gyro");
        else
            database.execSQL("DELETE FROM gyro WHERE id IN(SELECT id FROM gyro ORDER BY id ASC LIMIT " + limit + ")");

        Log.i("MySQLiteHelper", "Database size after delete (clearGyroDatabase): " + getSize());
    }

    public void clearAccDatabase(int limit) {
        SQLiteDatabase database = this.getWritableDatabase();
        Log.i("MySQLiteHelper", "Database size before delete (clearAccDatabase): " + getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM acc");
        else
            database.execSQL("DELETE FROM acc WHERE id IN(SELECT id FROM acc ORDER BY id ASC LIMIT " + limit + ")");
        Log.i("MySQLiteHelper", "Database size after delete (clearAccDatabase): " + getSize());
    }

    public void clearActivityDatabase(int limit) {
        SQLiteDatabase database = this.getWritableDatabase();
        Log.i("MySQLiteHelper", "Database size before delete (clearActivityDatabase): " + getSize());
        if (limit == -1)
            database.execSQL("DELETE FROM act");
        else
            database.execSQL("DELETE FROM act WHERE id IN(SELECT id FROM act ORDER BY id ASC LIMIT " + limit + ")");
        Log.i("MySQLiteHelper", "Database size after delete (clearActivityDatabase): " + getSize());
    }

    public void clearAll() {
        Log.i("MySQLiteHelper", "Deleting everything: " + getSize());
        SQLiteDatabase database = this.getWritableDatabase();
        database.execSQL("DELETE FROM act");
        database.execSQL("DELETE FROM gyro");
        database.execSQL("DELETE FROM acc");
        database.execSQL("DELETE FROM wifi");
        database.execSQL("DELETE FROM positions");
        database.execSQL("DELETE FROM proximity");
        File dir = new File(Settings.SAVE_PATH);
        DeleteRecursive(dir);
    }

    private void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
            {
                child.delete();
                DeleteRecursive(child);
                refreshFiles(child);
            }

        fileOrDirectory.delete();
    }

    public void addToAcc(AccelerometerSensorData data) {
        if (Settings.SAVE_ACCELEROMETER_TO_DATABASE) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("x", data.X);
                values.put("y", data.Y);
                values.put("z", data.Z);
                // values.put("user_id", NetworkCache.getInstance().user_id);
                values.put("timestamp", data.timestamp);
                db.insert("acc", null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // db.close();
    }

    public void addToLight(LightSensorData data) {
        if (Settings.SAVE_LIGHT_TO_DATABASE) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("illuminance", data.illuminance);
                values.put("timestamp", data.timestamp);
                db.insert("light", null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // db.close();
        }
    }

    public void addToPositions(PositionsData data) {
        if (Settings.SAVE_POSITIONS_TO_DATABASE) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("x", data.X);
                values.put("y", data.Y);
                values.put("timestamp", data.timestamp);
                db.insert("positions", null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // db.close();
    }

    public void addToActivity(ActivityData data) {
        if (Settings.SAVE_ACTIVITY_TO_DATABASE) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("activityCode", data.activityCode);
                values.put("confidence", data.confidence);
                values.put("timestamp", data.timestamp);
                db.insert("act", null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // db.close();
        }
    }

    public void addToProximity(ProximitySensorData data) {
        if (Settings.SAVE_PROXIMITY_TO_DATABASE) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("proximity", data.proximity);
                values.put("timestamp", data.timestamp);
                db.insert("proximity", null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // db.close();
        }
    }

    private void refreshFiles(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        context.sendBroadcast(mediaScanIntent);
    }

    public void addToWifi(WifiData data) {
        if (Settings.SAVE_WIFI_TO_DATABASE) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("bssid", data.bssid);
                values.put("rssi", data.rssi);
                values.put("timestamp", data.timestamp);
                values.put("distanceEstimate", data.distanceEstimate);
                db.insert("wifi", null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // db.close();
        }
    }

    public void addToGyro(GyroSensorData data) {
        if (Settings.SAVE_GYRO_TO_DATABASE) {
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("x", data.X);
                values.put("y", data.Y);
                values.put("z", data.Z);
                values.put("timestamp", data.timestamp);
                db.insert("gyro", null, values);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // db.close();
        }
    }

    private long getSize() {
        File f = context.getDatabasePath(DATABASE_NAME);
        return f.length();
    }

    /**
     * Occurs when the version number of the database is incremented
     *
     * @param db         The database object
     * @param oldVersion The old version number
     * @param newVersion The new version number
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS acc");
        db.execSQL("DROP TABLE IF EXISTS gyro");
        db.execSQL("DROP TABLE IF EXISTS act");
        db.execSQL("DROP TABLE IF EXISTS proximity");
        db.execSQL("DROP TABLE IF EXISTS wifi");
        db.execSQL("DROP TABLE IF EXISTS light");
        db.execSQL("DROP TABLE IF EXISTS positions");
        // create fresh table
        this.onCreate(db);
    }
}

