package com.example.fimae.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fimae.R;
import com.example.fimae.fragments.ProfileFragment;
import com.example.fimae.models.Follows;
import com.example.fimae.repository.FollowRepository;
import com.example.fimae.repository.PostRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String uid = getIntent().getStringExtra("uid");
        setContentView(R.layout.activity_profile);
        Button follow = findViewById(R.id.followButton);
        Button chat = findViewById(R.id.startChat);
        if(uid.equals(FirebaseAuth.getInstance().getUid())){
            follow.setVisibility(View.GONE);
            chat.setVisibility(View.GONE);
        }else{
            FollowRepository.getInstance().followRef.document(FirebaseAuth.getInstance().getUid()+"_"+ uid).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                    if(error != null || value == null){
                        return;
                    }
                    Follows follows = value.toObject(Follows.class);
                    if(follows != null){
                        follow.setText("Bỏ theo dõi");
                        chat.setVisibility(View.VISIBLE);
                    }
                    else{
                        chat.setVisibility(View.GONE);
                        follow.setVisibility(View.VISIBLE);
                        follow.setText("Theo dõi");
                    }
                }
            });

        }

        follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(follow.getText().equals("Theo dõi")){
                    // Thực hiện theo dõi người dùng và gửi tin nhắn mặc định
                    FollowRepository.getInstance().followWithDefaultMessage(uid);
                }
                else {
                    // Thực hiện bỏ theo dõi người dùng
                    FollowRepository.getInstance().unFollowWithDefaultMessage(uid);
                }
            }
        });


        chat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PostRepository.getInstance().goToChatWithUser(uid, ProfileActivity.this);
            }
        });
        if (savedInstanceState == null) {
            ProfileFragment profileFragment = ProfileFragment.newInstance(uid);
            // Add the initial fragment when the activity is first created
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, profileFragment)
                    .commit();
        }
    }
}
