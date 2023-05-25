package com.example.stepcounter;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;
import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.wear.widget.BoxInsetLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.stepcounter.databinding.ActivityMainBinding;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthController implements SensorEventListener {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private static final int SENSOR_DELAY_MICROS = 60 * 1000;
    private TextView tvStep, tvHeart, tvX, tvY, tvZ, tvSpeed, tvStress;
    private BoxInsetLayout frame;
    private Button btnReset;

    private long lastUpdate = 0;
    private long lastUpdate2 = 0;

    private SensorManager sensorManager;

    private Sensor mStepCounter, mHeartRate, mAccelerometer;

    private Boolean isSPresent, isHPresent, isAPresent;

    int stepCount = 0;

    double x = 0, y = 0, z = 0;

    double x2 = 0, y2 = 0, z2 = 0;

    private double lx = 0, ly = 0, lz = 0;

    private ArrayList<Double> xData = new ArrayList<>();
    private ArrayList<Double> yData = new ArrayList<>();
    private ArrayList<Double> zData = new ArrayList<>();
    long heartRate = 0;
    private ActivityMainBinding binding;

    private List<Long> rrIntervals = new ArrayList<>();

    private static final int WINDOW_SIZE = 25; // window size in samples
    private static final int THRESHOLD = 15;   // threshold for fall detection

    private float[] gravity = new float[3];
    private double[] magnitude = new double[WINDOW_SIZE];
    private int magnitudeIndex = 0;

    private Activity activity;

    public HealthController(Activity activity) {
        this.activity = activity;
    }
    public void start(){

        Log.d("KAMBING", "sasaassd");
        if(ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){ //ask for permission
            requestPermissions(activity, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }

        if(ContextCompat.checkSelfPermission(activity,
                Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_DENIED){ //ask for permission
            requestPermissions(activity, new String[]{Manifest.permission.BODY_SENSORS}, 0);
        }

        sensorManager = (SensorManager) activity.getSystemService(SENSOR_SERVICE);

        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null){

            Log.d("Unta", "kambing");

            mStepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSPresent = true;
        } else {
//            tvStep.setText('x');
            isSPresent = false;
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null){
            mHeartRate = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
            isHPresent = true;
        } else {
            tvHeart.setText('x');
            isHPresent = false;
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            isAPresent = true;
        } else {
//            tvX.setText('x');
//            tvY.setText('x');
//            tvZ.setText('x');
            isAPresent = false;
        }

        if (isSPresent) {
            sensorManager.registerListener(this, mStepCounter, SENSOR_DELAY_NORMAL);
        }

        if (isHPresent) {
            sensorManager.registerListener(this, mHeartRate, SENSOR_DELAY_NORMAL);
        }

        if (isAPresent) {
            sensorManager.registerListener(this, mAccelerometer, SENSOR_DELAY_NORMAL);
        }

        lastUpdate = System.currentTimeMillis();
    }

    public void stop() {
        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null){
            sensorManager.unregisterListener(this, mStepCounter);
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null){
            sensorManager.unregisterListener(this, mHeartRate);
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            sensorManager.unregisterListener(this, mAccelerometer);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(sensorEvent.sensor == mStepCounter){
            stepCount = (int) sensorEvent.values[0];
//            tvStep.setText(String.valueOf(stepCount));

            Log.d("STEP", String.valueOf(stepCount));
        }

        if(sensorEvent.sensor == mHeartRate){

            long curTime2 = System.currentTimeMillis();
            long diffTime2 =  curTime2 - lastUpdate2;

            heartRate = (long) sensorEvent.values[0];

            rrIntervals.add(heartRate);
//            tvHeart.setText(String.valueOf(heartRate));

//            Log.d("time", String.valueOf(diffTime2));

            Log.d("HEART RATE", String.valueOf(heartRate));

            if(diffTime2 > SENSOR_DELAY_MICROS){
                lastUpdate2 = curTime2;
            }

        }

        if(sensorEvent.sensor == mAccelerometer){

            long curTime = System.currentTimeMillis();
            long diffTime =  curTime - lastUpdate;

            xData.add((double) sensorEvent.values[0]);
            yData.add((double) sensorEvent.values[1]);
            zData.add((double) sensorEvent.values[2]);

            gravity[0] = 0.9f * gravity[0] + 0.1f * sensorEvent.values[0];
            gravity[1] = 0.9f * gravity[1] + 0.1f * sensorEvent.values[1];
            gravity[2] = 0.9f * gravity[2] + 0.1f * sensorEvent.values[2];
            double x = sensorEvent.values[0] - gravity[0];
            double y = sensorEvent.values[1] - gravity[1];
            double z = sensorEvent.values[2] - gravity[2];

            x2 = (double) sensorEvent.values[0];
            y2 = (double) sensorEvent.values[1];
            z2 = (double) sensorEvent.values[2];

//            tvX.setText(String.format("%.2f", x));
//            tvY.setText(String.format("%.2f", y));
//            tvZ.setText(String.format("%.2f", z));

            double r = Math.sqrt((x*x) + (y*y) + (z*z));

            double t = Math.sqrt((x2*x2) + (y2*y2) + (z2*z2));
//            Log.d("FALL", String.format("%.2f", r));
            magnitude[magnitudeIndex] = r;
            magnitudeIndex = (magnitudeIndex + 1) % WINDOW_SIZE;


            float sum = 0;
            for (double m : magnitude) {
                sum += m;
            }
            float avg = sum / WINDOW_SIZE;

//            Log.d("AVG", String.valueOf(avg));
            // FALL DETECTION
            if(t < 1) {


            }

            if (avg > THRESHOLD) {
//                frame.setBackgroundColor(Color.RED);
            }
//            Log.d("difftime", String.valueOf(diffTime));


            // IDLE TIME
            if(diffTime > SENSOR_DELAY_MICROS){

                double a = 9.0;
                double b = 1.0;

                double xRMS = calculateRMS(xData);
                double yRMS = calculateRMS(yData);
                double zRMS = calculateRMS(zData);
                double overallRMS = (xRMS + yRMS + zRMS) / 3.0;

                double stress_level = 1/(1 + Math.exp(-(a * overallRMS + b)));

                Log.d("STRESS", String.valueOf(stress_level));
                lastUpdate = curTime;

//                double speed = Math.abs( x + y + z - lx - ly - lz) / diffTime * 10000;
                double speed = xRMS + yRMS + zRMS;
                Log.d("speed", String.valueOf(speed));

//                tvSpeed.setText(String.format("%.2f", speed));

                if(speed < 11){
//                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
//                    long[] vibrationPattern = {0, 500, 50, 300};
                    //-1 - don't repeat
//                    final int indexInPatternToRepeat = -1;
//                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
//                    tvSpeed.setBackgroundColor(Color.parseColor("#BA450F"));
                } else {
//                    tvSpeed.setBackgroundColor(Color.parseColor("#1234B1"));
                }

                lx = x;
                ly = y;
                lz = z;


                xData.clear();
                yData.clear();
                zData.clear();
            }
        }
    }


    public void kambing(Activity activity){



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private double getAverage(List<Long> numbers) {
        long sum = 0;
        for (long number : numbers) {
            sum += number;
        }
        return (double) sum / numbers.size();
    }

    public double calculateRMS(ArrayList<Double> values){
        double sum = 0.0;
        for (double value : values) {
            sum += value * value;
        }
        double mean = sum / values.size();
        return Math.sqrt(mean);
    }
}
