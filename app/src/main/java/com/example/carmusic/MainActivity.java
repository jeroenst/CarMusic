package com.example.carmusic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media.session.MediaButtonReceiver;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.MediaSource;


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
    private static Context context;
    private static String errorMessage;
    private static ExoPlayer exoPlayer;

    public static void mediaControl(MediaControlAction mediaControlAction) {
        if (fileList.size() > 0) {
            // Select song to play
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
                // Set mediaIndex to the first song of the next folder
                while (currentfolder.equals(foundfolder)) {
                    if (++mediaIndex >= fileList.size()) mediaIndex = 0;
                    currentfolder = fileList.get(mediaIndex).getParent();
                    if (oldmediaindex == mediaIndex) break;
                }
            }

            if (mediaControlAction == MediaControlAction.previousfolder) {
                String currentFolder = fileList.get(mediaIndex).getParent();
                String foundFolder = currentFolder;
                int oldMediaIndex = mediaIndex;
                // Set mediaIndex to the last song of previous folder
                while (currentFolder.equals(foundFolder)) {
                    if (--mediaIndex < 0) mediaIndex = fileList.size() - 1;
                    currentFolder = fileList.get(mediaIndex).getParent();
                    if (oldMediaIndex == mediaIndex) break;
                }
                // Set mediaIndex to the last song of the folder before the previous folder
                while (currentFolder.equals(foundFolder)) {
                    if (--mediaIndex < 0) mediaIndex = fileList.size() - 1;
                    currentFolder = fileList.get(mediaIndex).getParent();
                    if (oldMediaIndex == mediaIndex) break;
                }
                // Set mediaIndex to the fist song of previous folder
                if (++mediaIndex >= fileList.size()) mediaIndex = 0;
            }


            errorMessage = "";
            try {
                if ((mediaControlAction == MediaControlAction.pause) ||
                    (exoPlayer.isPlaying() && mediaControlAction == MediaControlAction.play))
                {
                    if (exoPlayer.isPlaying()) exoPlayer.pause();
                    isPaused = true;
                    mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                }
                else if (isPaused && mediaControlAction == MediaControlAction.play){
                    exoPlayer.play();
                    isPaused = false;
                    mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                }
                else {
                    exoPlayer.stop();
                    // Build the media item.
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(fileList.get(mediaIndex).toString()));
                    // Set the media item to be played.
                    exoPlayer.setMediaItem(mediaItem);
                    // Prepare the player.
                    exoPlayer.prepare();
                    // Start the playback.
                    exoPlayer.play();
                    isPaused = false;
                    mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                }
            } catch (Exception e){
            Log.e("CarAudio", e.getMessage() != null ? e.getMessage() : "Unknown Exception");
            Log.e("CarAudio", e.getStackTrace().toString());
            errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown Exception";
            }

            // Show music details
            try {
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(fileList.get(mediaIndex).toString());
                String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                String path = new File(fileList.get(mediaIndex).getParent()).toString();
                String filename = new File(fileList.get(mediaIndex).getName()).toString();
                filename = filename.substring(filename.lastIndexOf("/") + 1);

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
                if (errorMessage != "") mTextView.setText(errorMessage);

            } catch (Exception e) {
                Log.e("CarAudio", e.getStackTrace().toString());
            }

            // Show fanart
            try {
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(fileList.get(mediaIndex).toString());
                byte[] image = metaRetriever.getEmbeddedPicture();
                if (BitmapFactory.decodeByteArray(image, 0, image.length) != null)
                    mImageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
            } catch (Exception e) {
                mImageView.setImageResource(R.drawable.speaker);
            }

            SharedPreferences sharedPref = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("mediaIndex", mediaIndex);
            editor.apply();
        }
    }

    public List<File> addFiles(List<File> files, File dir) {
        if (files == null)
            files = new LinkedList<File>();


        Boolean issymlink = false;
        try {
//            dir = dir.getCanonicalFile();
        } catch (Exception e) {
        }


        Log.d("CarMusic", "Found file:" + dir.toString());

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
        if (dirfiles != null) {
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
        context = this;

        mTextView = (TextView) findViewById(R.id.textView);
        mTextView.setText("Scanning\nFolders");

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(R.drawable.speaker);

        mButtonPlayPause = (ImageButton) findViewById(R.id.buttonPlayPause);

        dir = new File("/sdcard/Music");
        fileList = addFiles(fileList, dir);
        dir = new File("/storage/extsd/Music");
        fileList = addFiles(fileList, dir);

        SharedPreferences sharedPref = context.getSharedPreferences("preferences",Context.MODE_PRIVATE);
        mediaIndex = sharedPref.getInt("mediaIndex", 0);

        exoPlayer = new ExoPlayer.Builder(context).build();
        mediaControl(MediaControlAction.play);
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

    enum MediaControlAction {
        stop,
        play,
        pause,
        next,
        previous,
        nextfolder,
        previousfolder
    }
}

class service extends Service {

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
    public static int NOTIFICATION_ID = 1;

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

    public class LocalBinder extends Binder {
        public service getService() {
            return service.this;
        }
    }
}


