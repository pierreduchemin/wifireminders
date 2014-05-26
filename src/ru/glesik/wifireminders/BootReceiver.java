package ru.glesik.wifireminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
	public BootReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			SharedPreferences sharedPrefSettings = PreferenceManager.getDefaultSharedPreferences(context);
			String intervalString = sharedPrefSettings.getString("prefInterval", "60000");
			int interval = Integer.parseInt(intervalString);
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
