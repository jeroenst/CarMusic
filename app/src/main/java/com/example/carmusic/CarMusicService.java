package com.example.carmusic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.support.v4.media.session.MediaSessionCompat;

import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;



public class CarMusicService extends android.app.Service {
    enum MediaControlAction {
        stop,
        play,
        pause,
        next,
        previous,
        nextfolder,
        previousfolder
    }


    private PlaybackStateCompat playbackState;
    private MediaSessionConnector mediaSessionConnector;
    private MediaSessionCallbackCompat mediaSessionCallbackCompat;
    private MediaSessionCompat mediaSession;
    private NotificationManager mNM;
    private List<File> fileList = null;
    private ExoPlayer mExoPlayer = null;
    private File dir;
    private Boolean isPaused = false;
    private String mediaInfo = "";
    private String extension = "";
    private String errorMessage = "";
    private Integer mediaIndex = 0;
    private Long mediaPosition = 0L;
    private Context context;
    private byte[] image = null;


    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = 483838;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        CarMusicService getService() {
            return CarMusicService.this;
        }
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Display a notification about us starting.  We put an icon in the status bar.
          //    showNotification();

        context = this;

        boolean startPlaying = false;
        if (mExoPlayer == null) {
            mExoPlayer = new ExoPlayer.Builder(this).build();
            startPlaying = true;
        }

        dir = new File("/sdcard/Music");
        fileList = addFiles(fileList, dir);
        dir = new File("/storage/extsd/Music");
        fileList = addFiles(fileList, dir);

        mediaSession = new MediaSessionCompat(this, "CarMusic");
        mediaSessionCallbackCompat = new MediaSessionCallbackCompat();

        playbackState = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_FAST_FORWARD |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_REWIND)
  //              .setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1)
                .build();

        mediaSession.setPlaybackState(playbackState);
        mediaSession.setCallback(mediaSessionCallbackCompat);
        mediaSession.setActive(true);

