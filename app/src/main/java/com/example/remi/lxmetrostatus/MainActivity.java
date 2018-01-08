package com.example.remi.lxmetrostatus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.remi.lxmetrostatus.lines.LineInfo;
import com.example.remi.lxmetrostatus.settings.SettingsActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SharedPreferences
		.OnSharedPreferenceChangeListener {
	
	private static final String TAG = "MainActivity";
	TextView yellowStatus, blueStatus, redStatus, greenStatus, lastUpdate;
	ImageView yellowStatusImage, blueStatusImage, redStatusImage, greenStatusImage;
	LinearLayout menuView, yellowLL, blueLL, greenLL, redLL;
	ProgressBar mProgressBar;
	FloatingActionButton fab, map;
	AdView mAdView;
	LineInfo lineInfo = new LineInfo();
	
	private static boolean PREFERENCES_HAVE_BEEN_UPDATED = false;
	static Context appContext;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		appContext = getApplicationContext();
		setContentView(R.layout.activity_main);
		MobileAds.initialize(getApplicationContext(),
				"ca-app-pub-4408939052892175~3966136800");
		findIds();
		getInfo();
		setOnAction();
		if (getRefreshRate() != 0) {
			mHandlerTask.run();
		}
		Intent mServiceIntent = new Intent(this, BackgroundService.class);
		if (getNotificationCheck()) {
			mServiceIntent.putExtra("refreshRate", getRefreshRate());
			mServiceIntent.putExtra("notifiedLines", getNotificationLines());
			mServiceIntent.putExtra("amar", lineInfo.getTipo_msg_am());
			mServiceIntent.putExtra("azul", lineInfo.getTipo_msg_az());
			mServiceIntent.putExtra("verd", lineInfo.getTipo_msg_vd());
			mServiceIntent.putExtra("verm", lineInfo.getTipo_msg_vm());
			startService(mServiceIntent);
		}
		else {
			stopService(mServiceIntent);
		}
		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(this);
		
	}
	
	Handler mHandler = new Handler();
	
	Runnable mHandlerTask = new Runnable() {
		
		@Override
		public void run() {
			
			getInfo();
			mHandler.postDelayed(mHandlerTask, getRefreshRate());
		}
	};
	
	@Override
	protected void onStart() {
		
		super.onStart();
		if (PREFERENCES_HAVE_BEEN_UPDATED) {
			PREFERENCES_HAVE_BEEN_UPDATED = false;
		}
	}
	
	@Override
	protected void onStop() {
		
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		PreferenceManager.getDefaultSharedPreferences(this)
				.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	private void setOnAction() {
		
		yellowLL.setOnClickListener(view -> checkLine(view, R.drawable.linha_amarela));
		blueLL.setOnClickListener(view -> checkLine(view, R.drawable.linha_azul));
		greenLL.setOnClickListener(view -> checkLine(view, R.drawable.linha_verde));
		redLL.setOnClickListener(view -> checkLine(view, R.drawable.linha_vermelha));
		map.setOnClickListener(view -> checkLine(view, R.drawable.map));
		fab.setOnClickListener(view -> getInfo());
	}
	
	private void getInfo() {
		
		String android_id = "C5C2492667E1B41EF314B8814ED951E8";
		String toastText = yellowStatus.getText() == getString(R.string.error) ? "Error: " + getString(R
				.string.error) : "Refreshing...";
		Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show();
		new InfoThread().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "http://app.metrolisboa" +
				".pt/status/getLinhas.php");
		AdRequest adRequest = new AdRequest.Builder().addTestDevice(AdRequest
				.DEVICE_ID_EMULATOR).addTestDevice(android_id).build();
		mAdView.loadAd(adRequest);
	}
	
	private void checkLine(View view, int image) {
		
		Intent intent = new Intent(this, InfoActivity.class);
		intent.putExtra("ImageSrc", image);
		//intent.putExtra("timefrequency", getTimeFrequency());
		startActivity(intent);
	}
	
	private void findIds() {
		
		menuView = findViewById(R.id.mainLayout);
		yellowStatus = findViewById(R.id.yellowStatus);
		blueStatus = findViewById(R.id.blueStatus);
		redStatus = findViewById(R.id.redStatus);
		greenStatus = findViewById(R.id.greenStatus);
		lastUpdate = findViewById(R.id.lastUpdate);
		mProgressBar = findViewById(R.id.indeterminateBar);
		yellowStatusImage = findViewById(R.id.amarelaStatusImage);
		blueStatusImage = findViewById(R.id.azulStatusImage);
		redStatusImage = findViewById(R.id.vermelhaStatusImage);
		greenStatusImage = findViewById(R.id.verdeStatusImage);
		yellowLL = findViewById(R.id.yellowLL);
		blueLL = findViewById(R.id.blueLL);
		greenLL = findViewById(R.id.greenLL);
		redLL = findViewById(R.id.redLL);
		fab = findViewById(R.id.fab);
		map = findViewById(R.id.map);
		mAdView = findViewById(R.id.adView);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
		
		PREFERENCES_HAVE_BEEN_UPDATED = true;
	}
	
	class InfoThread extends AsyncTask<String, Void, LineInfo> {
		
		@Override
		protected void onPreExecute() {
			
			Animation rotation = AnimationUtils.loadAnimation(fab.getContext(), R.anim
					.button_rotate);
			rotation.setRepeatCount(Animation.INFINITE);
			fab.startAnimation(rotation);
			menuView.setVisibility(View.INVISIBLE);
			mProgressBar.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		
		@Override
		protected LineInfo doInBackground(String... strings) {
			
			try {
				URL    url    = new URL(strings[0]);
				String result = getResponseFromHttpUrl(url);
				return parse(result);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(LineInfo lineInfo) {
			
			super.onPostExecute(lineInfo);
			if (lineInfo != null) {
				yellowStatus.setText(lineInfo.getAmarela());
				setImageSrc(yellowStatusImage, lineInfo.getTipo_msg_am());
				blueStatus.setText(lineInfo.getAzul());
				setImageSrc(blueStatusImage, lineInfo.getTipo_msg_az());
				greenStatus.setText(lineInfo.getVerde());
				setImageSrc(greenStatusImage, lineInfo.getTipo_msg_vd());
				redStatus.setText(lineInfo.getVermelha());
				setImageSrc(redStatusImage, lineInfo.getTipo_msg_vm());
				lastUpdate.setText(currentTime());
				menuView.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.INVISIBLE);
			}
			else {
				yellowStatus.setText(R.string.error);
				yellowStatusImage.setImageResource(R.drawable.error404);
				blueStatus.setText(R.string.error);
				blueStatusImage.setImageResource(R.drawable.error404);
				greenStatus.setText(R.string.error);
				greenStatusImage.setImageResource(R.drawable.error404);
				redStatus.setText(R.string.error);
				redStatusImage.setImageResource(R.drawable.error404);
				menuView.setVisibility(View.VISIBLE);
				mProgressBar.setVisibility(View.INVISIBLE);
			}
			fab.clearAnimation();
		}
		
		private String getResponseFromHttpUrl(URL url) throws IOException {
			
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			try {
				InputStream in      = urlConnection.getInputStream();
				Scanner     scanner = new Scanner(in);
				scanner.useDelimiter("\\A");
				boolean hasInput = scanner.hasNext();
				if (hasInput) {
					return scanner.next();
				}
				else {
					return null;
				}
			} finally {
				urlConnection.disconnect();
			}
		}
	}
	
	private void setImageSrc(ImageView StatusImage, String s) {
		
		if (s.equals("0")) {
			StatusImage.setImageResource(R.drawable.check);
		}
		else if (s.equals("1")) {
			StatusImage.setImageResource(R.drawable.caution);
		}
	}
	
	private String currentTime() {
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			Calendar         cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			return sdf.format(cal.getTime());
		}
		return null;
	}
	
	private LineInfo parse(String input) {
		
		try {
			JSONObject mjson = new JSONObject(input);
			lineInfo.setAmarela(changeText(mjson.getString("amarela")));
			lineInfo.setAzul(changeText(mjson.getString("azul")));
			lineInfo.setVerde(changeText(mjson.getString("verde")));
			lineInfo.setVermelha(changeText(mjson.getString("vermelha")));
			lineInfo.setTipo_msg_am(mjson.getString("tipo_msg_am"));
			lineInfo.setTipo_msg_az(mjson.getString("tipo_msg_az"));
			lineInfo.setTipo_msg_vd(mjson.getString("tipo_msg_vd"));
			lineInfo.setTipo_msg_vm(mjson.getString("tipo_msg_vm"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return lineInfo;
	}
	
	private String changeText(String text) {
		
		if (text.toLowerCase().contains("ok")) {
			return getString(R.string.lsp);
		}
		else if (text.toLowerCase()
				.contains(getString(R.string.lcp1).toLowerCase())) {
			return getString(R.string.lcp1);
		}
		/*else if (text.equals("")) {
		}*/
		return text;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int id = item.getItemId();
		if (id == R.id.settings) {
			Intent settingsIntent = new Intent(this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public static int getRefreshRate() {
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(appContext);
		String key          = "refreshRate";
		String defaultValue = appContext.getString(R.string.pref_refreshRate_default);
		int    currentValue = Integer.parseInt(prefs.getString(key, defaultValue));
		int    refreshRate  = currentValue * 1000 * 60;
		return refreshRate;
	}
	
	public static boolean getNotificationCheck() {
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(appContext);
		String key = "notificationCheck";
		//boolean notificationCheck = Boolean.parseBoolean(prefs.getString(key, "true"));
		boolean notificationCheck = prefs.getBoolean(key, true);
		Log.w(TAG, "getNotificationCheck: " + notificationCheck);
		return notificationCheck;
		
	}
	
	public static ArrayList<String> getNotificationLines() {
		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(appContext);
		String            key           = "notifiedLines";
		ArrayList<String> notifiedLines = new ArrayList<>();
		Set<String>       selections    = prefs.getStringSet(key, null);
		if (selections != null)
			for (String selectedLine : selections) {
				notifiedLines.add(selectedLine);
			}
		return notifiedLines;
	}
	
}
