package com.example.carmusic;

import android.app.Application;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener  {

    private TextView mTextView;
    private ImageView mImageView;
    private Button mButtonPlayPause;
    private Context context;
    private String errorMessage;
    private ExoPlayer mExoPlayer = null;
    private CarMusicService mCarAudioService = null;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private String TAG = "CarMusicActivity";
    private boolean updateImage = true;
    private Long mediaPosition = 0L;
    private Long mediaDuration = 0L;

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.isShiftPressed() || event.isShiftPressed() || event.isCtrlPressed() || event.isFunctionPressed() || event.isLongPress()
        || event.getDownTime() != event.getEventTime())
        {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)
            {
                mCarAudioService.mediaControl(CarMusicService.MediaControlAction.nextfolder);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            {
                mCarAudioService.mediaControl(CarMusicService.MediaControlAction.previousfolder);
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)
        {
            mCarAudioService.mediaControl(CarMusicService.MediaControlAction.next);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        {
            mCarAudioService.mediaControl(CarMusicService.MediaControlAction.previous);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY)
        {
            mCarAudioService.mediaControl(CarMusicService.MediaControlAction.play);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)
        {
            mCarAudioService.mediaControl(CarMusicService.MediaControlAction.pause);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }


    /**
     * Create our connection to the service to be used in our bindService call.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            //We expect the service binder to be the main services binder.
            //As such we cast.
            if (service instanceof CarMusicService.MainServiceBinder) {
                final CarMusicService.MainServiceBinder myService = (CarMusicService.MainServiceBinder) service;
                //Then we simply set the exoplayer instance on this view.
                mExoPlayer = myService.getExoPlayerInstance();
                //Then we simply set the exoplayer instance on this view.
                mCarAudioService = myService.getInstance();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        context = this;

        Intent intent = new Intent(this, CarMusicService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);


        mTextView = (TextView) findViewById(R.id.textView);
        mTextView.setText("Scanning\nFolders");

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(R.drawable.speaker);

        mButtonPlayPause = (Button) findViewById(R.id.buttonPlayPause);



    }

    /**
     * Called when the user touches the on screen button
     */
    public void mediaStartPause(View view) {
        mCarAudioService.mediaControl(CarMusicService.MediaControlAction.play);
    }

    public void mediaNext(View view) {
        mCarAudioService.mediaControl(CarMusicService.MediaControlAction.next);
    }

    public void mediaPrevious(View view) {
        mCarAudioService.mediaControl(CarMusicService.MediaControlAction.previous);
    }

    public void mediaNextFolder(View view) {
        mCarAudioService.mediaControl(CarMusicService.MediaControlAction.nextfolder);
        }

    public void mediaPreviousFolder(View view) {
        mCarAudioService.mediaControl(CarMusicService.MediaControlAction.previousfolder);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onPause(){
        mCarAudioService.saveState();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
        unbindService(mConnection);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
        Intent intent = new Intent(this, CarMusicService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // For now we poll de service every 100ms. Later we can use a callback or receive
        // a broadcast message
        new CountDownTimer(Long.MAX_VALUE, 100) {

            public void onTick(long millisUntilFinished) {
                updateTime();
            }

            public void onFinish() {
            }

        }.start();

    }

    public void updateTime() {
        if ((mCarAudioService != null) && (mExoPlayer != null)) {
            if (mCarAudioService.getIsPaused()) mButtonPlayPause.setText("\uf04b");
            else mButtonPlayPause.setText("\uf04c");
            if ((mediaPosition != mExoPlayer.getCurrentPosition()) || (mediaDuration != mExoPlayer.getDuration())) {
                if (mExoPlayer.getDuration() > 0) {
                    long mediaPosition = mExoPlayer.getCurrentPosition();
                    long mediaDuration = mExoPlayer.getDuration();

                    if (mediaPosition == 0) {
                        updateImage = true;
                    }

                    long mediaPositionHour = mediaPosition / 3600000;
                    long mediaPositionMin = (mediaPosition / 60000) % 60;
                    long mediaPositionSec = (mediaPosition / 1000) % 60;

                    String mediaPositionSecStr = (mediaPositionSec < 10 ? "0" : "") + mediaPositionSec;
                    String mediaPositionMinStr = (mediaPositionMin < 10 ? "0" : "") + mediaPositionMin;

                    long mediaDurationHour = mediaDuration / 3600000;
                    long mediaDurationMin = (mediaDuration / 60000) % 60;
                    long mediaDurationSec = (mediaDuration / 1000) % 60;

                    String mediaDurationSecStr = (mediaDurationSec < 10 ? "0" : "") + mediaDurationSec;
                    String mediaDurationMinStr = (mediaDurationMin < 10 ? "0" : "") + mediaDurationMin;

                    String time = (mediaPositionHour > 0 ? mediaPositionHour + ":" + mediaPositionMinStr : "" + mediaPositionMin) + ":" + mediaPositionSecStr;
                    time += " / " + (mediaDurationHour > 0 ? mediaDurationHour + ":" + mediaDurationMinStr : "" + mediaDurationMin) + ":" + mediaDurationSecStr;
                    String mediaInfo = mCarAudioService.getMediaInfo() + time;
                    mTextView.setText(mediaInfo);
                    if (updateImage) {
                        try {
                            byte[] image = mCarAudioService.getImage();
                            if (BitmapFactory.decodeByteArray(image, 0, image.length) != null) {
                                mImageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
                            }
                        } catch (Exception e) {
                            mImageView.setImageResource(R.drawable.speaker);
                        }

                    }
                } else {
                    mTextView.setText(mCarAudioService.getMediaInfo());
                }
            }
        }
    }
}