//        mediaSessionConnector = new MediaSessionConnector(mediaSession);
        //mediaSessionConnector.setPlayer(mExoPlayer);


        SharedPreferences sharedPref = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        mediaIndex = sharedPref.getInt("mediaIndex", 0);
        mediaPosition = sharedPref.getLong("mediaPosition", 0);

        if (startPlaying) mediaControl(CarMusicService.MediaControlAction.play);

        mExoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(@Player.State int state) {
                if (state == Player.STATE_ENDED) {
                    mediaControl(CarMusicService.MediaControlAction.next);
                }
            }
        });

        new CountDownTimer(Long.MAX_VALUE, 10000) {

            public void onTick(long millisUntilFinished) {
                // For now save state every 10 seconds until we find a option to save on destroy
                if ((millisUntilFinished / 1000) % 10 == 0) saveState();
            }

            public void onFinish() {
            }

        }.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("CarAudioService", "onStartCommand() id " + startId + ": " + intent);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.media");
        filter.addAction("android.intent.action.MEDIA_BUTTON");
        registerReceiver(this.broadcastReceiver, filter);

        MediaButtonReceiver.handleIntent(mediaSession, intent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("CarAudioService", "onBind() intent " + intent);
        return new MainServiceBinder();
    }

    public List<File> addFiles(List<File> files, File dir) {
        if (files == null)
            files = new LinkedList<>();

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

    public void mediaControl(CarMusicService.MediaControlAction mediaControlAction) {
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
                if (currentfolder != null) while (currentfolder.equals(foundfolder)) {
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
                if (currentFolder != null) while (currentFolder.equals(foundFolder)) {
                    if (--mediaIndex < 0) mediaIndex = fileList.size() - 1;
                    currentFolder = fileList.get(mediaIndex).getParent();
                    if (oldMediaIndex == mediaIndex) break;
                }
                foundFolder = currentFolder;
                // Set mediaIndex to the last song of the folder before the previous folder
                if (currentFolder != null) while (currentFolder.equals(foundFolder)) {
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
                        (mExoPlayer.isPlaying() && mediaControlAction == MediaControlAction.play)) {
                    if (mExoPlayer.isPlaying()) mExoPlayer.pause();
                    isPaused = true;
                } else if (isPaused && mediaControlAction == MediaControlAction.play) {
                    // If player was restarted go to latest position
                    if (mediaPosition > 0L) mExoPlayer.seekTo(mediaPosition);
                    mediaPosition = 0L;
                    mExoPlayer.play();
                    isPaused = false;
                } else {
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
                }
            } catch (Exception e) {
                Log.e("CarAudio", e.getMessage() != null ? e.getMessage() : "Unknown Exception");
                errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown Exception";
            }

            // Show music details
            try {
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(fileList.get(mediaIndex).toString());
                String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                String path = "";
                String filename = "";

                File file = fileList.get(mediaIndex);
                if (file != null) {

                    path = file.getParent();
                    filename = file.getName();
                }

                filename = filename.substring(filename.lastIndexOf("/") + 1);
                path = path.replace("/sdcard/Music", "");
                path = path.replace("/storage/extsd/Music", "");
                path = path.replace("/", "");

                String bitrate = String.valueOf(Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)) / 1000);
                int i = fileList.get(mediaIndex).toString().lastIndexOf('.');
                if (i > 0)
                    extension = fileList.get(mediaIndex).toString().substring(i + 1).toUpperCase();
                if (artist == null) artist = "";
                if (title == null) title = filename;
                if (album == null) album = "";
                mediaInfo = artist + "\n" + title + "\n" + album + "\n" + path + "\n" + bitrate + "kbps " + extension + "\n";

                Intent intent = new Intent();
                intent.setAction("com.android.music.metachanged");
                intent.putExtra("track", title);
                intent.putExtra("artist", artist);
                intent.putExtra("album", album);
                intent.putExtra("playing", true);
                intent.putExtra("package", "com.example.caraudio");
                sendBroadcast(intent);
            } catch (Exception e) {
                mediaInfo = "File error";
            }

            // Get fanart image
            try {
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(fileList.get(mediaIndex).toString());
                image = metaRetriever.getEmbeddedPicture();
            } catch (Exception e) {
            }

            saveState();
        }
    }

    public void saveState() {
        SharedPreferences sharedPref = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("mediaIndex", mediaIndex);
        editor.putLong("mediaPosition", mediaPosition);
        editor.apply();
        editor.commit();
    }

    @Override
    public void onDestroy() {
        Intent in = new Intent();
        in.setAction("YouWillNeverKillMe");
        sendBroadcast(in);
        Log.d("CarMusicService", "onDestroy()...");
        saveState();
    }

    private class MediaSessionCallbackCompat extends MediaSessionCompat.Callback {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.d("MyLog", "executing onMediaButtonEvent: " + mediaButtonEvent.getAction());
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPlay() {
            mediaControl(MediaControlAction.play);
        }

        @Override
        public void onPause() {
            mediaControl(MediaControlAction.pause);
        }

        @Override
        public void onSkipToPrevious() {
            mediaControl(MediaControlAction.previous);
        }

        @Override
        public void onSkipToNext() {
            mediaControl(MediaControlAction.next);
        }

        @Override
        public void onFastForward() {
            mediaControl(MediaControlAction.nextfolder);
        }

        @Override
        public void onRewind() {
            mediaControl(MediaControlAction.previousfolder);
        }

        @Override
        public void onSeekTo(long pos) {
            mExoPlayer.seekTo(pos);
        }
    }

    /**
     * This class will be what is returned when an activity binds to this service.
     * The activity will also use this to know what it can get from our service to know
     * about the video playback.
     */
    class MainServiceBinder extends Binder {

        /**
         * This method should be used only for setting the exoplayer instance.
         * If exoplayer's internal are altered or accessed we can not guarantee
         * things will work correctly.
         */
        ExoPlayer getExoPlayerInstance() {
            return mExoPlayer;
        }

        CarMusicService getInstance() {
            return getCarMusicInstance();
        }
    }

    private CarMusicService getCarMusicInstance() {
        return this;
    }

    public byte[] getImage() {
        return image;
    }

    public String getMediaInfo() {
        return mediaInfo;
    }

    public Boolean getIsPaused() {
        return isPaused;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("CarMusicService", "message = " + intent.getAction());

            if ("android.media.VOLUME_CHANGED_ACTION".equals(intent.getAction())) {
                int volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE",0);
                Log.i("CarMusicService", "volume = " + volume);
            }
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                int command = intent.getIntExtra("android.intent.action.MEDIA_BUTTON",0);
                Log.i("CarMusicService", "command = " + command);
            }
            Log.i ("CarMusicService", "Received Broadcast:" + intent.getAction());
        }
    };
}

