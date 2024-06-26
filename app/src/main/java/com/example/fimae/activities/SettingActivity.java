package com.example.fimae.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.fimae.R;
import com.example.fimae.repository.AuthRepository;

public class SettingActivity extends AppCompatActivity {

    RelativeLayout editProfile, relativeSignOut, relativePrivacy, relativeCommunity, relativeChangePassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        relativePrivacy = findViewById(R.id.relativePrivacy);
        relativeCommunity = findViewById(R.id.relativeCommunity);
        relativeCommunity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, PrivacyActivity.class);
                intent.putExtra("url", "file:///android_asset/community.html");
                startActivity(intent);
            }
        });
        relativePrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingActivity.this, PrivacyActivity.class);
                intent.putExtra("url", "file:///android_asset/privacy.html");
                startActivity(intent);
            }
        });
        editProfile = findViewById(R.id.relativeEditProfile);
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navToEditProfile();
            }
        });
        relativeSignOut = findViewById(R.id.relativeLogout);
        relativeSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logOut();
            }
        });

        relativeChangePassword = findViewById((R.id.relativeChangePassword));
        relativeChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navToChangePassword();
            }
        });

    }
    private void logOut()
    {
        AuthRepository.getInstance().signOut();

        finish();
    }
    private void navToEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
    }
    private void navToChangePassword() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        startActivity(intent);
    }
}