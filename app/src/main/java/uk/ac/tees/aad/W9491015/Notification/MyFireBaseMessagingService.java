package uk.ac.tees.aad.W9491015.Notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import uk.ac.tees.aad.W9491015.R;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFireBaseMessagingService extends FirebaseMessagingService {
    String title,message;
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        title=remoteMessage.getData().get("Title");
        message=remoteMessage.getData().get("Message");

//        NotificationCompat.Builder builder =
//                new NotificationCompat.Builder(getApplicationContext())
//                        .setSmallIcon(R.drawable.logo)
//                        .setContentTitle(title)
//                        .setContentText(message);
//        NotificationManager manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
//        manager.notify(0, builder.build());

        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= 26)
        {
            //When sdk version is larger than26
            String id = "fcm_default_channel";
            String description = "143";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(id, description, importance);
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(getApplicationContext(), id)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(message)
                    //.setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();
            manager.notify(1, notification);
        }
        else
        {
            //When sdk version is less than26
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(title)
                    .setContentText(message)
                    //.setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .build();
            manager.notify(1,notification);
        }
    }

}
