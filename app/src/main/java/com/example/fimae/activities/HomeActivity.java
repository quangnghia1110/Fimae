package com.example.fimae.activities;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.MenuItem;

import com.example.fimae.R;
import com.example.fimae.adapters.ViewPagerAdapter;
import com.example.fimae.service.CallService;
import com.example.fimae.models.DisableUser;
import com.example.fimae.service.UpdateUserActivityTimeService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.Date;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView mNavigationView;
    private ViewPager2 mViewPager;
    public static String PACKAGE_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        listenDisable();
        PACKAGE_NAME = getApplicationContext().getPackageName();

        mNavigationView = findViewById(R.id.bottom_nav);
        mViewPager = findViewById(R.id.view_paper);
        setUpViewPager();

        mNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_home:
                        mViewPager.setCurrentItem(0, false);
                        break;
                    case R.id.action_feed:
                        mViewPager.setCurrentItem(1, false);
                        break;
                    case R.id.action_chat:
                        mViewPager.setCurrentItem(2, false);
                        break;
                    case R.id.action_profile:
                        mViewPager.setCurrentItem(3, false);
                        break;
                }
                return true;
            }
        });
        Intent intent = new Intent(this, UpdateUserActivityTimeService.class);
        startService(intent);

        CallService.getInstance().initStringeeConnection(getBaseContext(), HomeActivity.this);
        CallService.getInstance().addListener(new CallService.CallClientListener() {
            @Override
            public void onStatusChange(String status) {

            }

            @Override
            public void onIncomingCallVoice(String typeCall, String callId) {
                if(typeCall.equals(CallService.NORMAL)) {
                    Intent intent = new Intent(HomeActivity.this, CallOnChatActivity.class);
                    intent.putExtra("callId", callId);
                    intent.putExtra("isIncomingCall", true);
                    startActivity(intent);
                }
            }

            @Override
            public void onIncomingCallVideo(String typeCall, String callId) {
                if(typeCall.equals(CallService.NORMAL)) {
                    Intent intent = new Intent(HomeActivity.this, CallVideoActivity.class);
                    intent.putExtra("callId", callId);
                    intent.putExtra("isIncomingCall", true);
                    startActivity(intent);
                }
            }
        });
    }
    private void listenDisable(){
        if(FirebaseAuth.getInstance().getUid() == null){
            Intent intent = new Intent(HomeActivity.this, AuthenticationActivity.class);
            startActivity(intent);
        }
        FirebaseFirestore.getInstance().collection("user_disable")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid())+"USER")
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            // Document exists, check if the user is disabled
                            DisableUser disableUser = snapshot.toObject(DisableUser.class);
                            if (disableUser != null && disableUser.getTimeEnd().after(new Date()) && disableUser.getType() != null &&  disableUser.getType().equals("USER")) {
//                                Intent intent = new Intent(HomeActivity.this, AuthenticationActivity.class);
//                                intent.putExtra("signout", true);
//                                startActivity(intent);
                                Intent intent = new Intent(HomeActivity.this, DisabledUserActivity.class);
                                intent.putExtra( "disableId", disableUser.getUserId());
                                intent.putExtra("type", "USER");
                                startActivity(intent);
                                finish();
                            }
                        }
                    }
                });
    }

    void init(){

}
    private void setUpViewPager() {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this);
        mViewPager.setAdapter(viewPagerAdapter);
        //turn off smooth scroll
        mViewPager.setUserInputEnabled(false);

        mViewPager.setOffscreenPageLimit(5);
        //Add listener to change icon when page change
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });
    }


}
