package com.dah.dahrider;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dah.dahrider.Common.Common;
import com.dah.dahrider.Model.Rate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import me.zhanghai.android.materialratingbar.MaterialRatingBar;

public class RateActivity extends AppCompatActivity {

    Button btnSubmit;
    MaterialRatingBar ratingBar;
    MaterialEditText edtComment;

    FirebaseDatabase database;
    DatabaseReference rateDetailRef;
    DatabaseReference driverInformationRef;

    double ratingStars = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate);

        database = FirebaseDatabase.getInstance();
        rateDetailRef = database.getReference(Common.rate_detail_tb1);
        driverInformationRef = database.getReference(Common.user_driver_tb1);


        btnSubmit = findViewById(R.id.btnsubmit);
        ratingBar = findViewById(R.id.ratingBar);
        edtComment = findViewById(R.id.edtComment);

        ratingBar.setOnRatingChangeListener(new MaterialRatingBar.OnRatingChangeListener() {
            @Override
            public void onRatingChanged(MaterialRatingBar ratingBar, float rating) {
                ratingStars = rating;
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitRateDetails(Common.driverId);
            }
        });

    }

    private void submitRateDetails(final String driverId) {
        final android.app.AlertDialog alertDialog = new SpotsDialog.Builder().setContext(this).build();
        alertDialog.setCancelable(false);
        alertDialog.show();
        final Rate rate = new Rate();
        rate.setRates(String.valueOf(ratingStars));
        rate.setComments(edtComment.getText().toString());

        rateDetailRef.child(driverId)
                .push()
                .setValue(rate)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        rateDetailRef.child(driverId).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Double averageStars = 0.0;
                                int count = 0;
                                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                                    Rate rate = postSnapshot.getValue(Rate.class);
                                    averageStars += Double.parseDouble(rate.getRates());
                                    count++;
                                }

                                final Double finalAverage = averageStars / count;
                                DecimalFormat df = new DecimalFormat("#.#");
                                String valueUpdate = df.format(finalAverage);
                                Map<String, Object> driverUpdateRate = new HashMap<>();
                                driverUpdateRate.put("Rates", valueUpdate);
                                driverInformationRef.child(Common.driverId)
                                        .updateChildren(driverUpdateRate)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                alertDialog.dismiss();
                                                Toast.makeText(RateActivity.this, "Thank You For Submit", Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                alertDialog.dismiss();
                                                Toast.makeText(RateActivity.this, "Rate Updated But Can't Write To Driver Information", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        alertDialog.dismiss();
                        Toast.makeText(RateActivity.this, "Rate Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
