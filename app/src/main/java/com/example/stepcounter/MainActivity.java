package com.example.stepcounter;

import static android.hardware.SensorManager.SENSOR_DELAY_NORMAL;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.wear.widget.BoxInsetLayout;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.stepcounter.databinding.ActivityMainBinding;

import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener{

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

    private double lx = 0, ly = 0, lz = 0;

    private ArrayList<Double> xData = new ArrayList<>();
    private ArrayList<Double> yData = new ArrayList<>();
    private ArrayList<Double> zData = new ArrayList<>();
    long heartRate = 0;
    private ActivityMainBinding binding;

    private List<Long> rrIntervals = new ArrayList<>();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED){ //ask for permission
            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
        }

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_DENIED){ //ask for permission
            requestPermissions(new String[]{Manifest.permission.BODY_SENSORS}, 0);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        frame = findViewById(R.id.frame);
        tvStep = findViewById(R.id.tvStep);
        tvHeart = findViewById(R.id.tvHeart);
        tvX = findViewById(R.id.tvX);
        tvY = findViewById(R.id.tvY);
        tvZ = findViewById(R.id.tvZ);
        tvSpeed = findViewById(R.id.tvSpeed);
        btnReset = findViewById(R.id.btnReset);
        tvStress = findViewById(R.id.tvStress);

        btnReset.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null){
            mStepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            isSPresent = true;
        } else {
            tvStep.setText('x');
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
            tvX.setText('x');
            tvY.setText('x');
            tvZ.setText('x');
            isAPresent = false;
        }

        lastUpdate = System.currentTimeMillis();
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == mStepCounter){
            stepCount = (int) sensorEvent.values[0];
            tvStep.setText(String.valueOf(stepCount));
        }

        if(sensorEvent.sensor == mHeartRate){

            long curTime2 = System.currentTimeMillis();
            long diffTime2 =  curTime2 - lastUpdate2;

            heartRate = (long) sensorEvent.values[0];

            rrIntervals.add(heartRate);
            tvHeart.setText(String.valueOf(heartRate));

            Log.d("time", String.valueOf(diffTime2));
            if(diffTime2 > SENSOR_DELAY_MICROS){
                calculateStressLevel();
                lastUpdate2 = curTime2;
            }

        }

        if(sensorEvent.sensor == mAccelerometer){

            long curTime = System.currentTimeMillis();
            long diffTime =  curTime - lastUpdate;

            xData.add((double) sensorEvent.values[0]);
            yData.add((double) sensorEvent.values[1]);
            zData.add((double) sensorEvent.values[2]);

            x = (double) sensorEvent.values[0];
            y = (double) sensorEvent.values[1];
            z = (double) sensorEvent.values[2];

            tvX.setText(String.format("%.2f", x));
            tvY.setText(String.format("%.2f", y));
            tvZ.setText(String.format("%.2f", z));

            double r = Math.sqrt((x*x) + (y*y) + (z*z));
//            Log.d("FALL", String.format("%.2f", r));


            // FALL DETECTION
            if(r < 1){
                frame.setBackgroundColor(Color.RED);

                RequestQueue queue = Volley.newRequestQueue(this);
//                String url = "http://192.168.0.103/kambing/post.php";
                String url = "http://192.168.0.103/bolt-laravel/public/api/machines/1/triggers/abnormal";

                StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Log.d("KAMBING", "BERJAYA ...");
                            }
                        },  new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("KAMBING", "TIDAK BERJAYA ..." + error);
                    }
                }){
                    @Override
                    protected Map<String,String> getParams(){
                        Map<String,String> params = new HashMap<String, String>();
                        params.put("fall","yes");

                        return params;
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String,String> params = new HashMap<String, String>();
                        params.put("Content-Type","application/x-www-form-urlencoded");
                        return params;
                    }
                };

//                queue.setRetryPolicy(new RetryPolicy() {
//                    @Override
//                    public int getCurrentTimeout() {
//                        return 50000;
//                    }
//
//                    @Override
//                    public int getCurrentRetryCount() {
//                        return 50000;
//                    }
//
//                    @Override
//                    public void retry(VolleyError error) throws VolleyError {
//
//                    }
//                });

                queue.add(stringRequest);

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

                tvSpeed.setText(String.format("%.2f", speed));

                if(speed < 11){
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 500, 50, 300};
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                    tvSpeed.setBackgroundColor(Color.parseColor("#BA450F"));
                } else {
                    tvSpeed.setBackgroundColor(Color.parseColor("#1234B1"));
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

    private void calculateStressLevel() {

        if(rrIntervals.size() >= 10) {

//            rrIntervals.clear();
//
//            rrIntervals.add((long)100);
//            rrIntervals.add((long)101);
//            rrIntervals.add((long)103);
//            rrIntervals.add((long)100);
//            rrIntervals.add((long)98);
//            rrIntervals.add((long)98);
//            rrIntervals.add((long)99);
//            rrIntervals.add((long)97);
//            rrIntervals.add((long)95);
//            rrIntervals.add((long)100);
//            rrIntervals.add((long)102);

            List<Long> successiveDifferences = new ArrayList<>();
            for (int i = 1; i < rrIntervals.size(); i++) {
                successiveDifferences.add(rrIntervals.get(i) - rrIntervals.get(i-1));
            }

            List<Long> squaredDifferences = new ArrayList<>();
            for (Long difference : successiveDifferences) {
                squaredDifferences.add(difference * difference);
            }

            double rmssd = Math.sqrt(getAverage(squaredDifferences));
//            int rmssdInt = (int) Math.round(rmssd);

            Log.d("STRESS LEVEL",String.valueOf(rmssd));

            rrIntervals.clear();
//            successiveDifferences.clear();
//            squaredDifferences.clear();

            tvStress.setText(String.format("%.2f", rmssd));

            if(rmssd > 1){
                tvStress.setBackgroundColor(Color.GREEN);
                tvStress.setTextColor(Color.BLACK);

            } else if (rmssd <= 1 && rmssd >= 0.7){
                tvStress.setBackgroundColor(Color.YELLOW);
                tvStress.setTextColor(Color.BLACK);

            } else if (rmssd == 0.0) {
                tvStress.setBackgroundColor(Color.BLACK);
                tvStress.setTextColor(Color.WHITE);

            }else {
                tvStress.setBackgroundColor(Color.RED);
                tvStress.setTextColor(Color.WHITE);

            }

        }
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null){
            sensorManager.registerListener(this, mStepCounter, SENSOR_DELAY_NORMAL);
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null){
            sensorManager.registerListener(this, mHeartRate, SENSOR_DELAY_NORMAL);
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            sensorManager.registerListener(this, mAccelerometer, 100000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

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
    public void onClick(View view) {
//        Button b = (Button) v;
        switch (view.getId()){
            case R.id.btnReset:
                frame.setBackgroundColor(Color.BLACK);
                break;
        }
    }
}