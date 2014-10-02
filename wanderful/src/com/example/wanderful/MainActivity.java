
package com.example.wanderful;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.example.wanderful.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;


public class MainActivity extends Activity implements SensorEventListener, LocationListener, OnMarkerClickListener { //for events on change. 

	// global variables
	private GoogleMap googleMap;
	private SensorManager mSensorManager;
	private Sensor magSensor;
	private Sensor accSensor;
	private double latitude,longitude;
	float[] mGravity; // no more compass api 
	float[] mGeomagnetic; 
	float azimut; //compass direction 
	ArrayList<LatLng> allLocations = new ArrayList<LatLng>(); //an array of all coordinates. 
	ArrayList<String> testLocations = new ArrayList<String>();
	ArrayList<Marker> markerArray = new ArrayList<Marker>();
	Polyline joinLine; // red line 

	ArrayList<String> markerInfoArray; // {markerName/markerTitle/markerDetails}

	/*
	 *  Auto-generated method stubs for implemented protocols(non-Javadoc)
	 */
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {  }


	/*
	 *  Event Listeners
	 */

	// listen for marker click
	@Override
	public boolean onMarkerClick(final Marker marker){
		Log.i("here","here");
		marker.showInfoWindow();
		Log.i("title: ",marker.getTitle());
		Log.i("hc: ",Integer.toString(marker.hashCode())); // unique code (Title)
		for(String s : markerInfoArray){
			String placeHash = s.split("/")[0]; // "/" is delimiter
			if(marker.getTitle().equals(placeHash)){
				Log.i("s",s);
				
				String[] placeArray = s.split("/");
				String placeDetails;
				String placeName;
				if(placeArray.length == 3){
					placeName = placeArray[1];
					placeDetails = placeArray[2];
				}
				else{
					placeName = placeArray[1];
					placeDetails = "";
				}
				
				Intent detailsScreen = new Intent(getApplicationContext(),detailsView.class);
				detailsScreen.putExtra("placeDetails",placeDetails); // cheapskate method
				detailsScreen.putExtra("placeTitle",placeName);
				startActivity(detailsScreen);
			}
		}
		return true;
	}

	public double calcBearing(LatLng from, LatLng to){

		double dLon = (to.longitude - from.longitude);
		double y = Math.sin(dLon)*Math.cos(to.latitude);
		double x = Math.cos(from.latitude)*Math.sin(to.latitude) - Math.sin(from.latitude)*Math.cos(to.latitude)*Math.cos(dLon);
		double bearing = Math.atan2(y, x);

		return bearing;
	}

