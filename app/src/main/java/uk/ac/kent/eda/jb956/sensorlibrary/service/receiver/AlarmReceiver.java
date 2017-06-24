package uk.ac.kent.eda.jb956.sensorlibrary.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.ac.kent.eda.jb956.sensorlibrary.service.WifiService;

/**
 * Copyright (c) 2017, Jon Baker <Jonty800@gmail.com>
 * School of Engineering and Digital Arts, University of Kent
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final Intent myService = new Intent(context.getApplicationContext(), WifiService.class);
        myService.putExtra("code", WifiService.MSG_HEARTBEAT);
        if (!WifiService.uploading)
            context.stopService(myService);
        context.startService(myService);

        /*
        NetworkManager.getInstance().samsungFixCount++;
        if (NetworkManager.getInstance().samsungFixCount >= 60) { //10 mins
            NetworkManager.getInstance().samsungFixCount = 0;
            final Intent myService2 = new Intent(context.getApplicationContext(), WifiService.class);
            myService2.putExtra("code", WifiService.MSG_SENDING);
            context.startService(myService2);
            //myService2.putExtra("code", MainService.MSG_AUDIO);
            //context.startService(myService2);
            // SocialSenseApplication.getInstance().sendMessage(MainService.MSG_SENDING);
            //Intent sendingService = new Intent(context.getApplicationContext(), SendingService.class);
            // try {
            // context.stopService(sendingService);
            // context.startService(sendingService);
            //} catch (Exception e) {
            //   e.printStackTrace();
            //}
        }*/
    }
}
