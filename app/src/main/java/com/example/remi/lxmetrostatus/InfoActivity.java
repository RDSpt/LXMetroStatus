package com.example.remi.lxmetrostatus;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class InfoActivity extends AppCompatActivity {
	
	private static final String TAG = "InfoActivity";
	TouchImageView image;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
		this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		image =(TouchImageView) findViewById(R.id.lineImage);
		Intent intent = getIntent();
		int    imgSrc = Integer.valueOf(intent.getExtras().get("ImageSrc").toString());
		image.setImageResource(imgSrc);
		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finishAndRemoveTask();
		}
		return super.onOptionsItemSelected(item);
	}
}
