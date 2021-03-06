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
//import com.microsoft.band.sdk.sampleapp.streaming.R;
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
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandContactEvent;
import com.microsoft.band.sensors.BandContactEventListener;
import com.microsoft.band.sensors.BandContactState;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.UVIndexLevel;

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
import java.io.StringBufferInputStream;
import java.util.Calendar;
import java.io.File;
import android.os.Environment;
import java.util.Date;
import java.sql.Timestamp;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

public class BandStreamingAppActivity extends Aware_Plugin {

	private BandClient client = null;
	private Button btnStart;
	private TextView txtStatus;


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
	private float distance;
	private BandContactState contact;
	private UVIndexLevel UV;
	private int gyroscope;

	private String heartRateL = "Unix Timestamp,Local Timestamp,Heart Rate\n";
	private String accelerometerL = "Unix Timestamp,Local Timestamp,x,y,z\n";
	private String stepsL = "Unix Timestamp,Local Timestamp,steps\n";
	private String skinTempL  ="Unix Timestamp,Local Timestamp,Skin Temperature\n";
	private String caloriesL = "Unix Timestamp,Local Timestamp,Calories\n";
	private String distanceL = "Unix Timestamp,Local Timestamp,Distance\n";
	private String UVL = "Unix Timestamp,Local Timestamp,UV\n";

	private static class Params {
		String filename;
		String update;
		String firstLine;

		Params(String filename, String update, String firstLine) {
			this.filename = filename;
			this.update = update;
			this.firstLine = firstLine;
		}
	}


	@Override
    public void onCreate() {
        super.onCreate();
        new appTask().execute(); //runs receivers on background thread
		sdCard = Environment.getExternalStorageDirectory();
		dir = new File (sdCard.getAbsolutePath() + "/band");
		dir.mkdirs();


	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		//TODO:  unregister everything
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
                    //register receivers
					client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS16);
					client.getSensorManager().registerPedometerEventListener(mPedometerEventListener);
					client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureEventListener);
					client.getSensorManager().registerCaloriesEventListener(mCaloriesEventListener);
					client.getSensorManager().registerUVEventListener(mUVEventListener);
					client.getSensorManager().registerDistanceEventListener(mDistanceEventListener);
					if(client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
						client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
					} else {
						//TODO: fill in ???
                        //client.getSensorManager().requestHeartRateConsent(, mHeartRateConsentListener);
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
		/*this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				txtStatus.setText(string);
			}
		});*/
	}


    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {
				long time = event.getTimestamp();
				Date date= new java.util.Date();

				x = event.getAccelerationX();
				y = event.getAccelerationY();
				z = event.getAccelerationZ();
				String update = Long.toString(time);
				update += "," + new Timestamp(date.getTime());
				update += ("," + x + "," + y + "," + z + "\n");
				Params params = new Params("accelerometer.csv",update,accelerometerL);
				new writeOnFile().execute(params);
            	appendToUI(update);

            }
        }
    };


	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
		@Override
		public void onBandHeartRateChanged(final BandHeartRateEvent event) {
			if (event != null) {
				long time = event.getTimestamp();
				Date date= new java.util.Date();
				heartRate = event.getHeartRate();
				String update = Long.toString(time);
				update += "," + new Timestamp(date.getTime());
				update += ("," + heartRate + "\n");
				Params params = new Params("heartRate.csv",update,heartRateL);
				new writeOnFile().execute(params);
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
				long time = event.getTimestamp();
				Date date= new java.util.Date();
				skinTemp = event.getTemperature();
				String update = Long.toString(time);
				update += "," + new Timestamp(date.getTime());
				update += ("," + skinTemp + "\n");
				Params params = new Params("skinTemperature.csv",update,skinTempL);
				new writeOnFile().execute(params);
				appendToUI(update);
			}
		}
	};


	private BandPedometerEventListener mPedometerEventListener = new BandPedometerEventListener() {
		@Override
		public void onBandPedometerChanged(BandPedometerEvent event) {
			if (event != null) {
				long time = event.getTimestamp();
				Date date= new java.util.Date();
				steps = event.getTotalSteps();
				String update = Long.toString(time);
				update += "," + new Timestamp(date.getTime());
				update += ("," + steps + "\n");
				Params params = new Params("pedometer.csv",update,stepsL);
				new writeOnFile().execute(params);
				appendToUI(update);
			}
		}
	};


	private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
		@Override
		public void onBandCaloriesChanged(BandCaloriesEvent event) {
			if (event != null) {
				long time = event.getTimestamp();
				Date date= new java.util.Date();
				calories = event.getCalories();
				String update = Long.toString(time);
				update += "," + new Timestamp(date.getTime());
				update += ("," + calories + "\n");
				Params params = new Params("calories.csv",update,caloriesL);
				new writeOnFile().execute(params);
				appendToUI(update);
			}
		}
	};

	private BandDistanceEventListener mDistanceEventListener = new BandDistanceEventListener() {
		@Override
		public void onBandDistanceChanged(BandDistanceEvent event) {
			if (event != null) {
				long time = event.getTimestamp();
				Date date= new java.util.Date();
				distance = event.getTotalDistance();
				//can have motiontype, pace and speed as well
				String update = Long.toString(time);
				update += "," + new Timestamp(date.getTime());
				update += ("," + distance + "\n");
				Params params = new Params("distance.csv",update,distanceL);
				new writeOnFile().execute(params);
				appendToUI(update);
			}
		}
	};


	private BandUVEventListener mUVEventListener = new BandUVEventListener() {
		@Override
		public void onBandUVChanged(BandUVEvent event) {
			if (event != null){
				long time = event.getTimestamp();
				Date date= new java.util.Date();
				UV = event.getUVIndexLevel();
				String update = Long.toString(time);
				update += "," + new Timestamp(date.getTime());
				update += ("," + UV + "\n");
				Params params = new Params("UV.csv",update,UVL);
				new writeOnFile().execute(params);
				appendToUI(update);
			}
		}
	};

/*
is this information needed?
	private BandGyroscopeEventListener mGyrosocopeEventListener = new BandGyroscopeEventListener() {
		@Override
		public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
			if (event != null){
				time = event.getTimestamp();
			}
		}
	};
*/

	private BandContactEventListener mContactEventListener = new BandContactEventListener() {
		@Override
		public void onBandContactChanged(BandContactEvent event) {
			if (event != null) {
				long time = event.getTimestamp();
				contact = event.getContactState();
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


	private class writeOnFile extends AsyncTask<Params, Void, Void>{
		@Override
		protected Void doInBackground(Params...params) {
			String filename = params[0].filename;
			String update = params[0].update;
			String firstLine = params[0].firstLine;
			file = new File(dir, filename);
			if(!file.exists()) {
                try {
                    file.createNewFile();
					FileOutputStream nfos = null;
					try {
						nfos = new FileOutputStream(file,true);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					OutputStreamWriter nosw = new OutputStreamWriter(nfos);
					try {
						nosw.write(firstLine);
						nosw.flush();
						nosw.close();

					} catch (Exception ex) {
						Log.e("DEBUG", "HERE");
						ex.printStackTrace();
					}
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
			FileOutputStream fos = null;
			try {
                fos = new FileOutputStream(file,true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
			OutputStreamWriter osw = new OutputStreamWriter(fos);
			try {
                osw.write(update);
                osw.flush();
                osw.close();

            } catch (Exception ex) {
                Log.e("DEBUG", "HERE");
                ex.printStackTrace();
            }
			return null;
		}
	}
}

