package com.example.a15ie3300401;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, linearAccelerometer, magnetometer, stepCount;
    private static final float NS2S = 1.0f / 1000000000.0f; //according to android timestamp documentation, should be 1/1000
    private float timestamp;
    private float dTa[] = {0,0,0}; //time of activity
    private float distance=0; //distance
    private double speed; //instantaneous speed
    private int mstepCount = 0, counterSteps = 0; //to store step count
    private float[] mGravity; //to store accelerometer sensor values (with gravity): 0,1,2
    private float[] mGeomagnetic; // to save magnetometer sensor values: 0,1,2
    private float[] mAcceleration; // to save accelerometer sensor values (without gravity): 0,1,2
    private float azimuth, pitch, roll;
    private String myactivity; //current activity of user.

    private static final float VALUE_DRIFT = 0.05f;
    static final float ALPHA = 0.25f; // if ALPHA = 1 OR 0, no filter applies (low pass filter).

    TextView pitch_text, roll_text, azimuth_text, ax_text, ay_text, az_text, speed_text, distance_text, current_activity_text, dts, dtw, dtr, stepCount_text ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pitch_text = findViewById(R.id.pitch_text);
        roll_text = findViewById(R.id.roll_text);
        azimuth_text = findViewById(R.id.azimuth_text);

        ax_text = findViewById(R.id.ax_text);
        ay_text = findViewById(R.id.ay_text);
        az_text = findViewById(R.id.az_text);

        speed_text = findViewById(R.id.speed_text);
        distance_text = findViewById(R.id.distance_text);

        current_activity_text = findViewById(R.id.current_activity_text);
        dts = findViewById(R.id.dts);
        dtw = findViewById(R.id.dtw);
        dtr = findViewById(R.id.dtr);

        stepCount_text = findViewById(R.id.stepCount_text);

        //register sensors
        sensorManager =(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepCount = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        //register sensor listeners
        if(linearAccelerometer!=null){
            sensorManager.registerListener(this,linearAccelerometer, sensorManager.SENSOR_DELAY_NORMAL);
        }
        if(accelerometer!=null){
            sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_NORMAL);
        }
        if(magnetometer!=null){
            sensorManager.registerListener(this,magnetometer,sensorManager.SENSOR_DELAY_NORMAL);
        }
        if(stepCount!=null){
            sensorManager.registerListener(this,stepCount, sensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    //low pass filter to get smoother results.
    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    boolean capturingData = true;

    public void onStartClick(View view) {
        capturingData = true;
    }

    public void onStopClick(View view) {
        capturingData = false;
    }

    public void onResetClick(View view){
        capturingData = false;
        mstepCount = 0;
        stepCount_text.setText(String.format("%d", mstepCount));

        mGravity[0] = 0;
        mGravity[1] = 0;
        mGravity[2] = 0;
        ax_text.setText(String.format("%.2f m/s^2", mGravity[0]));
        ay_text.setText(String.format("%.2f m/s^2", mGravity[1]));
        az_text.setText(String.format("%.2f m/s^2", mGravity[2]));

        pitch = 0;
        azimuth = 0;
        roll = 0;
        pitch_text.setText(String.format("%.2f\u00b0", pitch));
        azimuth_text.setText(String.format("%.2f\u00b0", azimuth));
        roll_text.setText(String.format("%.2f\u00b0", roll));

        dTa[0] = 0;
        dTa[1] = 0;
        dTa[2] = 0;
        dts.setText(String.format("%.2f s",dTa[0]));
        dtw.setText(String.format("%.2f s",dTa[1]));
        dtr.setText(String.format("%.2f s",dTa[2]));

        speed = 0;
        distance = 0;
        speed_text.setText(String.format("%.2f m/s",speed));
        distance_text.setText(String.format("%.2f m",distance));

        myactivity = "RESET";
        current_activity_text.setText(myactivity);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(capturingData && sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            if (counterSteps < 1) {
                // initial value
                counterSteps = (int)sensorEvent.values[0];
            }
            // Calculate steps taken based on first counter value received.
            mstepCount = (int)sensorEvent.values[0] - counterSteps;
            Log.v("step", String.valueOf(mstepCount));
            stepCount_text.setText(String.format("%d", mstepCount));
        }

        if(capturingData && sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = sensorEvent.values;
            ax_text.setText(String.format("%.2f m/s^2", mGravity[0]));
            ay_text.setText(String.format("%.2f m/s^2", mGravity[1]));
            az_text.setText(String.format("%.2f m/s^2", mGravity[2]));
        }
        if(capturingData && sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mGeomagnetic = sensorEvent.values;
        }

        if(capturingData && mGravity != null && mGeomagnetic != null){
            float R[] = new float[9];
            float I[] = new float[9];
            if(sensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)){
                float orientation[] = new float[3];
                sensorManager.getOrientation(R, orientation);
                azimuth = (float)Math.toDegrees((double)orientation[0]);
                pitch = (float)Math.toDegrees((double)orientation[1]);
                roll = (float)Math.toDegrees((double)orientation[2]);

                if (Math.abs(pitch) < VALUE_DRIFT){
                    pitch = 0;
                }
                if (Math.abs(roll) < VALUE_DRIFT){
                    roll = 0;
                }
                pitch_text.setText(String.format("%.2f\u00b0", pitch));
                azimuth_text.setText(String.format("%.2f\u00b0", azimuth));
                roll_text.setText(String.format("%.2f\u00b0", roll));
            }
        }

        if(capturingData && sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){

            float dT = (sensorEvent.timestamp - timestamp) * NS2S;
            if (dT > 100.0){
                dT = 0;
            }
            float x,y,z;
            double normal;
            double vx,vy,vz;

            mAcceleration = lowPass(sensorEvent.values.clone(), sensorEvent.values);

            x = mAcceleration[0];
            y = mAcceleration[1];
            z = mAcceleration[2];

            normal=Math.sqrt(x * x + y * y + z * z);

            long timeStamp = System.currentTimeMillis();

            vx = x*dT;
            vy = y*dT;
            vz = z*dT;

            speed = (float) (Math.sqrt(vx*vx + vy*vy + vz*vz)) ;
            //speed = normal*dT;
            if (speed < 0.05) {
                //speed = 0 ;
                myactivity = "Still";
                dTa[0] += dT;
            }
            else if(speed > 0.05 && speed < 0.5 ){
                myactivity = "Walking";
                dTa[1] += dT;
            }
            else if(speed > 0.5){
                myactivity = "Running";
                dTa[2] += dT;
            }
            else{
                myactivity = "Unknown";
            }

            speed_text.setText(String.format("%.2f m/s",speed));

            distance += speed*dT;

            distance_text.setText(String.format("%.2f m",distance));

            current_activity_text.setText(myactivity);
            dts.setText(String.format("%.2f s",dTa[0]));
            dtw.setText(String.format("%.2f s",dTa[1]));
            dtr.setText(String.format("%.2f s",dTa[2]));

            final String textdata = String.format("t=%d dT=%f x=%f y=%f z=%f acceleration=%f  vx=%f vy=%f vz=%f speed=%f activity=%s still=%f walking=%f running=%f distance=%f stepCount=%d", timeStamp,dT,x,y,z,normal,vx,vy,vz,speed, myactivity, dTa[0], dTa[1], dTa[2], distance, mstepCount);

            Log.v("Accelerometer", textdata);

            timestamp = sensorEvent.timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

}
