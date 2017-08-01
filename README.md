# SensorLibrary

The SensorLibrary is a library project for Android application developers. The main goal of the project is to make accessing and polling for Android smartphone sensor data easy, highly configurable, and battery-friendly.

## Authors & Contributors
* Jon Baker ([Jonty800](https://github.com/Jonty800)), University of Kent, UK

## Example Sensor Subscription

     WifiSensorManager.getInstance(this).getSensorEventListener().subscribeToSensor(new SensingEvent.OnEventListener() {
          @Override
          public void onDataSensed(SensorData sensorData) {
              try {
                  WifiData data = sensorData.toWifiData();
                  Log.i(TAG, data.bssid);
              } catch (Exception e) {
                  e.printStackTrace();
              }
          }
      });

## License
Copyright (C) Jon Baker, University of Kent

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
