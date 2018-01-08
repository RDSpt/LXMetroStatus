package com.example.remi.lxmetrostatus;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Remi on 13/11/2017.
 */

public class BackgroundService extends Service {
	
	private static final String TAG = "BACKGROUND SERVICE";
	private Timer mTimer;
	int refreshRate;
	ArrayList<String> notifiedLines;
	static int amar;
	static int azul;
	static int verd;
	static int verm;
	
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		
		return null;
	}
	
	@Override
	public void onCreate() {
		
		super.onCreate();
		mTimer = new Timer();
	
		
	}
	
	TimerTask timerTask = new TimerTask() {
		
		@Override
		public void run() {
			
			try {
				new InfoThread().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "http://app.metrolisboa" +
						".pt/status/getLinhas.php");
				chooseLineNotification();
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
		}
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if (intent != null) {
			refreshRate = intent.getIntExtra("refreshRate", 1);
			notifiedLines = intent.getStringArrayListExtra("notifiedLines");
			Log.i(TAG, "Refresh Rate: " + refreshRate + " | Notified Lines: " + notifiedLines);
			amar = intent.getIntExtra("amar", 0);
			azul = intent.getIntExtra("azul", 0);
			verd = intent.getIntExtra("verd", 0);
			verm = intent.getIntExtra("verm", 0);
			if (refreshRate != 0)
				mTimer.schedule(timerTask, refreshRate);
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		
		super.onDestroy();
		try {
			mTimer.cancel();
			timerTask.cancel();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void chooseLineNotification() throws NoSuchFieldException {
		
		if (notifiedLines != null)
			for (String line : notifiedLines) {
				if (BackgroundService.class.getField(line.substring(0, 3).toLowerCase()).equals(1))
					sendNotifications(line, getResources().getString(R.string.lcp1));
			}
		
	}
	
	public void sendNotifications(String line, String message) {
		
		Intent           intent           = new Intent(this, MainActivity.class);
		TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
		taskStackBuilder.addParentStack(MainActivity.class);
		taskStackBuilder.addNextIntent(intent);
		PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent
				.FLAG_UPDATE_CURRENT);
		Notification notification =
				new Notification.Builder(this)
						.setSmallIcon(R.drawable.notification_icon)
						.setContentTitle("Existem perturbações na linha " + line)
						.setContentText(message)
						.setAutoCancel(true)
						.setContentIntent(pendingIntent)
						.build();
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(1, notification);
	}
	
	private class InfoThread extends AsyncTask<String, Void, String> {
		
		@Override
		protected String doInBackground(String... strings) {
			
			try {
				URL               url           = new URL(strings[0]);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				try {
					InputStream in      = urlConnection.getInputStream();
					Scanner     scanner = new Scanner(in);
					scanner.useDelimiter("\\A");
					boolean hasInput = scanner.hasNext();
					if (hasInput) {
						String     result = scanner.next();
						JSONObject mjson  = new JSONObject(result);
						amar = mjson.getInt("tipo_msg_am");
						azul = mjson.getInt("tipo_msg_az");
						verd = mjson.getInt("tipo_msg_vd");
						verm = mjson.getInt("tipo_msg_vm");
					}
				} catch (JSONException e) {
					e.printStackTrace();
				} finally {
					urlConnection.disconnect();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.wtf(TAG, amar + " " + azul + " " + verd + " " + verm);
			return amar + " " + azul + " " + verd + " " + verm;
		}
	}
}

