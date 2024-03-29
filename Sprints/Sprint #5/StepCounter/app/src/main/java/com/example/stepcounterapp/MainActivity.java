package com.example.stepcounterapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] accelerationValues = new float[3];
    private ArrayList<Float> normAccelerationValues = new ArrayList<Float>();
    private ArrayList<Float> accX = new ArrayList<Float>();
    private ArrayList<Float> accY = new ArrayList<Float>();
    private ArrayList<Float> accZ = new ArrayList<Float>();
    private  ArrayList<Long> tempo = new ArrayList<Long>();
    private boolean isRecording = false;
    private Button buttonStart, buttonStop;

    private long previousTimeStamp = 0;




    private static final String TAG = "MainActivity";

    private static final int RECORDING_TIME_MS = 30000; // 30 seconds
    private static final int SAMPLING_RATE_US = 20000; // 20ms = 50Hz


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        buttonStart = findViewById(R.id.buttonStart);
        buttonStop = findViewById(R.id.buttonStop);

        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRecording) {
            accelerationValues[0] = event.values[0];
            accX.add(accelerationValues[0]);
            accelerationValues[1] = event.values[1];
            accY.add(accelerationValues[0]);
            accelerationValues[2] = event.values[2];
            accZ.add(accelerationValues[0]);
            float normAcceleration = (float) Math.sqrt(
                    accelerationValues[0]*accelerationValues[0] +
                            accelerationValues[1]*accelerationValues[1] +
                            accelerationValues[2]*accelerationValues[2]
            );
            normAccelerationValues.add(normAcceleration);
            tempo.add(System.currentTimeMillis());


            // Checking the actual sampling rate of the accelerometer sensor
            long currentTimestamp = System.currentTimeMillis();
            if(previousTimeStamp != 0) {
                long timeDifference = currentTimestamp - previousTimeStamp;
                Log.d(TAG, "Time difference between sensor events: " + timeDifference);
            }
            previousTimeStamp = currentTimestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.buttonStart:
                startRecording();
                break;
            case R.id.buttonStop:
                stopRecording();
                break;
        }
    }

    private void startRecording() {
        Toast.makeText(this, "Test has been started", Toast.LENGTH_LONG).show();
        normAccelerationValues.clear();
        isRecording = true;
        buttonStart.setVisibility(View.INVISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                stopRecording();
            }
        }, RECORDING_TIME_MS); // 30 seconds
    }

    private void stopRecording() {


        Toast.makeText(this, "Test has been finished!", Toast.LENGTH_LONG).show();
        isRecording = false;
        buttonStart.setVisibility(View.VISIBLE);
        try {
            // Get the internal storage directory
            File directory = getFilesDir();
            // Create a new file in the directory
            File file = new File(directory, "dados_aceleracao.csv");
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            // Write the data to the file
            for (int i = 0; i < normAccelerationValues.size(); i++) {
                bw.write(tempo.get(i) + "," + accX.get(i) + "," +  accY.get(i) + "," + accZ.get(i) + "," + normAccelerationValues.get(i) + "\n");
            }
            bw.close();
            Log.e(TAG, "File saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Error writing to file: " + e.getMessage());
            }
    }


    public void countSteps(View view) {

        double threshold = 1.0;
        System.out.println("COMEÇOU A LEITURA!");
        // Get the internal storage directory
        File directory = getFilesDir();
        // Create a file object for the CSV file
        File file = new File(directory, "dados_aceleracao.csv");

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            double previousNormAcceleration = 0;
            int stepCount = 0;

            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                double normAcceleration = Double.parseDouble(data[4]);

                if (previousNormAcceleration == 0) {
                    previousNormAcceleration = normAcceleration;
                    continue;
                }

                double variation = normAcceleration - previousNormAcceleration;

                if (variation >= threshold) {
                    // Verifica se ocorreu uma variação maior ou igual ao limiar
                    double nextNormAcceleration = 0;

                    // Procura pelo próximo valor de norma de aceleração
                    while ((line = br.readLine()) != null) {
                        data = line.split(",");
                        nextNormAcceleration = Double.parseDouble(data[4]);

                        if (nextNormAcceleration < normAcceleration) {
                            break;
                        }
                    }

                    if (nextNormAcceleration < normAcceleration) {
                        // Verifica se ocorreu uma variação para baixo
                        stepCount++;
                    }
                }

                previousNormAcceleration = normAcceleration;
            }

            System.out.println("PASSOS: " + stepCount);
        } catch (IOException e) {
            e.toString();
        }
    }
}


