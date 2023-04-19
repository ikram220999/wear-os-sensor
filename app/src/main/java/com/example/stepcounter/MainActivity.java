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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.stepcounter.databinding.ActivityMainBinding;

import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener{

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private static final int SENSOR_DELAY_MICROS = 60 * 1000;
    private TextView tvStep, tvHeart, tvX, tvY, tvZ, tvSpeed;
    private BoxInsetLayout frame;
    private Button btnReset;

    private long lastUpdate = 0;

    private SensorManager sensorManager;

    private Sensor mStepCounter, mHeartRate, mAccelerometer;

    private Boolean isSPresent, isHPresent, isAPresent;

    int stepCount = 0;

    double x = 0, y = 0, z = 0;

    private double lx = 0, ly = 0, lz = 0;

    private ArrayList<Double> xData = new ArrayList<>();
    private ArrayList<Double> yData = new ArrayList<>();
    private ArrayList<Double> zData = new ArrayList<>();
    double heartRate = 0.0;
    private ActivityMainBinding binding;

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
            heartRate = (double) sensorEvent.values[0];
            tvHeart.setText(String.valueOf(heartRate));
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

            if(r < 1){
                frame.setBackgroundColor(Color.RED);

                RequestQueue queue = Volley.newRequestQueue(this);
                String url = "http://192.168.0.103/kambing/post.php";

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

                queue.add(stringRequest);

            }
            Log.d("difftime", String.valueOf(diffTime));

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