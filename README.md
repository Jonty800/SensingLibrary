# SensorLibrary

The SensorLibrary is a library project for Android application developers. The main goal of the project is to make accessing and polling for Android smartphone sensor data easy, highly configurable, and battery-friendly.

## Authors & Contributors
* Jon Baker ([Jonty800](https://github.com/Jonty800))


## Setting up permissions

	public void checkPermissions() {
		boolean ok = true;
		for (String permission : Settings.getPermissionCodes()) {
			if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, Settings.getPermissionCodes(), PERMISSIONS_REQUEST);
				ok = false;
				break;
			}
		}
		if (ok) {
			startSensorsAfterPermissionAccepted();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSIONS_REQUEST: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					checkPermissions();
				}
			}
		}
	}
	  
## Start Sensing

	void startSensorsAfterPermissionAccepted(){
		SensorConfig sensorConfig = new SensorConfig();
		sensorConfig.SAMPLING_RATE = 10000;
		sensorConfig.AWAKE_WINDOW_SIZE = 60000 + 100;
		sensorConfig.SLEEP_WINDOW_SIZE = 60000 * 2;
		sensorConfig.saveToDatabase = false;
		
		sensorManager.startSensor(SensorUtils.SENSOR_TYPE_WIFI, sensorConfig);
		sensorManager.subscribeToSensorListener(this);
	}
	
## Listen to Sensors

	implements SensingEvent.SensingEventListener {
	

    @Override
	public void onDataSensed(SensorData sensorData) {
		if(sensorData.getSensorType() == SensorUtils.SENSOR_TYPE_WIFI{
			NetworkCache.getInstance().getRawHistoricData().add(sensorData.toWifiData());
		}
	}

    @Override
    public void onSensingStarted(int sensorType) {

    }

    @Override
    public void onSensingStopped(int sensorType) {

    }

    @Override
    public void onSensingPaused(int sensorType) {

    }

    @Override
    public void onSensingResumed(int sensorType) {

    }

## License
Copyright (C) Jon Baker

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

More information available [here](http://en.wikipedia.org/wiki/BSD_licenses).
