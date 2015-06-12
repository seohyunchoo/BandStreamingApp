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
import com.microsoft.band.sdk.sampleapp.streaming.R;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.Calendar;
import java.io.File;
import android.os.Environment;

import static android.content.Context.MODE_WORLD_READABLE;

public class BandStreamingAppActivity extends Activity {

	private BandClient client = null;
	private Button btnStart;
	private TextView txtStatus;
	private String FILENAME = "accelerometerData.txt";
	private float x;
	private float y;
	private float z;
	private java.util.Date time;
	private Calendar cal = Calendar.getInstance();
	private String now;
	private String path = "/band";
	private File sdCard;
	private File dir;
	private File file;


	
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

		/*if (isExternalStorageReadable() && isExternalStorageWritable() && createDirIfNotExists(path)){
			File yourFile = new File(FILENAME);
			if(!yourFile.exists()) {
				try {
					yourFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}*/
		sdCard = Environment.getExternalStorageDirectory();
		dir = new File (sdCard.getAbsolutePath() + "/band");
		dir.mkdirs();
		file = new File(dir, FILENAME);
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
				x = event.getAccelerationX();
				y = event.getAccelerationY();
				z = event.getAccelerationZ();
				cal = Calendar.getInstance();
				time = cal.getTime();
				now = time.toString();
				now += ("  x: " + x + ", y: " + y + ", z: " + z + "\n");
				new writeOnFile().execute();
            	appendToUI(now);

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
				//String name = sdCard + path + "/" + FILENAME;
				//FileOutputStream fos = openFileOutput(name,MODE_APPEND);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
                try{
					osw.write(now);
					osw.flush();
					osw.close();
					//Toast.makeText(getBaseContext(),"Data saved", Toast.LENGTH_LONG).show();
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

