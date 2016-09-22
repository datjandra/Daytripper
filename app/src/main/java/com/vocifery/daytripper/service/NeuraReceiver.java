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
import com.vocifery.daytripper.ui.MainActivity;
import com.vocifery.daytripper.util.NeuraUtils;

public class NeuraReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "NeuraReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String eventName = intent.getStringExtra(NeuraConsts.EXTRA_EVENT_NAME);
        if (action.equalsIgnoreCase(NeuraConsts.ACTION_NEURA_EVENT)) {
            makeRobotRequest(context, eventName);
        } else if (action.equalsIgnoreCase(ResponderService.ROBOT_ACTION)) {
            handleRobotResponse(context, intent);
        }
    }

    private void makeRobotRequest(Context context, String eventName) {
        String eventDetails = NeuraUtils.getEventDetails(context, eventName);
        Intent chatbotIntent = new Intent(context, ResponderService.class);
        chatbotIntent.setAction(ResponderService.ROBOT_ACTION);
        chatbotIntent.putExtra(ResponderService.KEY_QUERY, eventDetails);
        context.startService(chatbotIntent);
    }

    private void handleRobotResponse(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Received response from " + action);
        String text = intent.getStringExtra(ResponderService.EXTRA_TEXT_MESSAGE);
        String url = intent.getStringExtra(ResponderService.EXTRA_URL_MESSAGE);
        String content = intent.getStringExtra(ResponderService.EXTRA_CONTENT_MESSAGE);

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setAction(action);
        resultIntent.putExtra(ResponderService.EXTRA_TEXT_MESSAGE, text);

        if (!TextUtils.isEmpty(url)) {
            resultIntent.putExtra(ResponderService.EXTRA_URL_MESSAGE, url);
        }

        if (!TextUtils.isEmpty(content)) {
            resultIntent.putExtra(ResponderService.EXTRA_CONTENT_MESSAGE, content);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(text)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Activity.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
