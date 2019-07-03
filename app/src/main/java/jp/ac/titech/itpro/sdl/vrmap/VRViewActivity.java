package jp.ac.titech.itpro.sdl.vrmap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VRViewActivity extends AppCompatActivity implements SensorEventListener ,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = "VRViewActivity";
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private GLSurfaceView mContentView;

    private SensorManager sensorManager;
    private Sensor gravitySensor;
    private Sensor magneticFieldSensor;

    private FieldRenderer renderer;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };
    private static final float ALPHA = 0.9f;
    private float[] geometric;
    private float[] gravity;
    private float[] rotationMatrix = new float[9];
    private float[] remapRotationMatrix = new float[9];
    private float[] attitude = new float[3];
    private float destination_latitude;
    private float destination_longitude;
    private float current_latitude;
    private float current_longitude;
    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private final static int REQ_PERMISSIONS = 1111;
    private GoogleApiClient apiClient;
    private FusedLocationProviderClient locationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vrview);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
        renderer = new FieldRenderer();
        mContentView.setRenderer(renderer);

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        Uri uri = getIntent().getData();
        String url = uri.toString();
        String[] parts = url.split(":");

        parts = parts[1].split("\\?");

        parts = parts[0].split(",");
        destination_latitude = Float.valueOf(parts[0]);
        destination_longitude = Float.valueOf(parts[1]);


        callback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    current_latitude = (float)location.getLatitude();
                    current_longitude = (float)location.getLongitude();
                    float[] distance = new float[3];
                    Location.distanceBetween(current_latitude, current_longitude, destination_latitude, destination_longitude, distance);
                    Log.i(TAG, "onLocationResult: " + distance[0] + " " + distance[1]);
                    float deg = (float) (distance[1]/180*Math.PI);
                    float x = (float) (distance[0]*Math.sin(deg));
                    float y = (float) (distance[0]*Math.cos(deg));
                    Log.i(TAG, "onLocationResult: " + x + " " + y);
                    renderer.setRelativePosition(x, y);
                }
            }
        };

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (geometric == null){
                    geometric = event.values.clone();
                }else{
                    float[] latest_geometric = event.values.clone();
                    for(int i = 0;i < geometric.length;i++){
                        geometric[i] = geometric[i]*ALPHA+(1f-ALPHA)*latest_geometric[i];
                    }
//                    Log.i(TAG, "onSensorChanged: " + geometric[0] + " " + geometric[1] + " " + geometric[2]);
                }
                break;
                
            case Sensor.TYPE_GRAVITY:
                if (gravity == null){
                    gravity = event.values.clone();
                }else{
                    float[] latest_gravity = event.values.clone();
                    for (int i = 0;i < gravity.length;i++){
                        gravity[i] = gravity[i]*ALPHA+(1f-ALPHA)*latest_gravity[i];
                    }
                }

                break;
        }
        
        if (geometric != null && gravity != null){
            float out[] = new float[3];
            out[0] = geometric[1]*gravity[2]-geometric[2]*gravity[1];
            out[1] = geometric[2]*gravity[0]-geometric[0]*gravity[2];
            out[2] = geometric[0]*gravity[1]-geometric[1]*gravity[0];
            float geometrica[] = new float[3];
            geometrica[0] = -out[1]*gravity[2]+out[2]*gravity[1];
            geometrica[1] = -out[2]*gravity[0]+out[0]*gravity[2];
            geometrica[2] = -out[0]*gravity[1]+out[1]*gravity[0];
            float mat[] = new float[16];
            float norm = 0;
            for(int i = 0;i < 3;i++){
                norm += out[i]*out[i];
            }
            norm = (float) Math.sqrt(norm);
            for(int i = 0;i < 3;i++){
                mat[i] = out[i]/norm;
            }
            mat[3] = 0;
            norm = 0;
            for(int i = 0;i < 3;i++){
                norm += geometrica[i]*geometrica[i];
            }
            norm = (float) Math.sqrt(norm);
            for(int i = 0;i < 3;i++){
                mat[i+4] = geometrica[i]/norm;
            }
            mat[7] = 0;
            norm = 0;
            for(int i = 0;i < 3;i++){
                norm += gravity[i]*gravity[i];
            }
            norm = (float) Math.sqrt(norm);
            for(int i = 0;i < 3;i++){
                mat[i+8] = gravity[i]/norm;
            }
            mat[11] = 0;
            for(int i = 0;i < 3;i++){
                mat[i+12] = 0;
            }
            mat[15] = 1;
            float inv[] = new float[16];
            Matrix.invertM(inv, 0, mat, 0);


            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geometrica);
//            SensorManager.getOrientation(rotationMatrix)
//            Log.i(TAG, "onSensorChanged: x = " + rotationMatrix[1] + " y = " + rotationMatrix[4] + " z = " + rotationMatrix[7]);
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapRotationMatrix);
            SensorManager.getOrientation(remapRotationMatrix, attitude);
            renderer.setAttitude(mat, inv);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magneticFieldSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        apiClient.connect();

    }

    @Override
    protected void onStop() {
        apiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (checkPermission(true)){
            locationProviderClient.requestLocationUpdates(locationRequest, callback, null);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private boolean checkPermission(boolean reqPermission) {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                if (reqPermission) {
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQ_PERMISSIONS);
                } else {
                    String text = getString(R.string.toast_requires_permission, permission);
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show();
                }
                return false;
            }
        }
        return true;
    }
}
