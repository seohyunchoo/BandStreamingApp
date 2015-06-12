//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.sampleapp;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sdk.sampleapp.streaming.R;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.io.File;
import android.os.Environment;

public class BandStreamingAppActivity extends Activity {

	private BandClient client = null;
	private Button btnStart;
	private TextView txtStatus;

	private long time;
	private Calendar cal = Calendar.getInstance();
	private String update;

	private File sdCard;
	private File dir;
	private File file;


	private int heartRate;
	private float x;
	private float y;
	private float z;
	private long steps;
	private float skinTemp;
	private long calories;

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus = (TextView) findViewById(R.id.txtStatus);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txtStatus.setText("");
				new appTask().execute();
			}
		});

		sdCard = Environment.getExternalStorageDirectory();
		dir = new File (sdCard.getAbsolutePath() + "/band");
		dir.mkdirs();
		file = new File(dir, "accelerometer.txt");
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			FileOutputStream fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}
	@Override
	protected void onDestroy(){
		super.onDestroy();
		try {
			client.getSensorManager().unregisterAccelerometerEventListeners();
		} catch (BandIOException e) {
			appendToUI(e.getMessage());
		}
	}

	
	private class appTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
				if (getConnectedBandClient()) {
					appendToUI("Band is connected.\n");
					client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS128);
					client.getSensorManager().registerPedometerEventListener(mPedometerEventListener);
					client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureEventListener);
					client.getSensorManager().registerCaloriesEventListener(mCaloriesEventListener);
					if(client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
						client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
					} else {
						client.getSensorManager().requestHeartRateConsent(BandStreamingAppActivity.this, mHeartRateConsentListener);
					}
				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage();
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}

	
	private void appendToUI(final String string) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				txtStatus.setText(string);
			}
		});
	}


    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {
				time = event.getTimestamp();
				x = event.getAccelerationX();
				y = event.getAccelerationY();
				z = event.getAccelerationZ();
				cal = Calendar.getInstance();
				update = Long.toString(time);
				update += ("  " + x + ", " + y + ", " + z + "\n");
				new writeOnFile().execute();
            	appendToUI(update);

            }
        }
    };


	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
		@Override
		public void onBandHeartRateChanged(final BandHeartRateEvent event) {
			if (event != null) {
				time = event.getTimestamp();
				heartRate = event.getHeartRate();
				update = Long.toString(time);
				update += ("  " + heartRate + "\n");
				new writeOnFile().execute();
				appendToUI(update);
			}
		}
	};


	private HeartRateConsentListener mHeartRateConsentListener = new HeartRateConsentListener() {
		@Override
		public void userAccepted(boolean b) {
			// handle user's heart rate consent decision
			if (!b){
				// Consent hasn't been given
				appendToUI(String.valueOf(b));
			}
		}
	};


	private BandSkinTemperatureEventListener mSkinTemperatureEventListener = new BandSkinTemperatureEventListener() {
		@Override
		public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent event) {
			if (event != null) {
				time = event.getTimestamp();
				skinTemp = event.getTemperature();
				update = Long.toString(time);
				update += ("  " + heartRate + "\n");
				new writeOnFile().execute();
				appendToUI(update);
			}
		}
	};


	private BandPedometerEventListener mPedometerEventListener = new BandPedometerEventListener() {
		@Override
		public void onBandPedometerChanged(BandPedometerEvent event) {
			if (event != null) {
				time = event.getTimestamp();
				steps = event.getTotalSteps();
				update = Long.toString(time);
				update += ("  " + steps + "\n");
				new writeOnFile().execute();
				appendToUI(update);
			}
		}
	};


	private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
		@Override
		public void onBandCaloriesChanged(BandCaloriesEvent event) {
			if (event != null) {
				time = event.getTimestamp();
				calories = event.getCalories();
				update = Long.toString(time);
				update += ("  " + calories + "\n");
				new writeOnFile().execute();
				appendToUI(update);
			}
		}
	};


	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}
		appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
	}


	private class writeOnFile extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... voids) {
			try {
				FileOutputStream fos = new FileOutputStream(file,true);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				try{
					osw.write(update);
					osw.flush();
					osw.close();

				}catch (Exception e){
					Log.e("DEBUG","HERE");
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				Log.e("DEBUG","not found");
				e.printStackTrace();
			}
			return null;
		}
	}
}

