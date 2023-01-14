package com.example.carmusic;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media.session.MediaButtonReceiver;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static List<File> fileList = null;
    private static MediaPlayer mPlayer;
    private static TextView mTextView;
    private static ImageView mImageView;
    private static ImageButton mButtonPlayPause;
    private static File dir;
    private static Boolean isPaused = false;
    private static String extension = "";
    private static Integer mediaIndex = 0;

    public List<File> addFiles(List<File> files, File dir) {
        if (files == null)
            files = new LinkedList<File>();


        Boolean issymlink = false;
        try {
//            dir = dir.getCanonicalFile();
        }
        catch (Exception e)
        {}


        Log.d("CarMusic", "Found file:"+dir.toString());

        if (!dir.isDirectory() && (!issymlink)) {
            String extension = "";
            int i = dir.toString().lastIndexOf('.');
            if (i > 0) extension = dir.toString().substring(i + 1);
            if (extension.equals("mp3") || extension.equals("flac") || extension.equals("wav") || extension.equals("ape") || extension.equals("aac") || extension.equals("ogg")) {
                files.add(dir);
            }
            return files;
        }

        File[] dirfiles = dir.listFiles();
        if (dirfiles != null)
        {
            Arrays.sort(dirfiles);
            for (File file : dirfiles) {
                addFiles(files, file);
            }
        }
        return files;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mTextView = (TextView) findViewById(R.id.textView);
        mTextView.setText("Scanning\nFolders");

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(R.drawable.speaker);

        mButtonPlayPause = (ImageButton) findViewById(R.id.buttonPlayPause);


        dir = new File("/sdcard/Music");
        fileList= addFiles(fileList, dir);
        dir = new File("/storage/extsd/Music");
        fileList= addFiles(fileList, dir);

        mediaControl(MediaControlAction.play);

        //AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);
    }


    enum MediaControlAction {
        stop,
        play,
        pause,
        next,
        previous,
        nextfolder,
        previousfolder
    }


    /**
     * Called when the user touches the on screen button
     */
    public void mediaStartPause(View view) {
        mediaControl(MediaControlAction.play);
    }

    public void mediaNext(View view) {
        mediaControl(MediaControlAction.next);
    }

    public void mediaPrevious(View view) {
        mediaControl(MediaControlAction.previous);
    }

    public void mediaNextFolder(View view) {
        mediaControl(MediaControlAction.nextfolder);
    }

    public void mediaPreviousFolder(View view) {
        mediaControl(MediaControlAction.previousfolder);
    }

    public static void mediaControl(MediaControlAction mediaControlAction) {
        if (mPlayer == null)
        {
            mPlayer = new MediaPlayer();

            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                        mTextView.setText("ERROR what:"+ String.valueOf(i) + "  extra:"+String.valueOf(i1));
                    return false;
                }
            });

            mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
                    mTextView.setText("INFO what:"+ String.valueOf(i)  +"  extra:"+String.valueOf(i1));
                    return false;
                }
            });

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d("CarAudio", "onComplete, mediaPlayer.isPlaying() returns " + mPlayer.isPlaying());
                    mediaControl(MediaControlAction.next);
                }
            });

            mPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
        }

        if (fileList.size() > 0) {
            if (mediaControlAction == MediaControlAction.next) {
                if (++mediaIndex >= fileList.size()) mediaIndex = 0;
            }

            if (mediaControlAction == MediaControlAction.previous) {
                if (--mediaIndex < 0) mediaIndex = fileList.size() - 1;
            }

            if (mediaControlAction == MediaControlAction.nextfolder) {
                String currentfolder = fileList.get(mediaIndex).getParent();
                String foundfolder = currentfolder;
                int oldmediaindex = mediaIndex;
                while (currentfolder.equals(foundfolder)) {
                    if (++mediaIndex >= fileList.size()) mediaIndex = 0;
                    currentfolder = fileList.get(mediaIndex).getParent();
                    if (oldmediaindex == mediaIndex) break;
                }
            }

            if (mediaControlAction == MediaControlAction.previousfolder) {
                String currentfolder = fileList.get(mediaIndex).getParent();
                String foundfolder = currentfolder;
                int oldmediaindex = mediaIndex;
                while (currentfolder.equals(foundfolder)) {
                    if (--mediaIndex < 0) mediaIndex = fileList.size() - 1;
                    currentfolder = fileList.get(mediaIndex).getParent();
                    if (oldmediaindex == mediaIndex) break;
                }
            }

            String errormessage = "";
            try {
             if (mPlayer.isPlaying() && (mediaControlAction == MediaControlAction.play || mediaControlAction == MediaControlAction.pause)) {
                    mPlayer.pause();
                    isPaused = true;
                    mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                } else if ((mediaControlAction != MediaControlAction.pause) && (mediaControlAction != MediaControlAction.stop)) {
                    if (!(mediaControlAction == MediaControlAction.play && isPaused))
                    {
                        mPlayer.stop();
                        mPlayer.reset();
                        mPlayer.setDataSource(fileList.get(mediaIndex).toString());
                        mPlayer.prepare();
                    }
                    isPaused = false;
                    mPlayer.start();
                    mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                } else if (mediaControlAction == MediaControlAction.stop)
                {
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                }
            } catch (Exception e) {
                Log.e("CarAudio", e.getMessage() != null ? e.getMessage() : "Unknown Exception");
                Log.e("CarAudio", e.getStackTrace().toString());
            }

            try {
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(fileList.get(mediaIndex).toString());
                String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                String path = new File(fileList.get(mediaIndex).getParent()).toString();
                String filename = new File(fileList.get(mediaIndex).getName()).toString();
                filename=filename.substring(filename.lastIndexOf("/")+1);

                path = path.replace("/sdcard/Music", "");
                path = path.replace("/storage/extsd/Music", "");
                path = path.replace("/", "");

                if (path == null) path = "";
                String bitrate = String.valueOf(Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)) / 1000);
                int i = fileList.get(mediaIndex).toString().lastIndexOf('.');
                if (i > 0) extension = fileList.get(mediaIndex).toString().substring(i + 1);
                if (artist == null) artist = "";
                if (title == null) title = filename;
                if (album == null) album = "";
                mTextView.setText(artist + "\n" + title + "\n" + album + "\n" + path + "\n" + bitrate + "kbps " + extension);
                if (errormessage != "") mTextView.setText(errormessage);

            } catch (Exception e) {
                Log.e("CarAudio", e.getStackTrace().toString());
            }
            try {
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(fileList.get(mediaIndex).toString());
                byte[] image = metaRetriever.getEmbeddedPicture();
                if (BitmapFactory.decodeByteArray(image, 0, image.length) != null)
                    mImageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            } catch (Exception e) {
                mImageView.setImageResource(R.drawable.speaker);
            }

        }
    }

}

