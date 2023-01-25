package com.example.carmusic;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class CarMusicService extends android.app.Service {
        private NotificationManager mNM;

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
            //      showNotification();
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.i("CarAudioService", "Received start id " + startId + ": " + intent);
            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            // Cancel the persistent notification.
            mNM.cancel(NOTIFICATION);

            // Tell the user we stopped.
//        Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT).show();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        public void SendBroadcast() {
            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("Data", "Data");
            sendBroadcast(broadcastIntent);

        }
}
