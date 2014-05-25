package ru.glesik.wifireminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
	public BootReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			SharedPreferences sharedPrefSettings = context.getSharedPreferences("settings", 0);
			int interval = sharedPrefSettings.getInt("Interval", 10000);
			AlarmManager am = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			Intent i = new Intent(context, AlarmReceiver.class);
			PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
			am.cancel(pi);
			am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(),
					interval, pi);
        }
	}


}
