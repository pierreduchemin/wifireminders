/*
 * This file is part of Wi-Fi Reminders.
 * 
 * Wi-Fi Reminders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Wi-Fi Reminders is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Wi-Fi Reminders.  If not, see <http://www.gnu.org/licenses/>.
 */

package ru.glesik.wifireminders;

import java.util.List;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends Service {
	public AlarmService() {
	}

	BroadcastReceiver scanReceiver;

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("none");
	}

	@Override
	public void onCreate() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		scanReceiver = new BroadcastReceiver() {
			public void onReceive(Context c, Intent i) {
				// Scan results are available.
				WifiManager w = (WifiManager) c
						.getSystemService(Context.WIFI_SERVICE);
				// Handle scan results.
				scanResultHandler(w.getScanResults());
			}
		};
		registerReceiver(scanReceiver, intentFilter);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (wifiManager.isWifiEnabled()) {
			Bundle extras = intent.getExtras();
			if (extras == null) { // Service started from AlarmManager - polling.
				// Start networks scan.
				wifiManager.startScan();
			} else { // Service started from BroadcastReceiver - Wi-Fi connected.
				// Check for rules with connected SSID.
				String SSID = (String) extras.get("SSID");
				Log.i("AlarmService", "received SSID " + SSID);
				checkNetworks(SSID);
			}
		} else {
			// Nothing to do.
			stopSelf();
		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(scanReceiver);
	}

	public void scanResultHandler(List<ScanResult> wifiList) {
		SharedPreferences sharedPreferences = this.getSharedPreferences(
				"rules", Context.MODE_PRIVATE);
		int rulesCount = sharedPreferences.getInt("RulesCount", 0);
		for (int j = 0; j < wifiList.size(); j++) {
			checkNetworks(wifiList.get(j).SSID);
		}
		// Check if there are still enabled reminders.
		boolean activeExist = false;
		for (int i = 1; i <= rulesCount; i++) {
			if (sharedPreferences.getBoolean("Enabled"
					+ Integer.toString(i), false)) {
				activeExist = true;
			}
		}
		if (!activeExist) {
			// Cancel all existing alarms.
			stopAlarm();
		}
		stopSelf();
	}
	
	public void checkNetworks(String SSID) {
		SharedPreferences sharedPreferences = this.getSharedPreferences(
				"rules", Context.MODE_PRIVATE);
		int rulesCount = sharedPreferences.getInt("RulesCount", 0);
		for (int k = 1; k <= rulesCount; k++) {
			boolean currentEnabled = sharedPreferences.getBoolean("Enabled"
					+ Integer.toString(k), false);
			if (currentEnabled) {
				String currentSSID = sharedPreferences.getString("SSID"
						+ Integer.toString(k), "error");
				if (currentSSID.equals(SSID)) {
					String currentTitle = sharedPreferences.getString(
							"Title" + Integer.toString(k), "error");
					String currentText = sharedPreferences.getString("Text"
							+ Integer.toString(k), "error");
					// Show reminder alert, vibrate and play sound.
					showReminder(currentTitle + " (" + currentSSID + ")",
							currentText);
					// Disable this reminder so that reminders don't pile
					// up.
					SharedPreferences.Editor editor = sharedPreferences
							.edit();
					editor.putBoolean("Enabled" + Integer.toString(k),
							false);
					editor.commit();
				}
			}
		}
	}

	public void stopAlarm() {
		AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(this, AlarmReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intent, 0);
		alarmManager.cancel(pendingIntent);
		// Disable boot receiver
		ComponentName receiver = new ComponentName(this, BootReceiver.class);
		PackageManager pm = this.getPackageManager();
		pm.setComponentEnabledSetting(receiver,
		        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
		        PackageManager.DONT_KILL_APP);
	}

	public void showReminder(String title, String text) {
		SharedPreferences sharedPrefSettings = PreferenceManager.getDefaultSharedPreferences(this);
		boolean vibrate = sharedPrefSettings.getBoolean("prefVibrate", true);
		String sound = sharedPrefSettings.getString("prefRingtone", "default");
		Uri soundURI = Uri.parse(sound);
		NotificationManager mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
		int icon;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			// Icon for Android 3.0+.
			icon = R.drawable.notify;
		} else {
			// Icon for Android 2.3 and lower.
			icon = R.drawable.notify_legacy;
		}
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this)
				.setSmallIcon(icon)
				.setContentTitle(title)
				.setContentText(text)
				//.setVibrate(pattern)
				.setSound(soundURI)
				.setDefaults(
//						Notification.DEFAULT_SOUND
//								| Notification.DEFAULT_VIBRATE
								Notification.DEFAULT_LIGHTS)
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setOnlyAlertOnce(true);
		Intent ni = new Intent(this, RemindersListActivity.class);
		ni.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, ni, 0);
		mBuilder.setContentIntent(pi);
		mBuilder.setAutoCancel(true);
		Notification notification = mBuilder.build();
		if (vibrate) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		// Displaying notification with random id, in case there will be more.
		mNotificationManager.notify((int) (Math.random() * ((999) + 1)),
				notification);
	}
}