	// listen for compass change event
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic); // calc the value of compass, accele?? etc
			if (success) {
				float orientation[] = new float[3]; // pointer
				SensorManager.getOrientation(R, orientation);
				azimut = orientation[0]; // orientation contains: azimut, pitch and roll
				//Log.i("orientation",Float.toString(azimut));
				CameraPosition pos = CameraPosition.builder().target(new LatLng(latitude,longitude)).bearing((float)Math.toDegrees(azimut)).zoom(16).build();
				googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos)); // draw new bearing on gMaps
				//mov cam
				drawPolyLine();
			}
		}
	}
	
	public void drawPolyLine(){
		// this function will draw the polyline
		LatLng myLocation = new LatLng(latitude,longitude);
		LatLng pointedLocation = new LatLng(latitude,longitude);
		double closestBearing = 9999; // sentinel value
		// sort array according to distance to allow closest point to be set IN CASE got some bearings
		//Log.i("azimut",Float.toString(azimut));
		Marker currMarker = null;
		for(int i = 0 ; i < markerArray.size() ; i++){
			double currentBearing = calcBearing(myLocation,markerArray.get(i).getPosition());
			//Log.i("currentBearing","Place: " + testLocations.get(i) + "  latlng: " + Double.toString(currentBearing));
			if(Math.abs((currentBearing-azimut)) < closestBearing){
				closestBearing = Math.abs(currentBearing-azimut);
				pointedLocation = new LatLng(allLocations.get(i).latitude,allLocations.get(i).longitude);
				currMarker = markerArray.get(i);
			}
			//Log.i("closestBearing",Double.toString(closestBearing));
		}
		if(currMarker != null){
		currMarker.showInfoWindow();
		}
		if(joinLine != null){
			ArrayList<LatLng>points = new ArrayList<LatLng>();
			points.add(new LatLng(latitude,longitude));
			points.add(pointedLocation);
			joinLine.setPoints(points); // array<LatLng> from, to
		}
		else{
			joinLine = googleMap.addPolyline(new PolylineOptions()
			.add(new LatLng(latitude,longitude),pointedLocation).width(5).color(Color.RED));
		}
		/*
		for(int i = 0 ; i < markerArray.size() ; i++){
			Marker marker = markerArray.get(i);
			if(marker.getPosition()==pointedLocation){
				marker.showInfoWindow();
			}
		}
		*/
	}

	// listen for when GPS coord change
	@Override
	public void onLocationChanged(Location location){
		// Getting latitude of the current location
		latitude = location.getLatitude();

		// Getting longitude of the current location
		longitude = location.getLongitude();
		Log.i("latLng",latitude+", "+longitude);
		// Creating a LatLng object for the current location
		LatLng latLng = new LatLng(latitude, longitude);

		CameraPosition cameraPosition = new CameraPosition.Builder().target(
				latLng).zoom(16).build();

		googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition)); //move cam
		drawPolyLine();
	}

	/*
	 * Initialization methods
	 */
	public void initSensor(){ //start sensor for listener to be called
		Log.i("sensor","init");
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		magSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	/*
	 * Networking methods
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) { //main starting point 
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.activity_main); // link UI to main class

		Button btnNextScreen = (Button) findViewById(R.id.btnNextScreen); //POI button
		btnNextScreen.setOnClickListener(new View.OnClickListener() {// set listerner to find click

			@Override
			public void onClick(View arg0) {
				//Starting a new Intent/screen 
				Intent nextScreen = new Intent(getApplicationContext(), poiView.class);
				nextScreen.putExtra("locationCoords", "?locationCoords="+Double.toString(latitude)+","+Double.toString(longitude)); // restrict to only surrounding location
				startActivity(nextScreen); // launch next screen 

			}
		});

		markerInfoArray = new ArrayList<String>();
		initSensor(); 

		try {
			// Loading map
			initilizeMap();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	// The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 0 meters
 
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 1; // 1 second
    
	private void initilizeMap() {
		
		LocationManager locationManager;
		boolean isGPSEnabled, isNetworkEnabled;
		try {
            Location location = null; // location
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
 
            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
 
            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
 
            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            	Log.i("err net","No network");
            } else {
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this); //enable listerner 
                    Log.i("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER); //if cannot get current, get last
                        if (location != null) {
                            latitude = location.getLatitude();// set values to lat and long 
                            longitude = location.getLongitude();
                        }
                    }
                }
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES, // min time to update time 
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);// enable listener
                        Log.i("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude(); 
                            }
                        }
                    }
                }
            }
 
        } catch (Exception e) {
            e.printStackTrace();
        }

		if (googleMap == null) {
			googleMap = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.map)).getMap();
			googleMap.setOnMarkerClickListener(this); // for marker events
			googleMap.setMyLocationEnabled(true);
			googleMap.getUiSettings().setCompassEnabled(true);
			
			/*

			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10, this);
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0,10, this);
			
			*/
			
			
			getLocations(); // pull from server 

			// check if map is created successfully or not
			if (googleMap == null) {
				Toast.makeText(getApplicationContext(),
						"Sorry! unable to create maps", Toast.LENGTH_SHORT)
						.show();
			}
		}

	}

	public void getLocations(){
		Log.i("here","here");
		new sendDataAsync().execute(); // execute() is API
	}

	private class sendDataAsync extends AsyncTask<String, Integer, String>{ // AsyncTask is the type of class

		@Override
		protected String doInBackground(String... arg0) { // derivate from main thread, run in background
			String responseString = "";
			String url = "http://darren.ngrok.com/wanderful_Web/getLocations.php?locationCoords="+Double.toString(latitude)+","+Double.toString(longitude); // add loc coords
			Log.i("url",url);
			HttpResponse response = null;
			try {
				HttpClient client = new DefaultHttpClient();
				response = client.execute(new HttpGet(url));
				responseString = EntityUtils.toString(response.getEntity()); // only get body
				Log.i("resp: ",responseString);

			} catch(IOException e) {

			}
			return responseString;
		}
		@Override
		protected void onPostExecute(String result) { // when connection is completed
			Log.i("result",result);
			try {
				JSONArray locationsArray = (JSONArray) new JSONTokener(result).nextValue(); // parse string into array.
				Log.d("json",locationsArray.toString(4));

				for(int i = 0 ; i < locationsArray.length() ; i++){
					JSONObject locationObject = locationsArray.getJSONObject(i);
					String placeName = locationObject.getString("placeName");
					String placeCoord = locationObject.getString("placeCoord");
					String placeTitle = locationObject.getString("placeTitle");
					String placeSnippet = locationObject.getString("placeSnippet");
					String placeDetails = locationObject.getString("placeDetails");

					double placeLat = Double.parseDouble(placeCoord.split(",")[0]);
					double placeLon = Double.parseDouble(placeCoord.split(",")[1]);

					allLocations.add(new LatLng(placeLat,placeLon));
					testLocations.add(placeName);

					MarkerOptions marker = new MarkerOptions().position(new LatLng(placeLat,placeLon)).title(placeTitle).snippet(placeSnippet); // create new custom marker
					Marker mar = googleMap.addMarker(marker);
					markerArray.add(mar);
					markerInfoArray.add(placeTitle+"/"+placeName+"/"+placeDetails);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_UI);
		initilizeMap();
	}

}