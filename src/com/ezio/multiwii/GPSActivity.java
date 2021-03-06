/*  MultiWii EZ-GUI
    Copyright (C) <2012>  Bartosz Szczygiel (eziosoft)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ezio.multiwii;

import java.util.Iterator;

import com.actionbarsherlock.app.SherlockActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GPSActivity extends SherlockActivity implements LocationListener {

	private boolean			killme			= false;

	App						app;
	Handler					mHandler		= new Handler();

	TextView				GPS_distanceToHomeTV;
	TextView				GPS_directionToHomeTV;
	TextView				GPS_numSatTV;
	TextView				GPS_fixTV;
	TextView				GPS_updateTV;

	TextView				GPS_altitudeTV;
	TextView				GPS_speedTV;
	TextView				GPS_latitudeTV;
	TextView				GPS_longitudeTV;

	TextView				PhoneLatitudeTV;
	TextView				PhoneLongtitudeTV;
	TextView				PhoneAltitudeTV;
	TextView				PhoneSpeedTV;
	TextView				PhoneNumSatTV;
	TextView				DeclinationTV;
	TextView				PhoneAccuracyTV;

	Button					InjectGPSButton;

	int						PhoneNumSat		= 0;
	double					PhoneLatitude	= 0;
	double					PhoneLongitude	= 0;
	double					PhoneAltitude	= 0;
	double					PhoneSpeed		= 0;
	int						PhoneFix		= 0;
	float					PhoneAccuracy	= 0;
	double					Declination		= 0;

	private LocationManager	locationManager;
	private String			provider;
	GeomagneticField		geoField;

	boolean					Injecting		= false;

	private Runnable		update			= new Runnable() {
												@Override
												public void run() {

													{
														app.mw.ProcessSerialData(app.loggingON);

														float lat = (float) (app.mw.GPS_latitude / Math.pow(10, 7));
														float lon = (float) (app.mw.GPS_longitude / Math.pow(10, 7));

														GPS_distanceToHomeTV.setText(String.valueOf(app.mw.GPS_distanceToHome));
														GPS_directionToHomeTV.setText(String.valueOf(app.mw.GPS_directionToHome));
														GPS_numSatTV.setText(String.valueOf(app.mw.GPS_numSat));
														GPS_fixTV.setText(String.valueOf(app.mw.GPS_fix));
														GPS_updateTV.setText(String.valueOf(app.mw.GPS_update));

														if (app.mw.GPS_update % 2 == 0) {
															GPS_updateTV.setBackgroundColor(Color.GREEN);

														}
														else {
															GPS_updateTV.setBackgroundColor(Color.BLACK);
														}

														GPS_altitudeTV.setText(String.valueOf(app.mw.GPS_altitude));
														GPS_speedTV.setText(String.valueOf(app.mw.GPS_speed));

														// GPS_latitudeTV.setText(String.valueOf(app.mw.GPS_latitude));
														// GPS_longitudeTV.setText(String.valueOf(app.mw.GPS_longitude));

														GPS_latitudeTV.setText(String.valueOf(lat));
														GPS_longitudeTV.setText(String.valueOf(lon));

														PhoneLatitudeTV.setText(String.valueOf((float) PhoneLatitude));
														PhoneLongtitudeTV.setText(String.valueOf((float) PhoneLongitude));
														PhoneAltitudeTV.setText(String.valueOf((int) PhoneAltitude));
														PhoneSpeedTV.setText(String.valueOf(PhoneSpeed));
														PhoneNumSatTV.setText(String.valueOf(PhoneNumSat));
														PhoneAccuracyTV.setText(String.valueOf(PhoneAccuracy));
													}

													app.Frequentjobs();

													if (Injecting)

														app.mw.SendRequestGPSinject21((byte) PhoneFix, (byte) PhoneNumSat, (int) (PhoneLatitude * 1e7), (int) (PhoneLongitude * 1e7),
																(int) PhoneAltitude, (int) PhoneSpeed);

													app.mw.SendRequest();
													if (!killme)
														mHandler.postDelayed(update, app.REFRESH_RATE);

												}
											};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.gps_layout);

		app = (App) getApplication();

		GPS_distanceToHomeTV = (TextView) findViewById(R.id.TextViewGPS_distanceToHome);
		GPS_directionToHomeTV = (TextView) findViewById(R.id.TextViewGPS_directionToHome);
		GPS_numSatTV = (TextView) findViewById(R.id.TextViewGPS_numSat);
		GPS_fixTV = (TextView) findViewById(R.id.TextViewGPS_fix);
		GPS_updateTV = (TextView) findViewById(R.id.TextViewGPS_update);

		GPS_altitudeTV = (TextView) findViewById(R.id.TextViewGPS_altitude);
		GPS_speedTV = (TextView) findViewById(R.id.TextViewGPS_speed);
		GPS_latitudeTV = (TextView) findViewById(R.id.TextViewGPS_latitude);
		GPS_longitudeTV = (TextView) findViewById(R.id.TextViewGPS_longitude);

		PhoneLatitudeTV = (TextView) findViewById(R.id.textViewPhoneLatitude);
		PhoneLongtitudeTV = (TextView) findViewById(R.id.textViewPhoneLongitude);
		PhoneAltitudeTV = (TextView) findViewById(R.id.TextViewPhoneAltitude);
		PhoneSpeedTV = (TextView) findViewById(R.id.textViewPhoneSpeed);
		PhoneNumSatTV = (TextView) findViewById(R.id.textViewPhoneNumSat);
		PhoneAccuracyTV = (TextView) findViewById(R.id.textViewPhoneAccuracy);

		InjectGPSButton = (Button) findViewById(R.id.buttonInjectGPS);
		DeclinationTV = (TextView) findViewById(R.id.textViewDeclination);

		if (!app.AdvancedFunctions) {
			InjectGPSButton.setVisibility(View.GONE);
		}

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		if (!app.GPSfromNet)
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
		provider = locationManager.getBestProvider(criteria, false);
		Location location = locationManager.getLastKnownLocation(provider);
		if (location != null) {
			geoField = new GeomagneticField(Double.valueOf(location.getLatitude()).floatValue(), Double.valueOf(location.getLongitude()).floatValue(), Double.valueOf(
					location.getAltitude()).floatValue(), System.currentTimeMillis());
			// PhoneLatitudeTV.setText(String.valueOf(location.getLatitude()));
			// PhoneLongtitudeTV.setText(String.valueOf(location.getLongitude()));
		}
		else {
			// PhoneLatitudeTV.setText("Provider not available");
			// PhoneLongtitudeTV.setText("Provider not available");

		}

		locationManager.addGpsStatusListener(new Listener() {

			@Override
			public void onGpsStatusChanged(int event) {
				if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
					GpsStatus status = locationManager.getGpsStatus(null);
					Iterable<GpsSatellite> sats = status.getSatellites();
					Iterator<GpsSatellite> it = sats.iterator();

					PhoneNumSat = 0;
					while (it.hasNext()) {

						GpsSatellite oSat = (GpsSatellite) it.next();
						if (oSat.usedInFix())
							PhoneNumSat++;

					}

				}
				if (event == GpsStatus.GPS_EVENT_FIRST_FIX)
					PhoneFix = 1;

			}
		});

	}

	public void SHOWHIDDENOncLick(View v) {
		InjectGPSButton.setVisibility(View.VISIBLE);
	}

	public void InjectGPSOnClick(View v) {

		Injecting = true;

	}

	@Override
	protected void onPause() {
		super.onPause();
		mHandler.removeCallbacks(update);
		locationManager.removeUpdates(this);
		killme = true;

	}

	@Override
	protected void onResume() {
		super.onResume();
		app.ForceLanguage();
		killme = false;
		mHandler.postDelayed(update, app.REFRESH_RATE);
		locationManager.requestLocationUpdates(provider, 400, 1, this);
		app.Say(getString(R.string.GPS));

	}

	@Override
	public void onLocationChanged(Location location) {

		PhoneLatitude = location.getLatitude();
		PhoneLongitude = location.getLongitude();
		PhoneAltitude = location.getAltitude();
		PhoneSpeed = location.getSpeed() * 100f;
		PhoneAccuracy=location.getAccuracy()*100f;

		geoField = new GeomagneticField(Double.valueOf(location.getLatitude()).floatValue(), Double.valueOf(location.getLongitude()).floatValue(), Double.valueOf(
				location.getAltitude()).floatValue(), System.currentTimeMillis());
		Declination = geoField.getDeclination();

		DeclinationTV.setText(String.valueOf(Declination));

	}

	@Override
	public void onProviderDisabled(String arg0) {

	}

	@Override
	public void onProviderEnabled(String arg0) {

	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {

	}

}
