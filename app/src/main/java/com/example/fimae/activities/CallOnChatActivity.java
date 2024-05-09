
package com.example.fimae.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.fimae.R;
import com.example.fimae.databinding.ActivityCallOnChatBinding;
import com.example.fimae.repository.ConnectRepo;
import com.example.fimae.service.CallService;
import com.squareup.picasso.Picasso;
import com.stringee.call.StringeeCall;
import com.stringee.common.StringeeAudioManager;
import com.stringee.listener.StatusListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CallOnChatActivity extends AppCompatActivity {
    ActivityCallOnChatBinding binding;

    private MediaPlayer ringtonePlayer;

    private StringeeCall call;

    private boolean isInComingCallVideo = false;
    private String to;
    private String callId;

    private StringeeCall.SignalingState mSignalingState;
    private StringeeCall.MediaState mMediaState;

    // audio
    private StringeeAudioManager audioManager;

    // check trang thai speaker and mic
    private boolean isSpeaker = false;
    private boolean isMicOn = true;
    private boolean isVideoOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallOnChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // set image user
        if(ConnectRepo.getInstance().getUserLocal() != null){
            Picasso.get().load(ConnectRepo.getInstance().getUserLocal().getAvatarUrl()).placeholder(R.drawable.ic_default_avatar).into(binding.imgAvatarLocal);
        }
        if(ConnectRepo.getInstance().getUserRemote() != null){
            Picasso.get().load(ConnectRepo.getInstance().getUserRemote().getAvatarUrl()).placeholder(R.drawable.ic_default_avatar).into(binding.imgAvatarRemote);
        }
        if(ConnectRepo.getInstance().getUserLocal() != null){
            binding.tvNameRemote.setText(ConnectRepo.getInstance().getUserRemote().getName());
        }
        binding.btnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    if(audioManager != null) {
                        audioManager.setSpeakerphoneOn(!isSpeaker);
                        isSpeaker = !isSpeaker;
                        binding.btnSpeaker.setBackgroundResource(isSpeaker? R.drawable.background_btn_speaker_on : R.drawable.background_btn_speaker_off);
                    }
                });
            }
        });

        binding.btnMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    if(call != null){
                        call.mute(isMicOn);
                        isMicOn = !isMicOn;
                        binding.btnMute.setBackgroundResource(isMicOn? R.drawable.background_btn_mic_on : R.drawable.background_btn_mic_off);
                    }
                });
            }
        });
        binding.btnAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    if(call != null){
                        call.answer(new StatusListener() {
                            @Override
                            public void onSuccess() {
                                releaseMediaPlayer();
                            }
                        });
                        binding.vIncoming.setVisibility(View.GONE);
                        binding.vOption.setVisibility(View.VISIBLE);
                        binding.btnEnd.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
        binding.btnReject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    if(call != null){
                        call.reject(new StatusListener() {
                            @Override
                            public void onSuccess() {
                                releaseMediaPlayer();
                            }
                        });
                        onFinish();
                    }
                });
            }
        });

        binding.btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                releaseMediaPlayer();onEndCall();
            }
        });


        if(getIntent() != null){
            isInComingCallVideo = getIntent().getBooleanExtra("isIncomingCall", false);
            to = getIntent().getStringExtra("to");
            // duoc goi
            callId = getIntent().getStringExtra("callId");
        }

        // kiem tra dang goi den
        binding.vIncoming.setVisibility(isInComingCallVideo? View.VISIBLE : View.GONE);
        binding.vOption.setVisibility(isInComingCallVideo? View.GONE: View.VISIBLE);
        binding.btnEnd.setVisibility(isInComingCallVideo? View.GONE: View.VISIBLE);

        // list permission
        List<String> listPermission = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // add permisson
            listPermission.add(Manifest.permission.RECORD_AUDIO);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // add permisson
            listPermission.add(Manifest.permission.CAMERA);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // add permisson
                listPermission.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if(listPermission.size() > 0){
            String[] permissions = new String[listPermission.size()];
            for(int i = 0; i < listPermission.size(); i++){
                permissions[i] = listPermission.get(i);
            }
            ActivityCompat.requestPermissions(this, permissions, 0);
            return;
        }
        initCall();
    }

    private void onFinish() {
        //WaitingActivity.isCalled = false;
        finish();
    }

    private void onEndCall(){
        if(call != null){
            call.hangup(new StatusListener(){
                @Override
                public void onSuccess() {
                }
            });
            onFinish();
        }
    }

    // lay token de thuc hien cuoc goi
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
// lay quyen audio
        boolean isGranted = false;
        if(grantResults.length > 0){
            for(int grantResult : grantResults){
                // check trang thai quyen nguoi dung cho phep
                if(grantResult != PackageManager.PERMISSION_GRANTED){
                    isGranted = false;
                    break;
                }else{
                    isGranted = true;
                }
            }
        }
        // check nguoi dung cap quyen chua
        if(requestCode == 0){
            if(!isGranted){
                onFinish();
            } else {
                initCall();
            }
        }
    }

    private void initCall(){
        if(isInComingCallVideo){
            // cuoc goi den
            call = CallService.getInstance().callMap.get(callId);
            if( call == null){
                onFinish();
                return;
            }
        }else{
            // tao cuoc goi moi
            call = new StringeeCall(CallService.getInstance().client, CallService.getInstance().client.getUserId(), to);
            call.setVideoCall(true);
            call.setCustom(CallService.NORMAL);
        }

        // theo doi trang thai cuoc goi
        call.setCallListener(new StringeeCall.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String s, int i, String s1) {
                // trang thai dieu huong cuoc goi
                // khi nao bat dau, ket thuc
                runOnUiThread(()->{
                    mSignalingState = signalingState;
                    switch (signalingState) {
                        case CALLING:
                            binding.tvStatus.setText("Đang gọi");
                            break;
                        case RINGING:
                            binding.tvStatus.setText("Đang đổ chuông");
                            break;
                        case ANSWERED:
                            binding.tvStatus.setText("Đang trả lời");
                            releaseMediaPlayer();
                            // cuoc goi bat dau
                            if(mMediaState == StringeeCall.MediaState.CONNECTED){
                                binding.tvStatus.setText("");
                            }
                            break;
                        case BUSY:
                            binding.tvStatus.setText("Máy bận");
                            releaseMediaPlayer();
                            onFinish();
                            break;
                        case ENDED:
                            binding.tvStatus.setText("Kết thúc");
                            releaseMediaPlayer();
                            onFinish();
                            break;

                    }
                });
            }

            @Override
            public void onError(StringeeCall stringeeCall, int i, String s) {
                // cuoc goi bi loi
                runOnUiThread(()->{
                    binding.tvStatus.setText("Lỗi đường truyền");
                    onFinish();
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String s) {

            }

            @Override
            public void onMediaStateChange(StringeeCall stringeeCall, StringeeCall.MediaState mediaState) {
                // khi nao co media connected
                runOnUiThread(()->{
                    mMediaState = mediaState;
                    if(mediaState == StringeeCall.MediaState.CONNECTED){
                        if(mSignalingState == StringeeCall.SignalingState.ANSWERED){
                            binding.tvStatus.setText("");
                        }
                    }else{
                        // mat ket noi
                        binding.tvStatus.setText("Đang kết nối lại");
                    }
                });
            }
            @Override
            public void onLocalStream(StringeeCall stringeeCall) {
                runOnUiThread(() -> {

                    stringeeCall.renderLocalView(true);
                });
            }

            @Override
            public void onRemoteStream(StringeeCall stringeeCall) {
                runOnUiThread(() -> {

                    stringeeCall.renderRemoteView(false);
                });
            }

            @Override
            public void onCallInfo(StringeeCall stringeeCall, JSONObject jsonObject) {
                // Xử lý thông tin cuộc gọi, bao gồm cả tin nhắn
                try {
                    String callInfoType = jsonObject.getString("type");

                    if (callInfoType.equals("message")) {
                        String fromUserId = jsonObject.getString("from");
                        String messageContent = jsonObject.getString("content");

                        // Hiển thị tin nhắn
                        showToast("Received message from " + fromUserId + ": " + messageContent);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        // manage audio
        audioManager = new StringeeAudioManager(this);
        audioManager.start((audioDevice, set) -> {
            // start audio
        });

        audioManager.setSpeakerphoneOn(true);
        // khoi tao cuoc goi
        if(isInComingCallVideo){
            // do chuong goi
            call.ringing(new StatusListener() {
                @Override
                public void onSuccess() {
                    playSound();

                }
            });
        }else{
            call.makeCall(new StatusListener() {
                @Override
                public void onSuccess() {
                    releaseMediaPlayer();

                }
            });
        }
    }
    private void playSound() {
        // Check if the MediaPlayer is null or not playing
        if (ringtonePlayer == null || !ringtonePlayer.isPlaying()) {
            // Create and start the MediaPlayer
            ringtonePlayer = MediaPlayer.create(CallOnChatActivity.this, R.raw.ring); // Replace R.raw.ring with your sound file
            ringtonePlayer.setLooping(true); // Set looping to true if you want the sound to repeat
            ringtonePlayer.start();
        }
    }

    private void releaseMediaPlayer() {
        if (ringtonePlayer != null) {
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
    }
    private void showToast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }
}