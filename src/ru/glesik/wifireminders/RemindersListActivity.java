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

/**
 * On 'Add' button press AddReminderActivity is launched. When new reminder is added,
 * we go back and update reminders list. If there are active reminders,
 * the AlarmManager is started which fires AlarmReciever periodically. AlarmReciever
 * starts AlarmService which checks for any of available networks match the reminders.
 * If yes, the notification is shown and active rules check is repeated. If there are no
 * active reminders, the AlarmManager is stopped.
 * 
 * TODO: Restart after reboot.
 * TODO: Add settings (sound, update frequency).
 * TODO: Pass active SSIDs with AlarmManager to minimize settings reads.
 */

package ru.glesik.wifireminders;

import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class RemindersListActivity extends Activity {

	private static Context context;

	public static Context getAppContext() {
		return RemindersListActivity.context;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reminders_list);
		// Keep context for external use (AlarmManager).
		RemindersListActivity.context = getApplicationContext();
		// Create adapter for remindersListView.
		ListView listView = (ListView) findViewById(R.id.remindersListView);
		ArrayList<String> listItems = new ArrayList<String>();
		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, listItems);
		listView.setAdapter(listAdapter);
		// Create context menu.
		registerForContextMenu(listView);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// Check box was clicked.
				ListView listView = (ListView) findViewById(R.id.remindersListView);
				Boolean chk = listView.isItemChecked(position);
				SharedPreferences sharedPrefRules = getSharedPreferences(
						"rules", MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPrefRules.edit();
				editor.putBoolean("Enabled" + Integer.toString(position + 1),
						chk);
				editor.commit();
				// Refreshing reminders and restarting alarms.
				refreshList();
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		refreshList();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.reminders_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add:
			// Show Add Reminder activity.
			Intent intent = new Intent(this, AddReminderActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Inflate the menu.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.reminders_list_context, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.action_delete:
			// Delete selected.
			SharedPreferences sharedPrefRulesD = getSharedPreferences("rules",
					MODE_PRIVATE);
			int rulesCount = sharedPrefRulesD.getInt("RulesCount", 0);
			// Removing item: shifting all up starting from selected.
			SharedPreferences.Editor editor = sharedPrefRulesD.edit();
			for (int i = info.position + 1; i < rulesCount; i++) {
				editor.putString(
						"Title" + Integer.toString(i),
						sharedPrefRulesD.getString(
								"Title" + Integer.toString(i + 1), "error"));
				editor.putString(
						"Text" + Integer.toString(i),
						sharedPrefRulesD.getString(
								"Text" + Integer.toString(i + 1), "error"));
				editor.putString(
						"SSID" + Integer.toString(i),
						sharedPrefRulesD.getString(
								"SSID" + Integer.toString(i + 1), "error"));
				editor.putBoolean(
						"Enabled" + Integer.toString(i),
						sharedPrefRulesD.getBoolean(
								"Enabled" + Integer.toString(i + 1), false));
			}
			// Removing last item.
			editor.remove("Title" + Integer.toString(rulesCount));
			editor.remove("Text" + Integer.toString(rulesCount));
			editor.remove("SSID" + Integer.toString(rulesCount));
			editor.remove("Enabled" + Integer.toString(rulesCount));
			editor.putInt("RulesCount", rulesCount - 1);
			editor.commit();
			refreshList();
			return true;
		case R.id.action_edit:
			// Edit Text selected.
			final SharedPreferences sharedPrefRulesE = getSharedPreferences(
					"rules", MODE_PRIVATE);
			// Showing dialog with selected item's reminder text to edit.
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.action_edit);
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
					| InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			input.setSingleLine(false);
			input.setText(sharedPrefRulesE.getString(
					"Text" + Integer.toString(info.position + 1), "error"));
			alert.setView(input);
			alert.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// OK pressed: storing new value and refreshing
							// list.
							String value = input.getText().toString();
							SharedPreferences.Editor editor = sharedPrefRulesE
									.edit();
							editor.putString(
									"Text"
											+ Integer
													.toString(info.position + 1),
									value);
							refreshList();
							editor.commit();
						}
					});
			alert.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Cancel pressed.
						}
					});
			alert.show();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}

	public void refreshList() {
		// Create adapter for remindersListView.
		ListView listView = (ListView) findViewById(R.id.remindersListView);
		ArrayList<String> listItems = new ArrayList<String>();
		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice, listItems);
		listView.setAdapter(listAdapter);
		listItems.clear();
		Bundle bundle = new Bundle();
		SharedPreferences sharedPreferences = getSharedPreferences("rules",
				MODE_PRIVATE);
		int rulesCount = sharedPreferences.getInt("RulesCount", 0);
		int activeCount = 0;
		for (int i = 1; i <= rulesCount; i++) {
			listItems.add(sharedPreferences.getString(
					"Title" + Integer.toString(i), "error")
					+ " ("
					+ sharedPreferences.getString("SSID" + Integer.toString(i),
							"error") + ")");
			if (sharedPreferences.getBoolean("Enabled" + Integer.toString(i),
					false)) {
				listView.setItemChecked(i - 1, true);
				activeCount++;
				// Add SSIDs of active reminders to bundle.
				bundle.putString(
						"SSID" + Integer.toString(activeCount),
						sharedPreferences.getString(
								"SSID" + Integer.toString(i), "error"));
			} else {
				listView.setItemChecked(i - 1, false);
			}
		}
		// Add active reminders count to bundle.
		bundle.putInt("ActiveRulesCount", activeCount);
		if (activeCount > 0) {
			startAlarm();
		} else {
			stopAlarm();
		}
	}

	public void startAlarm() {
		AlarmManager am = (AlarmManager) this
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(getAppContext(), AlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(getAppContext(), 0, i, 0);
		am.cancel(pi);
		am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(),
				5000, pi);
	}

	public void stopAlarm() {
		AlarmManager am = (AlarmManager) this
				.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(getAppContext(), AlarmReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(getAppContext(), 0, i, 0);
		am.cancel(pi);
	}

}