class service extends Service {

    public class LocalBinder extends Binder {
        public service getService() {
            return service.this;
        }
    }


    public static int NOTIFICATION_ID = 1;
    public static final String CHANNEL_ID = "flutter_media_notification";
    public static final String MEDIA_SESSION_TAG = "flutter_media_notification";

    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_PLAY_PAUSE = "PLAY_PAUSE";
    public static final String ACTION_REWIND = "REWIND";
    public static final String ACTION_FAST_FORWARD = "FAST_FORWARD";

    private static final long ALWAYS_AVAILABLE_PLAYBACK_ACTIONS
            = PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_REWIND
            | PlaybackStateCompat.ACTION_FAST_FORWARD
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_STOP;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("CarAudio", "onStartCommand() " + intent);
        MediaSessionCompat mediaSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG);
        if (intent != null) {
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                // `MediaButtonReceiver` may use `Context.startForegroundService(Intent)`,
                // so we *have* to call `startForeground(...)`
                // or the app will crash during service shutdown

                //setForegroundAndNotification(true);
            }

            MediaButtonReceiver.handleIntent(mediaSession, intent);

            String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                MainActivity.mediaControl(MainActivity.MediaControlAction.play);
            } else if (ACTION_PAUSE.equals(action)) {
                MainActivity.mediaControl(MainActivity.MediaControlAction.pause);
            } else if (ACTION_PLAY_PAUSE.equals(action)) {
                MainActivity.mediaControl(MainActivity.MediaControlAction.play);
            } else if (ACTION_REWIND.equals(action)) {
                MainActivity.mediaControl(MainActivity.MediaControlAction.previous);
            } else if (ACTION_FAST_FORWARD.equals(action)) {
                MainActivity.mediaControl(MainActivity.MediaControlAction.next);
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


