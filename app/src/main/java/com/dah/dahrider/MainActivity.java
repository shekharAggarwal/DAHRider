package com.dah.dahrider;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.dah.dahrider.Common.Common;
import com.dah.dahrider.Model.Rider;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CODE = 99;
    Button btnContinue;
    RelativeLayout rootLayout;
    FirebaseDatabase db;
    DatabaseReference users;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
        setContentView(R.layout.activity_main);

        Paper.init(this);


        rootLayout = findViewById(R.id.rootLayout);
        db = FirebaseDatabase.getInstance();
        users = db.getReference(Common.user_rider_tb1);
        btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithPhone();
            }
        });

        if (AccountKit.getCurrentAccessToken() != null) {
            final android.app.AlertDialog waitingDialog = new SpotsDialog.Builder().setContext(this).build();
            waitingDialog.setMessage("Please waiting...");
            waitingDialog.setCancelable(false);
            waitingDialog.show();

            AccountKit.getCurrentAccount(new AccountKitCallback<com.facebook.accountkit.Account>() {
                @Override
                public void onSuccess(com.facebook.accountkit.Account account) {
                    users.child(account.getId())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Common.currentUser = dataSnapshot.getValue(Rider.class);

                                    Intent homeIntent = new Intent(MainActivity.this, Home.class);
                                    startActivity(homeIntent);

                                    waitingDialog.dismiss();
                                    finish();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                }

                @Override
                public void onError(AccountKitError accountKitError) {

                }
            });
        }

    }

    private void signInWithPhone() {

        Intent intent = new Intent(MainActivity.this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(LoginType.PHONE, AccountKitActivity.ResponseType.TOKEN);
        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, configurationBuilder.build());
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
            if (result.getError() != null) {
                Toast.makeText(this, "" + result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();
                return;
            } else if (result.wasCancelled()) {
                Toast.makeText(this, "Cancel login", Toast.LENGTH_SHORT).show();
                return;
            } else {
                if (result.getAccessToken() != null) {
                    final android.app.AlertDialog waitingDialog = new SpotsDialog.Builder().setContext(this).build();
                    waitingDialog.setMessage("Please waiting.....");
                    waitingDialog.setCancelable(false);
                    waitingDialog.show();
                    AccountKit.getCurrentAccount(new AccountKitCallback<com.facebook.accountkit.Account>() {
                        @Override
                        public void onSuccess(final com.facebook.accountkit.Account account) {

                            users.orderByKey().equalTo(account.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (!dataSnapshot.child(account.getId()).exists()) {
                                        final Rider user = new Rider();
                                        user.setPhone(account.getPhoneNumber().toString());
                                        user.setName(account.getPhoneNumber().toString());
                                        user.setAvatarUrl("");
                                        user.setRates("0.0");


                                        users.child(account.getId())
                                                .setValue(user)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        users.child(account.getId())
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                        Common.currentUser = dataSnapshot.getValue(Rider.class);

                                                                        Intent homeIntent = new Intent(MainActivity.this, Home.class);
                                                                        startActivity(homeIntent);

                                                                        waitingDialog.dismiss();
                                                                        finish();
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                    }
                                                                });


                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                                            }
                                        });
                                    } else {
                                        users.child(account.getId())
                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                        Common.currentUser = dataSnapshot.getValue(Rider.class);

                                                        Intent homeIntent = new Intent(MainActivity.this, Home.class);
                                                        startActivity(homeIntent);

                                                        waitingDialog.dismiss();
                                                        finish();
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

                        @Override
                        public void onError(AccountKitError accountKitError) {

                            Toast.makeText(MainActivity.this, "" + accountKitError.getErrorType().getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
                }
            }
        }
    }
}
