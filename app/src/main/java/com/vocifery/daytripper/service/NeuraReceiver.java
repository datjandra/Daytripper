package com.vocifery.daytripper.service;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.neura.sdk.config.NeuraConsts;
import com.vocifery.daytripper.R;
import com.vocifery.daytripper.ui.Daytripper;
import com.vocifery.daytripper.ui.MainActivity;

public class NeuraReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "NeuraReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String eventName = intent.getStringExtra(NeuraConsts.EXTRA_EVENT_NAME);

        if (action.equalsIgnoreCase(NeuraConsts.ACTION_NEURA_EVENT)) {
            receivedNeuraEvent(context, intent, eventName);
        } else if (action.equalsIgnoreCase(ResponderService.ROBOT_ACTION)) {
            Log.i(TAG, "Received response from " + action);
            String message = intent.getStringExtra(ResponderService.EXTRA_NO_OP_MESSAGE);
            String url = intent.getStringExtra(ResponderService.EXTRA_URL_MESSAGE);
            Intent resultIntent = new Intent(context, MainActivity.class);
            resultIntent.setAction(action);
            resultIntent.putExtra(ResponderService.EXTRA_NO_OP_MESSAGE, message);

            if (!TextUtils.isEmpty(url)) {
                resultIntent.putExtra(ResponderService.EXTRA_URL_MESSAGE, url);
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    0,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(message)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void receivedNeuraEvent(Context context, Intent intent, String eventName) {
        Daytripper daytripper = (Daytripper) Daytripper.getAppContext();
        String eventDetails = daytripper.getNeuraEventDetails();
        Intent chatbotIntent = new Intent(context, ResponderService.class);
        chatbotIntent.setAction(ResponderService.ROBOT_ACTION);
        chatbotIntent.putExtra(ResponderService.KEY_QUERY, eventDetails);
        context.startService(chatbotIntent);
    }
}
