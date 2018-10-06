package com.example.ankita.screen_recorder;

import android.Manifest;
import android.annotation.TargetApi;
import android.arch.core.util.Function;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.AlteredCharSequence;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    Button btn_action;
    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    public MediaProjectionCallback mMediaProjectionCallback;

    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSION_KEY = 1;
    boolean isRecording = false;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
//    if(!Function.hasPermission(this, PERMISSIONS)){
//        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
//    }

    DisplayMetrics metrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(metrics);
    mScreenDensity = metrics.densityDpi;

    mMediaRecorder = new MediaRecorder();
    mProjectionManager = (MediaProjectionManager)
            getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    btn_action = (Button)findViewById(R.id.btn_action);
    btn_action.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            onToggleScreenShare();

        }

    });

    }
    public void actionBtnReload(){
        if (isRecording){
            btn_action.setText("stop recording");
        }else {
            btn_action.setText("start recording");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onToggleScreenShare(){
        if(!isRecording){
            initRecorder();
            shareScreen();
            }
            else {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                stopScreenSharing();
            }


        }


    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void shareScreen(){
        if(mMediaProjection == null){
          startActivityForResult(mProjectionManager.createScreenCaptureIntent(),REQUEST_CODE);

          return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
        isRecording = true;
        actionBtnReload();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(),
                null, null);
    }





    private void initRecorder() {
        try {
//            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //THREE_GPP
            mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/video.mp4");
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(16); // 30
            mMediaRecorder.setVideoEncodingBitRate(3000000);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        private void stopScreenSharing(){
    if (mVirtualDisplay == null){
        return;
    }
    mVirtualDisplay.release();
            destroyMediaProjection();
            isRecording = false;
            actionBtnReload();
        }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void destroyMediaProjection() {
    if (mMediaProjection!= null) {
    mMediaProjection.unregisterCallback(mMediaProjectionCallback);
    mMediaProjection.stop();
//    mMediaProjectionn == null;
    }
    Log.i(TAG, "MediaProjection Stopped");

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            isRecording = false;
            actionBtnReload();
            return;
        }
        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mVirtualDisplay = createVirtualDisplay();
        }
        mMediaRecorder.start();
        isRecording = true;
        actionBtnReload();
    }


@RequiresApi(api = Build.VERSION_CODES.M)
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

    switch (requestCode) {
        case REQUEST_PERMISSION_KEY: {
            if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                onToggleScreenShare();
            } else {
                isRecording = false;
                actionBtnReload();


                return;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        class MediaProjectionCallback extends MediaProjection.Callback {
            @Override
            public void onStop() {
                if (isRecording) {
                    isRecording = false;
                    actionBtnReload();
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                }
                mMediaProjection = null;
                stopScreenSharing();
            }
        }

    }
}
}















