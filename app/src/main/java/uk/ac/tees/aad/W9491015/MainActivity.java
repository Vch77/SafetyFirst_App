package uk.ac.tees.aad.W9491015;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.ac.tees.aad.W9491015.Notification.APIService;
import uk.ac.tees.aad.W9491015.Notification.Client;
import uk.ac.tees.aad.W9491015.Notification.Data;
import uk.ac.tees.aad.W9491015.Notification.MyResponse;
import uk.ac.tees.aad.W9491015.Notification.NotificationSender;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    Circle circle;
    GoogleMap mMap;
    MapView mMapView;
    //AutocompleteSupportFragment source, destination;

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int DEFAULT_ZOOM = 15;

    String currentUser;
    DatabaseReference mDatabaseReference, mConnectionReference, mNotificationDatabase;

    Marker currentLocationMarker;

    MaterialCardView mylocation, logout;
    MaterialButton start, stop, pause, connections;
    int trackStatus = 3;
    Location previousLocation;

    private LocationCallback locationCallback;
    LocationRequest locationRequest;

    TextView mCountDown;
    //countdown timer declaration
    private long seconds, minutes;
    CountDownTimer mCountDownTimer = null;
    int i = 0;
    private ProgressBar mProgressBar;

    private APIService apiService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mylocation = findViewById(R.id.MyLocation);
        logout = findViewById(R.id.Logout);
        start = findViewById(R.id.btnStartTracking);
        stop = findViewById(R.id.btnStopTracking);
        pause = findViewById(R.id.btnPauseTracking);
        connections = findViewById(R.id.btnConnections);

        currentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users").child(currentUser);

        mConnectionReference = FirebaseDatabase.getInstance().getReference("Connections").child(currentUser);

        mNotificationDatabase = FirebaseDatabase.getInstance().getReference("Notifications");

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView = (MapView) findViewById(R.id.mapView);

        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    if (currentLocationMarker != null) {
                        currentLocationMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                    } else {
                        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                                .title("Currently here"));
                    }

                    updateLastKnowLocationToDatabase(location);

                    switch (trackStatus) {
                        case 0:
                            stoppedTrack(location);
                            trackStatus = 3;
                            break;

                        case 1:
                            pausedTrack();
                            break;

                        case 2:
                            checkSuspiciousActivity(location);
                            ongoingTrack(location);
                            break;

                        default:
                            break;
                    }
                }
            }
        };

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                locationRequest.setInterval(10000);
                locationRequest.setFastestInterval(10000);

                trackStatus = 2;
                stop.setVisibility(View.VISIBLE);
                pause.setVisibility(View.VISIBLE);
                start.setVisibility(View.GONE);
                connections.setVisibility(View.GONE);

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                locationRequest.setInterval(3000);
                locationRequest.setFastestInterval(3000);

                trackStatus = 0;
                stop.setVisibility(View.GONE);
                pause.setVisibility(View.GONE);
                start.setVisibility(View.VISIBLE);
                connections.setVisibility(View.VISIBLE);

            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                trackStatus = 1;

            }
        });

        connections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(MainActivity.this, ConnectionsActivity.class));

            }
        });

        mylocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                getDeviceLocation();

            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(false);
                builder.setTitle("Sign out");
                builder.setMessage("Do you want to sign out from your account?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //mDatabaseReference.child("token_id").removeValue();

                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

    }

    public static class UpdateDeviceToken extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.d("Device FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        // Get new FCM registration token
                        String token = task.getResult();

                        Map<String, Object> tokenmap = new HashMap<>();
                        tokenmap.put("token_id", token);
                        FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(tokenmap);
                    }
                });
            }
            return null;
        }
    }

    private void checkSuspiciousActivity(Location currentLocation) {

        if (previousLocation != null) {

            float[] result = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                    previousLocation.getLatitude(), previousLocation.getLongitude(), result);

            if (result[0] < 5) {
                createSuspiciousAlert();
            }
        }
    }

    private void createSuspiciousAlert() {

        trackStatus = 3;

        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.CustomAlertDialog);
        ViewGroup viewGroup = findViewById(android.R.id.content);
        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.suspicious_alert_layout, viewGroup, false);

        MaterialButton sendAlert = dialogView.findViewById(R.id.btnSendAlert);
        MaterialButton marksafe = dialogView.findViewById(R.id.btnMarkSafe);

        builder.setView(dialogView);
        final AlertDialog alertDialog = builder.create();

        alertDialog.setCanceledOnTouchOutside(false);

        mCountDown = dialogView.findViewById(R.id.countDownText);
        mProgressBar = dialogView.findViewById(R.id.ProgressBar);

        // Count time with Progress bar logic
        mCountDownTimer = new CountDownTimer(60000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                i++;

                seconds = (long) (millisUntilFinished / 1000);
                minutes = seconds / 60;
                seconds = seconds % 60;
                mCountDown.setText(String.format("%02d", minutes) + ":" + String.format("%02d", seconds));

                mProgressBar.setProgress((int) i * 100 / (60000 / 1000));
            }

            @Override
            public void onFinish() {
                //Do what you want
                i++;
                mProgressBar.setProgress(100);

                sendAlertToAllConnections();

                mCountDown.setText("Alerts Sent to Connections !!!");

            }
        };

        mCountDownTimer.start();// Countdown time with Progress bar logic ends here

        sendAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackStatus = 2;
                mCountDownTimer.cancel();
                i = 0;
                sendAlertToAllConnections();
                alertDialog.dismiss();
            }
        });

        marksafe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackStatus = 2;
                mCountDownTimer.cancel();
                i = 0;
                alertDialog.dismiss();
            }
        });

        alertDialog.show();

    }

    private void ongoingTrack(Location location) {

        if (circle != null)
            circle.setCenter(new LatLng(location.getLatitude(), location.getLongitude()));
        else {
            CircleOptions circleOptions = new CircleOptions()
                    .radius(5).center(new LatLng(location.getLatitude(), location.getLongitude()));
            circle = mMap.addCircle(circleOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), 20));
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 20));

        previousLocation = location;

    }

    private void stoppedTrack(final Location location) {

        if (circle != null) {
            circle.remove();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        }

    }

    private void pausedTrack() {

    }

    private void sendAlertToAllConnections() {

        mConnectionReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (final DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                    mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot2) {

                            String connectionToSend = postSnapshot.getKey();

                            String needieName = dataSnapshot2.child("Name").getValue().toString();
                            String lastLat = dataSnapshot2.child("LastLat").getValue().toString();
                            String lastLng = dataSnapshot2.child("LastLng").getValue().toString();

                            FirebaseDatabase.getInstance().getReference("Users").child(connectionToSend).child("token_id")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            String usertoken = dataSnapshot.getValue(String.class);
                                            sendNotifications(usertoken,
                                                    getNotificationTitle(needieName, System.currentTimeMillis()),
                                                    "Geocordinates: Lat " + lastLat + " Lng " + lastLng);

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void sendNotifications(String usertoken, String title, String message) {
        Data data = new Data(title, message);
        NotificationSender sender = new NotificationSender(data, usertoken);
        apiService.sendNotifcation(sender).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                if (response.code() == 200) {
                    if (response.body().success != 1) {
                        Toast.makeText(MainActivity.this, "Failed ", Toast.LENGTH_LONG);
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {

            }
        });
    }

    private String getNotificationTitle(String name, long time) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();//get your local time zone.
        //SimpleDateFormat sdf = new SimpleDateFormat("E dd-LLL-yyyy hh:mm a"); //day date-month-year hour:minute am/pm
        SimpleDateFormat sdf = new SimpleDateFormat("dd-LLL-yy hh:mm a");
        sdf.setTimeZone(tz);//set time zone.

        return name + " needs you | " + sdf.format(new Date(time));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        getDeviceLocation();
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            if (mLastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));

                                if (currentLocationMarker != null) {
                                    currentLocationMarker.setPosition(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()));
                                } else {
                                    currentLocationMarker = mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude()))
                                            .title("Currently here"));
                                }

                                updateLastKnowLocationToDatabase(mLastKnownLocation);
                                createLocationRequest();
                            }
                        } else {
                            Log.d("homepagemap", "Current location is null. Using defaults.");
                            Log.d("homepagemap", "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);

                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d("homepagemap", e.getMessage());
        }
    }

    protected void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void updateLastKnowLocationToDatabase(Location mLastKnownLocation){

        HashMap<String,Object> latestLocation = new HashMap<>();
        latestLocation.put("LastLat",mLastKnownLocation.getLatitude());
        latestLocation.put("LastLng",mLastKnownLocation.getLongitude());
        mDatabaseReference.updateChildren(latestLocation);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        new UpdateDeviceToken().execute();
        mMapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
