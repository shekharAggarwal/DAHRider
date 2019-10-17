package com.dah.dahrider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arsy.maps_library.MapRipple;
import com.dah.dahrider.Common.Common;
import com.dah.dahrider.Helper.CustomInfoWindow;
import com.dah.dahrider.Model.Rider;
import com.dah.dahrider.Model.Token;
import com.dah.dahrider.Remote.IFCMService;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.SphericalUtil;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener, ValueEventListener {

    private static final int MY_PERMISSION_REQUEST_CODE = 7192;

    private static final int LIMIT = 3;
    private static final int UPDATE_INTERVAL = 5000;
    private static final int FASTEST_INTERVAL = 3000;
    private static final int DISPLACEMENT = 10;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    SupportMapFragment mapFragment;

    CircleImageView imageAvatar;
    TextView txtRiderName, txtStars;

    FirebaseStorage storage;
    StorageReference storageReference;

    ImageView carX, carBlack;
    boolean isX = true;

    MapRipple mapRipple;

    Marker mUserMarker, markerDestination;

    Button btnPickupRequest;

    int radius = 1; //1km
    int distance = 1;//3km

    IFCMService mService;

    DatabaseReference driversAvailable;

    PlaceAutocompleteFragment place_location, place_destination;
    String mPlaceLocation, mPlaceDestination;
    AutocompleteFilter typeFilter;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;

    private BroadcastReceiver mCancelBroadCast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Common.isDriverFound = false;
            Common.driverId = "";
            btnPickupRequest.setText("PICKUP REQUEST");
            btnPickupRequest.setEnabled(true);

            if (mapRipple.isAnimationRunning())
                mapRipple.stopRippleMapAnimation();

            mUserMarker.hideInfoWindow();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        LocalBroadcastManager.getInstance(this)
//                .registerReceiver(mCancelBroadCast, new IntentFilter("cancel_request"));

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCancelBroadCast, new IntentFilter(Common.CANCEL_BROADCAST_STRING));

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCancelBroadCast, new IntentFilter(Common.BROADCAST_DROP_OFF));

        mService = Common.getFCMService();

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navigationHeaderView = navigationView.getHeaderView(0);
        txtRiderName = navigationHeaderView.findViewById(R.id.txtRiderName);
        txtRiderName.setText(String.format("%s", Common.currentUser.getName()));
        txtStars = navigationHeaderView.findViewById(R.id.txtStars);
        txtStars.setText(String.format("%s", Common.currentUser.getRates()));
        imageAvatar = navigationHeaderView.findViewById(R.id.imageAvatar);

        carX = findViewById(R.id.select_X);
        carBlack = findViewById(R.id.select_Black);

        //event
        carX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isX = true;
                if (isX) {
                    carX.setImageResource(R.drawable.car_cui_select);
                    carBlack.setImageResource(R.drawable.car_vip);
                } else {
                    carX.setImageResource(R.drawable.car_cui);
                    carBlack.setImageResource(R.drawable.car_vip_select);
                }
                mMap.clear();
                if (driversAvailable != null)
                    driversAvailable.removeEventListener(Home.this);

                driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tb1).child(isX ? "X" : "Black");
                driversAvailable.addValueEventListener(Home.this);
                loadAllAvailableDriver(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));

            }
        });

        carBlack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isX = false;
                if (isX) {
                    carX.setImageResource(R.drawable.car_cui_select);
                    carBlack.setImageResource(R.drawable.car_vip);
                } else {
                    carX.setImageResource(R.drawable.car_cui);
                    carBlack.setImageResource(R.drawable.car_vip_select);
                }
                mMap.clear();
                if (driversAvailable != null)
                    driversAvailable.removeEventListener(Home.this);
                driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tb1).child(isX ? "X" : "Black");
                driversAvailable.addValueEventListener(Home.this);
                loadAllAvailableDriver(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
            }
        });


        if (Common.currentUser.getAvatarUrl() != null && !TextUtils.isEmpty(Common.currentUser.getAvatarUrl())) {
            Picasso.get().load(Common.currentUser.getAvatarUrl()).into(imageAvatar);
        }


        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        place_destination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_destination);
        place_location = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_location);
        typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .setTypeFilter(3)
                .build();


        place_location.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlaceLocation = place.getAddress().toString();
                mMap.clear();
                mUserMarker = mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                        .title("Pickup here"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15.0f));
            }

            @Override
            public void onError(Status status) {

            }
        });
        place_destination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlaceDestination = place.getAddress().toString();
                mMap.addMarker(new MarkerOptions().position(place.getLatLng())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                        .title("Destination"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15.0f));
                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.newInstance(mPlaceLocation, mPlaceDestination, false);
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());

            }

            @Override
            public void onError(Status status) {

            }
        });

        setUpLocation();
        updateFirebaseToken();

        btnPickupRequest = findViewById(R.id.btnPickupRequest);
        btnPickupRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Common.isDriverFound) {
                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(Account account) {
                            requestPickupHere(account.getId());
                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {

                        }
                    });
                } else {
                    btnPickupRequest.setEnabled(false);
                    Common.sendRequestToDriver(Common.driverId, mService, getBaseContext(), Common.mLastLocation);
                }
            }
        });

    }

    private void updateFirebaseToken() {
        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
            @Override
            public void onSuccess(final Account account) {
                FirebaseDatabase db = FirebaseDatabase.getInstance();
                final DatabaseReference tokens = db.getReference(Common.token_tb1);

                FirebaseInstanceId.getInstance()
                        .getInstanceId()
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(Home.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                            @Override
                            public void onSuccess(InstanceIdResult instanceIdResult) {
                                Token token = new Token(instanceIdResult.getToken());
                                tokens.child(account.getId())
                                        .setValue(token);
                            }
                        });
            }

            @Override
            public void onError(AccountKitError accountKitError) {

            }
        });
    }


    private void requestPickupHere(String uid) {
        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tb1);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
