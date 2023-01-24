package com.example.carmusic;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

public class MainActivity extends AppCompatActivity implements Player.Listener {

    private static List<File> fileList = null;
    private static TextView mTextView;
    private static ImageView mImageView;
    private static ImageButton mButtonPlayPause;
    private static File dir;
    private static Boolean isPaused = false;
    private static String extension = "";
    private static Integer mediaIndex = 0;
    private static Long mediaPosition = 0L;
    private static Context context;
    private static String errorMessage;
    private static ExoPlayer mExoPlayer = null;
    private static String mediaInfo = "";

/*    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)
        {
            mediaControl(MediaControlAction.nextfolder);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        {
            mediaControl(MediaControlAction.previousfolder);
            return true;
        }
        return false;
    }
*/

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.isShiftPressed() || event.isShiftPressed() || event.isCtrlPressed() || event.isFunctionPressed() || event.isLongPress()
        || event.getDownTime() != event.getEventTime())
        {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)
            {
                mediaControl(MediaControlAction.nextfolder);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            {
                mediaControl(MediaControlAction.previousfolder);
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)
        {
            mediaControl(MediaControlAction.next);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        {
            mediaControl(MediaControlAction.previous);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY)
        {
            mediaControl(MediaControlAction.play);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE)
        {
            mediaControl(MediaControlAction.pause);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }


        public void mediaControl(MediaControlAction mediaControlAction) {
        if (fileList.size() > 0) {
            // Select song to play
            if (mediaControlAction == MediaControlAction.next) {
                if (++mediaIndex >= fileList.size()) mediaIndex = 0;
                mediaPosition = 0L;
            }

            if (mediaControlAction == MediaControlAction.previous) {
                if (--mediaIndex < 0) mediaIndex = fileList.size() - 1;
                mediaPosition = 0L;
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
                mediaPosition = 0L;
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
                foundFolder = currentFolder;
                // Set mediaIndex to the last song of the folder before the previous folder
                while (currentFolder.equals(foundFolder)) {
                    if (--mediaIndex < 0) mediaIndex = fileList.size() - 1;
                    currentFolder = fileList.get(mediaIndex).getParent();
                    if (oldMediaIndex == mediaIndex) break;
                }
                // Set mediaIndex to the fist song of previous folder
                if (++mediaIndex >= fileList.size()) mediaIndex = 0;
                mediaPosition = 0L;
            }


            errorMessage = "";
            try {
                if ((mediaControlAction == MediaControlAction.pause) ||
                    (mExoPlayer.isPlaying() && mediaControlAction == MediaControlAction.play))
                {
                    if (mExoPlayer.isPlaying()) mExoPlayer.pause();
                    isPaused = true;
                    mButtonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                }
                else if (isPaused && mediaControlAction == MediaControlAction.play){
                    // If player was restarted go to latest position
                    if (mediaPosition > 0L) mExoPlayer.seekTo(mediaPosition);
                    mediaPosition = 0L;
                    mExoPlayer.play();
                    isPaused = false;
                    mButtonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                }
                else {
                    mExoPlayer.stop();
                    // Build the media item.
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(fileList.get(mediaIndex).toString()));
                    // Set the media item to be played.
                    mExoPlayer.setMediaItem(mediaItem);
                    // Prepare the player.
                    mExoPlayer.prepare();
                    // If player was restarted go to latest position
                    if (mediaPosition > 0L) mExoPlayer.seekTo(mediaPosition);
                    mediaPosition = 0L;
                    // Start the playback.
                    mExoPlayer.play();
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
                if (i > 0) extension = fileList.get(mediaIndex).toString().substring(i + 1).toUpperCase();
                if (artist == null) artist = "";
                if (title == null) title = filename;
                if (album == null) album = "";
                mediaInfo = artist + "\n" + title + "\n" + album + "\n" + path + "\n" + bitrate + "kbps " + extension + "\n";
                updateTime();

                Intent intent = new Intent();
                intent.setAction("com.android.music.metachanged");
                intent.putExtra("track", title);
                intent.putExtra("artist", artist);
                intent.putExtra("album", album);
                intent.putExtra("playing", true);
                intent.putExtra("package", "com.example.caraudio");
                sendBroadcast(intent);
            } catch (Exception e) {
                Log.e("CarAudio", e.getStackTrace().toString());
                mediaInfo = "File error";
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
            editor.putLong("mediaPosition", mExoPlayer.getCurrentPosition());
            editor.apply();
        }
    }

    public List<File> addFiles(List<File> files, File dir) {
        if (files == null)
            files = new LinkedList<File>();


        if (!dir.isDirectory()) {
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
        mediaPosition = sharedPref.getLong("mediaPosition", 0);

        if (mExoPlayer == null)
        {
            mExoPlayer = new ExoPlayer.Builder(context).build();
            mediaControl(MediaControlAction.play);
        }


        mExoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int state) {
                if (state == Player.STATE_ENDED) {
                        mediaControl(MediaControlAction.next);
                }
            }
        });


        new CountDownTimer(Long.MAX_VALUE, 1000) {

            public void onTick(long millisUntilFinished) {
                updateTime();
                // For now save state every 10 seconds until we find a option to save on destroy
                if ((millisUntilFinished/1000) % 10 == 0) saveState();
            }

            public void onFinish() {
            }

        }.start();
    }


    public void updateTime() {
        if (mExoPlayer.getDuration() > 0) {
            mediaPosition = mExoPlayer.getCurrentPosition();
            Long mediaDuration = mExoPlayer.getDuration();

            Long mediaPositionHour = mediaPosition / 3600000;
            Long mediaPositionMin = (mediaPosition / 60000) % 60;
            Long mediaPositionSec = (mediaPosition / 1000) % 60;

            String mediaPositionSecStr = (mediaPositionSec < 10 ? "0" : "") + mediaPositionSec;
            String mediaPositionMinStr = (mediaPositionMin < 10 ? "0" : "") + mediaPositionMin;

            Long mediaDurationHour = mediaDuration / 3600000;
            Long mediaDurationMin = (mediaDuration / 60000) % 60;
            Long mediaDurationSec = (mediaDuration /1000) % 60;

            String mediaDurationSecStr = (mediaDurationSec < 10 ? "0" : "") + mediaDurationSec;
            String mediaDurationMinStr = (mediaDurationMin < 10 ? "0" : "") + mediaDurationMin;

            String time = (mediaPositionHour > 0 ? mediaPositionHour + ":" + mediaPositionMinStr : ""+ mediaPositionMin) + ":" + mediaPositionSecStr;
            time += " / " + (mediaDurationHour  > 0 ? mediaDurationHour + ":" + mediaDurationMinStr : "" + mediaDurationMin)  + ":" + mediaDurationSecStr;
            mTextView.setText(mediaInfo + time);
        } else
        {
            mediaPosition = 0L;
            mTextView.setText(mediaInfo + " ");
        }
    }

    public void saveState()
    {
        SharedPreferences sharedPref = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("mediaIndex", mediaIndex);
        editor.putLong("mediaPosition", mediaPosition);
        editor.apply();
        editor.commit();
    }

    @Override
    protected void onPause(){
        saveState();
        super.onPause();
    }

    @Override
    protected void onStop(){
        saveState();
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        saveState();
        super.onDestroy();
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

