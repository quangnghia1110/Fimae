package com.example.fimae.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fimae.R;
import com.example.fimae.adapters.BottomSheetItemAdapter;
import com.example.fimae.adapters.MessageAdapter;
import com.example.fimae.fragments.FimaeBottomSheet;
import com.example.fimae.fragments.MediaListDialogFragment;
import com.example.fimae.models.BottomSheetItem;
import com.example.fimae.models.Conversation;
import com.example.fimae.models.Fimaers;
import com.example.fimae.models.Message;
import com.example.fimae.repository.ChatRepository;
import com.example.fimae.repository.ConnectRepo;
import com.example.fimae.utils.FileUtils;
import com.example.fimae.utils.FirebaseHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OnChatActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;

    public static String currentConversationId;

    private CollectionReference messagesCol;
    private String conversationId;
    private EditText textInput;
    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private LinearLayoutManager linearLayoutManager;
    private LinearLayout inputMediaLayout;
    private Uri photoUri;
    private List<BottomSheetItem> bottomSheetItemList;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_second_menu, menu);
        return true;
    }
    Fimaers fimaer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_chat);
        conversationId = getIntent().getStringExtra("conversationID");
        fimaer = (Fimaers) getIntent().getSerializableExtra("fimaer");
        currentConversationId = conversationId;
        messagesCol = FirebaseFirestore.getInstance().collection("conversations").document(conversationId).collection("messages");
        initViews();
        initBottomSheetItems();
        initListeners();
        initMessagesListener();
        TextView nameTextView = findViewById(R.id.status_name);

        // Kiểm tra xem đối tượng fimaer có tồn tại hay không
        if (fimaer != null) {
            // Lấy tên của người được nhắn từ đối tượng fimaer và đặt nội dung cho TextView
            String userName = fimaer.getName(); // Giả sử getName() là phương thức để lấy tên
            nameTextView.setText(userName);
        }
        // call
        mTvStatusConnect = findViewById(R.id.status_appbar);
        try{
            if (fimaer != null) {
                String status = "Không hoạt động";
                if (fimaer.isOnline()) {
                    status = "Đang hoạt động";
                } else {
                    int lastActiveMinutes = fimaer.getLastActiveMinuteAgo();
                    if (lastActiveMinutes <= 60) {
                        status = "Hoạt động " + lastActiveMinutes + " phút trước";
                    }
                    else if(lastActiveMinutes >1440){
                        status = "Hoạt động " + lastActiveMinutes/1440 + " ngày trước";
                    }
                    else {
                        // Chuyển đổi phút thành giờ và phút
                        int hours = lastActiveMinutes / 60;
                        int minutes = lastActiveMinutes % 60;
                        status = "Hoạt động " + hours + " giờ ";
                    }
                }
                mTvStatusConnect.setText(status);
            }

        }catch (Exception e) {
            mTvStatusConnect.setText("Không hoạt động");
        }
        //initStringeeConnection();
        getRemoteUserId();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setTitle("");
        }
        recyclerView = findViewById(R.id.list_messages);

        linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);
        ImageView btnAddMedia = findViewById(R.id.btn_add_media);
        inputMediaLayout = findViewById(R.id.input_media_layout);
        inputMediaLayout.setVisibility(View.GONE);
        ImageView btnSend = findViewById(R.id.btn_send);
        textInput = findViewById(R.id.random_chat_et_input);
        ImageView btnCamera = findViewById(R.id.btn_camera);
        ImageView btnGallery = findViewById(R.id.btn_galllery);
        ImageView btnMicro = findViewById(R.id.btn_micro);
    }

    private void initBottomSheetItems() {
        bottomSheetItemList = new ArrayList<BottomSheetItem>() {
            {
                add(new BottomSheetItem("Delete", R.drawable.ic_delete, "Xóa cuộc trò chuyện"));
//                add(new BottomSheetItem("Media", R.drawable.ic_gallery, "Tất cả ảnh", R.drawable.ic_mic_on));
//                add(new BottomSheetItem("EditNickname", R.drawable.ic_edit, "Chỉnh sửa biệt danh"));
//                add(new BottomSheetItem("Block", R.drawable.ic_user_block, "Chặn"));
//                add(new BottomSheetItem("Report", R.drawable.ic_flag, "Báo cáo"));
            }
        };
    }

    private void initListeners() {
        ImageView btnAddMedia = findViewById(R.id.btn_add_media);
        btnAddMedia.setOnClickListener(v -> {
            inputMediaLayout.setVisibility(inputMediaLayout.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        });

        ImageView btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(v -> {
            sendTextMessage();
        });

        ImageView btnCamera = findViewById(R.id.btn_camera);
        btnCamera.setOnClickListener(v -> {
            dispatchTakePictureIntent();
        });

        ImageView btnGallery = findViewById(R.id.btn_galllery);
        btnGallery.setOnClickListener(v -> {
            MediaListDialogFragment mediaListDialogFragment = new MediaListDialogFragment();
            mediaListDialogFragment.setOnMediaSelectedListener(new MediaListDialogFragment.OnMediaSelectedListener() {
                @Override
                public void OnMediaSelected(boolean isSelected, ArrayList<String> data) {
                    if (isSelected) {
                        ArrayList<Uri> uris = new ArrayList<>();
                        data.forEach(e -> uris.add(Uri.parse(e)));
                        sendMediaMessage(uris);
                    }
                }
            });
            mediaListDialogFragment.show(getSupportFragmentManager(), "dialog");
        });

        ImageView btnMicro = findViewById(R.id.btn_micro);

        // Bottom sheet menu
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            switch (id) {
                case R.id.option_more:
                    FimaeBottomSheet fimaeBottomSheet = new FimaeBottomSheet(bottomSheetItemList,
                            new BottomSheetItemAdapter.IClickBottomSheetItemListener() {
                                @Override
                                public void onClick(BottomSheetItem bottomSheetItem) {
                                    switch (bottomSheetItem.getTag()){
                                        case "Delete":
                                            ChatRepository.getDefaultChatInstance().deleteConversation(conversationId);
                                            finish();
                                            break;
                                    }
                                }
                            });
                    fimaeBottomSheet.show(getSupportFragmentManager(), fimaeBottomSheet.getTag());
                    return true;
                case R.id.option_call:
                    initCall();
                    return true;
                case R.id.option_call_video:
                    initCallVideo();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);

            }
        });
    }

    private void initMessagesListener() {
        Query query = messagesCol.orderBy("sentAt", Query.Direction.ASCENDING);
        messageAdapter = new MessageAdapter(query, this);
        recyclerView.setAdapter(messageAdapter);
        messageAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
            }
        });
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void sendTextMessage() {
        if (textInput.getText().toString().trim().isEmpty())
            return;
        ChatRepository.getDefaultChatInstance().sendTextMessage(conversationId, String.valueOf(textInput.getText()));
        textInput.setText("");
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void sendMediaMessage(ArrayList<Uri> uris) {
        inputMediaLayout.setVisibility(View.GONE);
        ChatRepository.getDefaultChatInstance().sendMediaMessage(conversationId, uris).addOnCompleteListener(task -> {
            recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        });
    }

    private void sendMediaMessage(byte[] bytes) {
        inputMediaLayout.setVisibility(View.GONE);
        FirebaseHelper firebaseHelper = new FirebaseHelper();
        firebaseHelper.uploadBytesToFirebase(bytes, this::handleMediaMessageUpload, "conversation-medias", conversationId);
    }

    private void handleMediaMessageUpload(ArrayList<String> downloadUrls) {
        Message message = createMediaMessage(downloadUrls);
        sendMessage(message);
        textInput.setText("");
    }

    private Message createMediaMessage(ArrayList<String> downloadUrls) {
        Message message = new Message();
        message.setId(messagesCol.document().getId());
        message.setType(Message.MEDIA);
        message.setContent(downloadUrls);
        message.setConversationID(conversationId);
        message.setIdSender(FirebaseAuth.getInstance().getUid());
        return message;
    }

    private void sendMessage(Message message) {
        recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
        messagesCol.document(message.getId()).set(message)
                .addOnFailureListener(e -> {
                    recyclerView.scrollToPosition(messageAdapter.getItemCount() - 1);
                });
    }

    private boolean requestCameraPermission() {
       if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
           requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);
           return false;
       }
         return true;
    }

    private void dispatchTakePictureIntent() {
        if (!requestCameraPermission()) {
            return;
        }
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data.getExtras() != null) {
                Bitmap photoBitmap = (Bitmap) data.getExtras().get("data");
                byte[] bitmapBytes = FileUtils.getBytesFromBitmap(photoBitmap, 100);
                sendMediaMessage(bitmapBytes);
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.option_more) {
            FimaeBottomSheet fimaeBottomSheet = new FimaeBottomSheet(bottomSheetItemList,
                    new BottomSheetItemAdapter.IClickBottomSheetItemListener() {
                        @Override
                        public void onClick(BottomSheetItem bottomSheetItem) {

                        }
                    });
            fimaeBottomSheet.show(getSupportFragmentManager(), fimaeBottomSheet.getTag());
        } else if (id == R.id.option_call) {
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentConversationId = null;
        messageAdapter.stopListening();
    }

    // goi dien ====================================================
    private String remoteUserToken;
    private TextView mTvStatusConnect;
    public static StringeeClient client;

    public static Map<String, StringeeCall> callMap = new HashMap<>();

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void initCall() {
        Intent intent = new Intent(this, CallOnChatActivity.class);
        intent.putExtra("to", remoteUserToken);
        intent.putExtra("isIncomingCall", false);
        startActivity(intent);
    }

    private void initCallVideo() {
        Intent intent = new Intent(this, CallVideoActivity.class);
        intent.putExtra("to", remoteUserToken);
        intent.putExtra("isIncomingCall", false); // Đảm bảo cài đặt đúng cho cuộc gọi ra
        startActivity(intent);
    }
    private void getRemoteUserId() {
        getConversationById(conversationId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Conversation conversation = task.getResult();
                if (conversation != null) {
                    // lấy id ko phải local id
                    conversation.getParticipantIds().forEach(s -> {
                        if(!s.equals(FirebaseAuth.getInstance().getUid())){
                            remoteUserToken = s;
                            ConnectRepo.getInstance().setUserRemoteById(remoteUserToken);
                            return;
                        }
                    });
                }
            }
        });
    }

    public Task<Conversation> getConversationById(String id){
        TaskCompletionSource<Conversation> taskCompletionSource = new TaskCompletionSource<>();
        FirebaseFirestore.getInstance().collection("conversations").document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    Conversation conversation = task.getResult().toObject(Conversation.class);
                    taskCompletionSource.setResult(conversation);
                } else {
                    taskCompletionSource.setException(Objects.requireNonNull(task.getException()));
                }
            }
        });
        return taskCompletionSource.getTask();
    }
}
