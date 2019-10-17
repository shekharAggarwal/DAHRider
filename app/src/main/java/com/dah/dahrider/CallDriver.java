package com.dah.dahrider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dah.dahrider.Common.Common;
import com.dah.dahrider.Model.Rider;
import com.dah.dahrider.Remote.IFCMService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CallDriver extends AppCompatActivity {
    CircleImageView avatar_image;
    TextView txt_name, txt_phone, txt_rate;
    Button btn_call_driver, getBtn_call_driver_phone;

    String driverId;
    Location mLastLocation;

    IFCMService mService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_driver);

        mService = Common.getFCMService();
        avatar_image = findViewById(R.id.avatar_image);
        txt_name = findViewById(R.id.txt_Name);
        txt_phone = findViewById(R.id.txt_Phone);
        txt_rate = findViewById(R.id.txt_Rate);
        btn_call_driver = findViewById(R.id.btn_Call_Driver);
        getBtn_call_driver_phone = findViewById(R.id.btn_Call_Driver_Phone);
        btn_call_driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (driverId != null && !driverId.isEmpty())
                    Common.sendRequestToDriver(driverId, mService, getBaseContext(), mLastLocation);
            }
        });
        getBtn_call_driver_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + txt_phone.getText().toString()));
                if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                startActivity(intent);
            }
        });
        if (getIntent() != null) {
            driverId = getIntent().getStringExtra("driverId");
            double lat = getIntent().getDoubleExtra("lat", -1.0);
            double lng = getIntent().getDoubleExtra("lng", -1.0);
            mLastLocation = new Location("");
            mLastLocation.setLatitude(lat);
            mLastLocation.setLongitude(lng);
            loadDriverInfo(driverId);
        }

    }

    private void loadDriverInfo(final String driverId) {
        if (driverId != null && !driverId.isEmpty())
            FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1).child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Rider driverUser = dataSnapshot.getValue(Rider.class);

                    assert driverUser != null;
                    if (!driverUser.getAvatarUrl().isEmpty()) {
                        Picasso.get().load(driverUser.getAvatarUrl()).into(avatar_image);
                    }
                    txt_name.setText(driverUser.getName());
                    txt_phone.setText(driverUser.getPhone());
                    txt_rate.setText(driverUser.getRates());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
    }
}
