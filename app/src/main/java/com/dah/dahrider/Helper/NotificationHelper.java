package com.dah.dahrider.Helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.RequiresApi;

import com.dah.dahrider.R;

public class NotificationHelper extends ContextWrapper {

    private static final String dah_rider_Id = "com.dah.dahrider.DAH";
    private static final String dah_rider_Name = "DAH";
    private NotificationManager manager;


    public NotificationHelper(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannels();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationChannel dahChannels = new NotificationChannel(dah_rider_Id
                , dah_rider_Name
                , NotificationManager.IMPORTANCE_DEFAULT);
        dahChannels.enableLights(true);
        dahChannels.enableVibration(true);
        dahChannels.setLightColor(Color.GRAY);
        dahChannels.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(dahChannels);
    }

    public NotificationManager getManager() {
        if (manager == null)
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return manager;

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public android.app.Notification.Builder getdahNotification(String title, String content, PendingIntent contentIntent, Uri soundUri) {
        return new android.app.Notification.Builder(getApplicationContext(), dah_rider_Id)
                .setContentText(content)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_car);

    }


}
