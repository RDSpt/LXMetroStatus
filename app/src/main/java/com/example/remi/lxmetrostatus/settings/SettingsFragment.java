package com.example.remi.lxmetrostatus.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.example.remi.lxmetrostatus.R;

import java.util.Set;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
	
	private static final String TAG = "Debug SettingsFragment";
	
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		
		addPreferencesFromResource(R.xml.pref_general);
		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
		PreferenceScreen  prefScreen        = getPreferenceScreen();
		int               count             = prefScreen.getPreferenceCount();
		disableNotificationLine();
		for (int i = 0; i < count; i++) {
			Preference p = prefScreen.getPreference(i);
			if (!(p instanceof CheckBoxPreference)) {
				if (p instanceof MultiSelectListPreference && p.isEnabled()) {
					Set<String> value = sharedPreferences.getStringSet(p.getKey(), null);
					setPreferenceSummary(p, value);
					
				}
			}
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		
		disableNotificationLine();
		Preference preference = findPreference(s);
		if (preference != null) {
			if (preference instanceof MultiSelectListPreference) {
				setPreferenceSummary(preference, sharedPreferences.getStringSet(s, null));
			}
			else if (!(preference instanceof CheckBoxPreference)) {
				setPreferenceSummary(preference, sharedPreferences.getString(s, ""));
			}
			
		}
	}
	
	private void disableNotificationLine() {
		
		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
		boolean notificationCheck = sharedPreferences.getBoolean("notificationCheck",
				true);
		Log.d(TAG, "disableNotificationLine: " + notificationCheck);
		MultiSelectListPreference notifiedLines = (MultiSelectListPreference) getPreferenceScreen
				().getPreference(1);
		if (!notificationCheck)
			notifiedLines.setEnabled(false);
		else notifiedLines.setEnabled(true);
	}
	
	private void setPreferenceSummary(Preference preference, Object value) {
		
		String        stringValue = value.toString();
		StringBuilder summary     = new StringBuilder();
		if (preference instanceof MultiSelectListPreference) {
			SharedPreferences sharedPrefs = preference.getSharedPreferences();
			Set<String>       selections  = sharedPrefs.getStringSet("notifiedLines", null);
			for (CharSequence entry : selections) {
				summary.append(entry);
				summary.append(" ");
			}
			preference.setSummary(summary);
		}
		else {
			preference.setSummary(stringValue);
		}
	}
	
	@Override
	public void onStart() {
		
		super.onStart();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onDestroyView() {
		
		super.onDestroyView();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}
	
}
