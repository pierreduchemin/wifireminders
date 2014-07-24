package ru.glesik.wifireminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WifiReceiver extends BroadcastReceiver {
	public WifiReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
	    if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
	    	NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
	    	NetworkInfo.State state = netInfo.getState();
	    	if (state == NetworkInfo.State.CONNECTED) {
	    		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	    		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	    		String SSID = wifiInfo.getSSID().toString().replaceAll("^\"|\"$", "");
	    		// Starting service, to be sure we're not killed too soon.
	    		Intent serviceIntent = new Intent(context, AlarmService.class);
	    		serviceIntent.putExtra("SSID", SSID);
	    		context.startService(serviceIntent);
	    	}
	    }
	}
}
