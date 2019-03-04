package com.example.a15ie3300401;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer, linearAccelerometer, magnetometer;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private float dTa[] = {0,0,0};
    private float distance=0;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private static final float VALUE_DRIFT = 0.05f;

    TextView pitch_text, roll_text, azimuth_text, ax_text, ay_text, az_text, speed_text, distance_text, current_activity_text, dts, dtw, dtr ;

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

        sensorManager =(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if(linearAccelerometer!=null){
            sensorManager.registerListener(this,linearAccelerometer, sensorManager.SENSOR_DELAY_UI);
        }
        if(accelerometer!=null){
            sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_UI);
        }
        if(magnetometer!=null){
            sensorManager.registerListener(this,magnetometer,sensorManager.SENSOR_DELAY_UI);
        }
    }

    boolean capturingData = true;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(capturingData && sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            mGravity = sensorEvent.values;
            ax_text.setText(String.format("%.2f m/s^2", mGravity[0]));
            ay_text.setText(String.format("%.2f m/s^2", mGravity[1]));
            az_text.setText(String.format("%.2f m/s^2", mGravity[2]));
        }
        if(capturingData && sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            mGeomagnetic = sensorEvent.values;
        }

        if(mGravity != null & mGeomagnetic != null){
            float R[] = new float[9];
            float I[] = new float[9];
            if(sensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)){
                float orientation[] = new float[3];
                sensorManager.getOrientation(R, orientation);
                float azimuth = orientation[0];
                float pitch = orientation[1];
                float roll = orientation[2];

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
            float vx,vy,vz,speed;

            String myactivity;
            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            z = sensorEvent.values[2];

            normal=Math.sqrt(x * x + y * y + z * z);

            long timeStamp = System.currentTimeMillis();

            vx = x*dT;
            vy = y*dT;
            vz = z*dT;

            speed = (float) (Math.sqrt(vx*vx + vy*vy + vz*vz)) ;
            if (speed < 0.05) {
                speed = 0 ;
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

            final String textdata = String.format("t=%d dT=%f x=%f y=%f z=%f acceleration=%f  vx=%f vy=%f vz=%f speed=%f activity=%s still=%f walking=%f running=%f distance=%f", timeStamp,dT,x,y,z,normal,vx,vy,vz,speed, myactivity, dTa[0], dTa[1], dTa[2], distance);

            Log.v("Accelerometer", textdata);

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    textView.setText(textdata);
//                }
//            });

            timestamp = sensorEvent.timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}


}