//                        Toast.makeText(Home.this, "" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        if (mUserMarker.isVisible())
            mUserMarker.remove();

        mUserMarker = mMap.addMarker(new MarkerOptions()
                .title("Pickup Here")
                .position(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)));

        mUserMarker.showInfoWindow();


        mapRipple = new MapRipple(mMap, new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), this);
        mapRipple.withNumberOfRipples(1);
        mapRipple.withDistance(500);
        mapRipple.withRippleDuration(1000);
        mapRipple.withTransparency(0.5f);
        mapRipple.startRippleMapAnimation();

        btnPickupRequest.setText("Getting your DRIVER......");

        findDriver();


    }

    private void findDriver() {

        DatabaseReference driverLocation;
        if (isX)
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tb1).child("X");
        else
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tb1).child("Black");

        GeoFire gf = new GeoFire(driverLocation);
        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()), radius);

        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //if found
                if (!Common.isDriverFound) {
                    Common.isDriverFound = true;
                    Common.driverId = key;
                    btnPickupRequest.setText("Call Driver");
                    //Toast.makeText(Home.this, "" + key, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //if still not found driver,increase distance
                if (!Common.isDriverFound && radius < LIMIT) {
                    radius++;
                    findDriver();
                } else {
                    if (!Common.isDriverFound) {
                        Toast.makeText(Home.this, "No Available any driver near you", Toast.LENGTH_SHORT).show();
                        btnPickupRequest.setText("REQUEST PICKUP");
                    }
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    displayLocation();
                }
        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CALL_PHONE
            }, MY_PERMISSION_REQUEST_CODE);
        } else {
            buildLocationCallBack();
            createLocationRequest();
            displayLocation();

        }
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Common.mLastLocation = locationResult.getLastLocation();
                Common.mLastLocation = locationResult.getLocations().get(locationResult.getLocations().size() - 1);
                displayLocation();
            }
        };

    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;


        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                Common.mLastLocation = location;

                if (Common.mLastLocation != null) {

                    LatLng center = new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude());
                    //distance in meters
                    //heading 0 is northSide, 90 is east,  180 is south and 270 is west
                    LatLng northSide = SphericalUtil.computeOffset(center, 5000, 0);
                    LatLng eastSide = SphericalUtil.computeOffset(center, 5000, 90);
                    LatLng westSide = SphericalUtil.computeOffset(center, 5000, 270);
                    LatLng southSide = SphericalUtil.computeOffset(center, 5000, 180);

                    LatLngBounds bounds = LatLngBounds.builder()
                            .include(northSide)
                            .include(eastSide)
                            .include(westSide)
                            .include(southSide)
                            .build();
                    place_location.setBoundsBias(bounds);
                    place_location.setFilter(typeFilter);

                    place_destination.setBoundsBias(bounds);
                    place_destination.setFilter(typeFilter);


                    driversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tb1).child(isX ? "X" : "Black");
                    driversAvailable.addValueEventListener(Home.this);
