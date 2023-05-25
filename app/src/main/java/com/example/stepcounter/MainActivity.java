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


public class MainActivity extends Activity implements View.OnClickListener{

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

    private HealthController healthController;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        healthController = new HealthController(this);

        healthController.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        healthController.stop();
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