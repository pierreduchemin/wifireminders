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

import java.util.ArrayList;
import java.util.List;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class AddReminderActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_reminder);
		// Show the Up button in the action bar.
		setupActionBar();
	}

	protected void onResume() {
		super.onResume();
		WifiManager mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		List<String> spinnerArray = new ArrayList<String>();
		// Getting list of stored networks.
		List<WifiConfiguration> wifiList = mainWifi.getConfiguredNetworks();
		for (int i = 0; i < wifiList.size(); i++) {
			// Removing quotes.
			spinnerArray.add(wifiList.get(i).SSID.toString().replaceAll(
					"^\"|\"$", ""));
		}
		// Creating adapter to populate spinnerSSID items.
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spinnerArray);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner Items = (Spinner) findViewById(R.id.spinnerSSID);
		Items.setAdapter(adapter);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add_reminder, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown.
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_save:
			// Save Rule selected, saving data to SharedPreferences.
			Spinner spinnerSSID = (Spinner) findViewById(R.id.spinnerSSID);
			String selectedSSID = spinnerSSID.getSelectedItem().toString();
			SharedPreferences sharedPrefRules = getSharedPreferences("rules",
					MODE_PRIVATE);
			// Getting number of rules.
			int rulesCount = sharedPrefRules.getInt("RulesCount", 0);
			// Saving new rule to SharedPreferences.
			SharedPreferences.Editor editor = sharedPrefRules.edit();
			EditText editRuleTitle = (EditText) findViewById(R.id.editTitle);
			EditText editReminderText = (EditText) findViewById(R.id.editReminderText);
			editor.putString("Title" + Integer.toString(rulesCount + 1),
					editRuleTitle.getText().toString());
			editor.putString("Text" + Integer.toString(rulesCount + 1),
					editReminderText.getText().toString());
			editor.putString("SSID" + Integer.toString(rulesCount + 1),
					selectedSSID);
			editor.putBoolean("Enabled" + Integer.toString(rulesCount + 1),
					true);
			editor.putInt("RulesCount", rulesCount + 1);
			editor.commit();
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}