package com.example.kevin.localization;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    final static int AP_NUMBER = 6;
    final static int SCAN_PER_LOC = 3;
    final static int SIG_LOSS = -100;
    final static int K = 3; // default k value for knn()
    final static double REAL_X_INTERVAL = 1.2; //interval is 1.2m
    final static double REAL_Y_INTERVAL = 1.2; //interval is 1.2m
    final static double MOVE_INTERVAL = 25; //超過則判斷錯誤

    private int counter = 0;
    private myPoint current_p, prev_p;
    private double Dn; //最長的那條路方位，基準值
    private double D; //degree at a point

    //debug use
    private TextView textView;

    //sensors
    private SensorManager sensorManager;
    private Sensor aSensor; //加速度感測
    private Sensor mfSensor; //磁場感測
    private SensorEventListener sensorEventListener;

    double accelarate; //加速度


    //UI Widgets
    private ImageView imageView;
    private Canvas canvas;
    private Paint paint;
    private Bitmap bitmap;
    private Point point; //get display area's width and height
    private Office office;
    private Bitmap userIcon;

    //database
    static final String db_name = "WirelessLocation.db";
    static final String tb_name = "data";
    SQLiteDatabase db;
    ArrayList<Fingerprinting> fingerPrintSet; //dynamic array to store data read from database

    //wifi
    private WifiManager wifi;
    private WifiReceiver wifireceiver;
    private List<ScanResult> scanresult;
    private AP APGroup[];
    private int FpSum[];//to get average fingerprinting
    private Fingerprinting UserFp = new Fingerprinting(AP_NUMBER); //fingerprinting at user's current location
    Fingerprinting tmp;

    //UI Variables
    private float prevX, prevY;
    private Boolean prevFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userIcon = ((BitmapDrawable) getResources().getDrawable(R.mipmap.ic_launcher)).getBitmap();

        current_p = new myPoint();
        prev_p = new myPoint();

        //register sensor service
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mfSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorEventListener = new SensorEventListener() {

            private float[] accelerometerValues;
            private float[] magneticFieldValues;

            @Override
            public void onSensorChanged(SensorEvent event) {

                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    //加速度感測

                    //Log.i("sensor", "accelaration");
                    accelerometerValues = (float[]) event.values.clone();
                    //accelarate = Math.sqrt(Math.pow(accelerometerValues[0], 2) + Math.pow(accelerometerValues[1], 2));
                    accelarate = accelerometerValues[2];

                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    //Log.i("sensor", "megnetic");
                    //指南針
                    magneticFieldValues = (float[]) event.values.clone();

                }
                if (accelerometerValues != null && magneticFieldValues != null) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            calculateOrientation();
                            textView.setText(String.valueOf(D));

                            textView.setText(String.valueOf(accelarate));

                            if (accelarate > 10.5 || accelarate < 10) {
                                double move = 0.01;
                                double biasAngle = 45;

                                if (current_p.x >= 2 && current_p.x <= 24 && current_p.y == 0) {
                                    //老師那排
                                    Dn = -180;

                                    if (D <= Dn + biasAngle && D >= -Dn - biasAngle) {
                                        //視為前進
                                        drawDot(current_p.x + move, current_p.y);
                                        //textView.setText(String.valueOf(D) + " 老師前進");
                                        current_p.x += move;
                                        Log.i("方向", "老師前進");
                                    } else if (D <= Dn + 180 + biasAngle && D >= Dn + 180 - biasAngle) {
                                        drawDot(current_p.x - move, current_p.y);
                                        //textView.setText(String.valueOf(D) + " 老師後退");
                                        current_p.x -= move;
                                        Log.i("方向", "老師後退");
                                    }


                                } else if (current_p.x >= 0 && current_p.x <= 1 && current_p.y >= 0 && current_p.y <= 47) {
                                    //中間那排
                                    Dn = 100;

                                    if (D <= Dn + biasAngle && D >= Dn - biasAngle) {
                                        //視為前進
                                        drawDot(current_p.x, current_p.y - move);
                                        //textView.setText(String.valueOf(D) + " 中間前進");
                                        current_p.y -= move;
                                        Log.i("方向", "中間前進");
                                    } else if (D <= Dn - 180 + biasAngle && D >= Dn - 180 - biasAngle) {
                                        drawDot(current_p.x, current_p.y + move);
                                        //textView.setText(String.valueOf(D) + " 中間後退");
                                        current_p.y += move;
                                        Log.i("方向", "中間後退");
                                    }

                                } else if (current_p.x >= 0 && current_p.x <= 12 && current_p.y >= 46 && current_p.y <= 47) {
                                    //實驗室那排
                                    Dn = 10;

                                    if (D <= Dn + biasAngle && D >= Dn - biasAngle) {
                                        //視為前進
                                        drawDot(current_p.x - move, current_p.y);
                                        //textView.setText(String.valueOf(D) + " Lab前進");
                                        current_p.x -= move;
                                        Log.i("方向", "Lab前進");
                                    } else if (D <= Dn - 180 + biasAngle && D >= Dn - 180 - biasAngle + 360) {
                                        drawDot(current_p.x + move, current_p.y);
                                        //textView.setText(String.valueOf(D) + " Lab後退");
                                        current_p.x += move;
                                        Log.i("方向", "Lab後退");
                                    }
                                }
                            }
                        }
                    });

                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }

            private void calculateOrientation() {

                Log.i("calculate", "orientation");

                float[] values = new float[3];
                float[] inR = new float[9];
                SensorManager.getRotationMatrix(inR, null, accelerometerValues, magneticFieldValues);


                float[] outR = new float[9];
                SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);

                SensorManager.getOrientation(inR, values);
                values[0] = (float) Math.toDegrees(values[0]);
                D = values[0];
                //for debug use

            }
        };

        sensorManager.registerListener(sensorEventListener, aSensor, sensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorEventListener, mfSensor, sensorManager.SENSOR_DELAY_FASTEST);

        //------------------------UI widgets code start------------------------
        textView = (TextView) findViewById(R.id.textView);
        imageView = (ImageView) findViewById(R.id.imageView);
        point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        bitmap = Bitmap.createBitmap(point.x, point.y, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        imageView.setImageBitmap(bitmap);

        initializeDraw();
        office = new Office(MainActivity.this);

        //------------------------UI widgets code finish-----------------------

        //wifi and database initialization
        fingerPrintSet = new ArrayList<>();
        APGroup = new AP[AP_NUMBER];
        FpSum = new int[AP_NUMBER];

        for (int i = 0; i < AP_NUMBER; i++) {
            APGroup[i] = new AP();
            FpSum[i] = 0;
        }

        setAPinfo(); //assign MAC address to each AP

        //initialize wifi objects
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        if (!wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(true);
        }

        wifireceiver = new WifiReceiver();
        registerReceiver(wifireceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        //open database
        db = SQLiteDatabase.openDatabase(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + db_name, null, 1);

        //use cursor to return query result
        Cursor cursor = db.rawQuery("SELECT * FROM " + tb_name, null);

        if (cursor.moveToFirst()) {

            do {
                //read data to class(data structure) defined by ourselves
                Fingerprinting cur = new Fingerprinting(AP_NUMBER);
                cur.bias = -1; //reset bias to -1
                cur.x = cursor.getInt(0); //get x coordinate
                cur.y = cursor.getInt(1); //get y coordinate

                //get signal strength of all APs
                for (int i = 0; i < AP_NUMBER; i++) {
                    cur.data[i] = cursor.getInt(i + 2);
                }

                fingerPrintSet.add(cur); //add an data to array

            } while (cursor.moveToNext());

        }

        //finished reading, close database
        db.close();

        wifi.startScan();
    }

    @Override
    public void onResume() {
        registerReceiver(wifireceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    @Override
    public void onPause() {
        unregisterReceiver(wifireceiver);
        super.onPause();
    }

    public class WifiReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            //take action when receive wifi signal
            counter = (counter + 1) % SCAN_PER_LOC;

            addToSum();

            if (counter != 0) {

                //by prediction

            } else {

                //calculate location and pass parameter to display function
                for (int i = 0; i < AP_NUMBER; i++) {
                    FpSum[i] /= SCAN_PER_LOC;
                }

                //not sure, to be checked
                UserFp.setData(FpSum);

                tmp = KNN(K);

                current_p.x = tmp.x;
                current_p.y = tmp.y;

                //debug use
                Toast.makeText(MainActivity.this, String.valueOf(current_p.x) + "," + String.valueOf(current_p.y), Toast.LENGTH_SHORT).show();

                drawDot(current_p.x, current_p.y);

                //reset
                for (int i = 0; i < AP_NUMBER; i++) {
                    FpSum[i] = 0;
                }

            }


            wifi.startScan();
        }
    }

    public void setAPinfo() {
        APGroup[0].setMAC("ac:22:0b:d1:89:50"); //repeater1 2.4G
        APGroup[1].setMAC("ac:22:0b:31:49:48"); //repeater2 2.4G
        APGroup[2].setMAC("ac:22:0b:30:7b:90"); //mintloc2 2.4G
        APGroup[3].setMAC("ac:22:0b:31:46:10"); //research AP1 2.4G
        APGroup[4].setMAC("ac:22:0b:d1:5e:40"); //research AP2 2.4G
        APGroup[5].setMAC("ac:22:0b:31:3b:4b"); //research AP3 2.4G
    }

    public void addToSum() {

        scanresult = wifi.getScanResults();
        boolean flags[] = new boolean[AP_NUMBER]; //flag to mark missing signals

        for (int i = 0; i < AP_NUMBER; i++) {
            flags[i] = false; //false means the AP is not founded by our device
        }

        for (int i = 0; i < scanresult.size(); i++) {

            for (int j = 0; j < AP_NUMBER; j++) {

                if (scanresult.get(i).BSSID.equals(APGroup[j].MAC)) {
                    //sum
                    FpSum[j] += scanresult.get(i).level;
                    flags[j] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < AP_NUMBER; i++) {
            if (!flags[i]) {
                //replace lost signal with constant SIG_LOSS
                FpSum[i] += SIG_LOSS;
            }
        }
    }

    public Fingerprinting KNN(int k) {

        for (int i = 0; i < fingerPrintSet.size(); i++) {

            for (int j = 0; j < AP_NUMBER; j++) {
                //sum of Xn^2
                fingerPrintSet.get(i).bias += Math.pow(UserFp.data[j] - fingerPrintSet.get(i).data[j], 2);
            }
            //square
            fingerPrintSet.get(i).bias = Math.sqrt(fingerPrintSet.get(i).bias);
        }

        Collections.sort(fingerPrintSet); //sort by bias

        Fingerprinting resultfp = new Fingerprinting(AP_NUMBER);

        //only need to calculate x, y value
        for (int i = 0; i < k; i++) {

            resultfp.x += fingerPrintSet.get(i).x;
            resultfp.y += fingerPrintSet.get(i).y;
        }

        resultfp.x /= k;
        resultfp.y /= k;

        for (int i = 0; i < fingerPrintSet.size(); i++) {
            //reset bias
            fingerPrintSet.get(i).bias = 0;
        }

        return resultfp;
    }


    private void initializeDraw() {
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);
        bitmap = Bitmap.createBitmap(point.x, point.y, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

        //paint
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);


        imageView.setImageBitmap(bitmap);
        LayoutParams params = imageView.getLayoutParams();
        params.width = point.x;
        params.height = point.y;
        imageView.setLayoutParams(params);
    }

    private void drawDot(double x, double y) {

        if (prevFlag) {
            clearPrev();
        }
        float xx, yy;
        xx = (float) (3.4 + 0.33 * x) / 13 * point.x;
        yy = (float) (1.6 + 16.5 / 46 * y) / (float) 19.5 * point.y;
        //Log.i("MainActivity", "point.x is "+ point.x);
        //Log.i("MainActivity", "point.y is " + point.y);
        canvas.drawPoint(xx, yy, paint);
        /*initializeDraw();
        canvas.drawBitmap(userIcon, xx, yy, paint);*/
        imageView.invalidate();

        prevFlag = true;
        prevX = xx;
        prevY = yy;

        //Log.i("MainActivity", "in drawDot");
        /*String temp;
        if ((temp = office.getOfficeInfo(100 * (int) x + (int) y)) != null) {
            String officeInfo = "" + office.getOfficeInfo(100 * (int) x + (int) y);

            Log.i("office information", officeInfo);
            textView.setText(officeInfo);
        } else {
            // textView.setText("");
        }*/
    }

    private void clearPrev() {
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(7);
        canvas.drawPoint(prevX, prevY, paint);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5);
    }

    private boolean checkLoc(double cx, double cy, double px, double py) {

        if (Math.sqrt(Math.pow(cx - px, 2) * REAL_X_INTERVAL * REAL_X_INTERVAL + Math.pow(cy - py, 2) * REAL_Y_INTERVAL * REAL_Y_INTERVAL) > MOVE_INTERVAL) {
            return false;
        }


        return true;
    }


    public class myPoint {

        double x, y;

        public myPoint() {
            x = y = -1;
        }
    }
}