//                    final double latitude = Common.mLastLocation.getLatitude();
//                    final double longitude = Common.mLastLocation.getLongitude();


                    loadAllAvailableDriver(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));

                } else {
                    Log.d("Error", "Cannot get your location");
                }
            }
        });

    }

    private void loadAllAvailableDriver(final LatLng location) {

        mMap.clear();
        mUserMarker = mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                .position(location)
                .title("Your Location"));


        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f));

        //load all available driver in distance 3kms
        DatabaseReference driverLocation;
        if (isX)
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tb1).child("X");
        else
            driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tb1).child("Black");
        GeoFire gf = new GeoFire(driverLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(location.latitude, location.longitude), distance);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {

                FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                Rider rider = dataSnapshot.getValue(Rider.class);
                                if (isX) {
                                    assert rider != null;
                                    if (rider.getCarType().equals("X")) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
                                                .flat(true)
                                                .title(rider.getName())
                                                .snippet("Driver ID : " + dataSnapshot.getKey())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                    }
                                } else {
                                    if (rider.getCarType().equals("Black")) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(location.latitude, location.longitude))
                                                .flat(true)
                                                .title(rider.getName())
                                                .snippet("Driver ID : " + dataSnapshot.getKey())
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));
                                    }
                                }


                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (distance <= LIMIT) {
                    distance++;
                    loadAllAvailableDriver(location);
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

//        mMap.clear();
//        mMap.addMarker(new MarkerOptions().position(location)
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
//                .title("you"));
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//         Handle action bar item clicks here. The action bar will
//         automatically handle clicks on the Home/Up button, so long
//         as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        if (id == R.id.nav_sign_out) {
            signOut();
        } else if (id == R.id.updateInformation) {
            showUpdateInformationDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showUpdateInformationDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Home.this);
        alertDialog.setTitle("Update Information");
        alertDialog.setMessage("Please Fill All Information");
        LayoutInflater inflater = this.getLayoutInflater();
        View layout_update_information = inflater.inflate(R.layout.layout_update_information, null);
        final MaterialEditText edtName = layout_update_information.findViewById(R.id.edtname);
        final MaterialEditText edtPhone = layout_update_information.findViewById(R.id.edtphone);
        final ImageView imgAvatar = layout_update_information.findViewById(R.id.image_upload);

        alertDialog.setView(layout_update_information);

        imgAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImageAndUpload();
            }
        });

        alertDialog.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {

                dialogInterface.dismiss();
                final android.app.AlertDialog waitingDialog = new SpotsDialog.Builder().setContext(Home.this).build();
                waitingDialog.setCancelable(false);
                waitingDialog.show();


                AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                    @Override
                    public void onSuccess(Account account) {
                        String name = edtName.getText().toString();
                        String phone = edtPhone.getText().toString();

                        Map<String, Object> updateInfo = new HashMap<>();
                        if (!TextUtils.isEmpty(name))
                            updateInfo.put("name", name);
                        if (!TextUtils.isEmpty(phone))
                            updateInfo.put("phone", phone);
                        DatabaseReference riderinformation = FirebaseDatabase.getInstance().getReference(Common.user_rider_tb1);
                        riderinformation.child(account.getId())
                                .updateChildren(updateInfo)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful())
                                            Toast.makeText(Home.this, "Information Updated !", Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(Home.this, "Information update failed !", Toast.LENGTH_SHORT).show();

                                        waitingDialog.dismiss();

                                    }
                                });
                    }

                    @Override
                    public void onError(AccountKitError accountKitError) {

                    }
                });
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {

                dialogInterface.dismiss();
            }
        });

        alertDialog.show();
    }

    private void chooseImageAndUpload() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select picture"), Common.PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Common.PICK_IMAGE_REQUEST && requestCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri saveUri = data.getData();
            if (saveUri != null) {
                final ProgressDialog mDialog = new ProgressDialog(this);
                mDialog.setMessage("Uploading....");
                mDialog.show();
                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder = storageReference.child("image/" + imageName);
                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                                mDialog.dismiss();
                                Toast.makeText(Home.this, "Uploaded !", Toast.LENGTH_SHORT).show();
                                imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(final Uri uri) {

                                        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                                            @Override
                                            public void onSuccess(Account account) {
                                                Map<String, Object> avatarUpdate = new HashMap<>();
                                                avatarUpdate.put("avatarUrl", uri.toString());

                                                DatabaseReference riderInformation = FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1);
                                                riderInformation.child(account.getId())
                                                        .updateChildren(avatarUpdate)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                if (task.isSuccessful())
                                                                    Toast.makeText(Home.this, "Uploaded !", Toast.LENGTH_SHORT).show();
                                                                else
                                                                    Toast.makeText(Home.this, "Uploaded error !", Toast.LENGTH_SHORT).show();

                                                            }
                                                        });

                                            }

                                            @Override
                                            public void onError(AccountKitError accountKitError) {

                                            }
                                        });
                                    }
                                });
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                mDialog.setMessage("Uploaded" + progress + "%");
                            }
                        });
            }
        }

    }

    private void signOut() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        else
            builder = new AlertDialog.Builder(this);

        builder.setMessage("Do you Want To LogOut??")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AccountKit.logOut();
                        Intent intent = new Intent(Home.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.show();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        try {
            boolean isSuccess = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_style_map));
            if (!isSuccess)
                Log.e("ERROR", "My Style Load Failed");
        } catch (Resources.NotFoundException ex) {
            ex.printStackTrace();
        }

        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        mMap.setInfoWindowAdapter(new CustomInfoWindow(this));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (markerDestination != null)
                    markerDestination.remove();

                markerDestination = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destination_marker))
                        .position(latLng)
                        .title("Destination"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));

                //show bottom sheet
                BottomSheetRiderFragment mBottomSheet = BottomSheetRiderFragment.
                        newInstance(String.format("%f,%f", Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()),
                                String.format("%f,%f", latLng.latitude, latLng.longitude), true);
                mBottomSheet.show(getSupportFragmentManager(), mBottomSheet.getTag());

            }
        });

        mMap.setOnInfoWindowClickListener(this);

        if (ActivityCompat.checkSelfPermission(Home.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(Home.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        if (!marker.getTitle().equals("Pickup Here") && !marker.getTitle().equals("Your Location") && !marker.getTitle().equals("Destination") ) {
            Intent intent = new Intent(Home.this, CallDriver.class);
            intent.putExtra("driverId", marker.getSnippet().replaceAll("\\D+", ""));
            intent.putExtra("lat", Common.mLastLocation.getLatitude());
            intent.putExtra("lng", Common.mLastLocation.getLongitude());
            startActivity(intent);

        }
    }

    @Override
    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        loadAllAvailableDriver(new LatLng(Common.mLastLocation.getLatitude(), Common.mLastLocation.getLongitude()));
    }

    @Override
    public void onCancelled(@NonNull DatabaseError databaseError) {

    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCancelBroadCast);
        super.onDestroy();
    }
}
