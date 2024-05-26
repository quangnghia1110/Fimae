package com.example.fimae.repository;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.example.fimae.R;
import com.example.fimae.models.Conversation;
import com.example.fimae.models.Message;
import com.example.fimae.service.FirebaseService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class ChatRepository {
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private static ChatRepository defaultChatInstance;
    private static ChatRepository randomChatInstance;
    CollectionReference conversationsRef;

    private ChatRepository(CollectionReference reference) {
        this.conversationsRef = reference;
    }

    public static synchronized ChatRepository getDefaultChatInstance() {
        if (defaultChatInstance == null) {
            defaultChatInstance = new ChatRepository(FirebaseFirestore.getInstance().collection("conversations"));
        }
        return defaultChatInstance;
    }
    public static synchronized ChatRepository getRandomChatInstance() {
        if (defaultChatInstance == null) {
            defaultChatInstance = new ChatRepository(FirebaseFirestore.getInstance().collection("chats"));
        }
        return defaultChatInstance;
    }
    public ListenerRegistration getConversationsRef(@NotNull EventListener<QuerySnapshot> listener) {

        return conversationsRef.addSnapshotListener(listener);
    }

    public Query getRandomMessageQuery(String id) {
        return conversationsRef.document(id).collection("messages").orderBy("sentAt", Query.Direction.ASCENDING);
    }
    public Task<Void> likeFimaerInRandomChat(String id) {
        TaskCompletionSource<Void> taskCompletionSource = new TaskCompletionSource<>();
         conversationsRef.document(id).update("like." + FirebaseAuth.getInstance().getUid(), true).addOnCompleteListener(
                 task -> {
                        if (task.isSuccessful()) {
                            taskCompletionSource.setResult(null);
                        } else {
                            taskCompletionSource.setException(Objects.requireNonNull(task.getException()));
                        }
                 }
         );
         return taskCompletionSource.getTask();
    }

    public Query getConversationQuery() {
        return conversationsRef.whereArrayContains("participantIds", Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));
    }

    public Task<Conversation> getOrCreateFriendConversation(String id) {
        ArrayList<String> arrayList = new ArrayList() {{
            add(id);
            add(FirebaseAuth.getInstance().getUid());
        }};
        return getOrCreateConversation(arrayList, Conversation.FRIEND_CHAT);
    }

    public Task<Conversation> getOrCreateConversation(ArrayList<String> participantIds, String type) {

        TaskCompletionSource<Conversation> taskCompletionSource = new TaskCompletionSource<>();
        Collections.sort(participantIds);
        Task<QuerySnapshot> queryTask = conversationsRef
                .whereEqualTo("participantIds", participantIds)
                .limit(1)
                .get();
        queryTask.addOnCompleteListener(querySnapshotTask -> {
            if (querySnapshotTask.isSuccessful()) {
                QuerySnapshot querySnapshot = querySnapshotTask.getResult();
                System.out.println("Res: " + querySnapshot.getDocuments());
                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                    Conversation conversation = documentSnapshot.toObject(Conversation.class);
                    assert conversation != null;
                    taskCompletionSource.setResult(conversation);
                } else {
                    DocumentReference newConDoc = conversationsRef.document();
                    WriteBatch batch = FirebaseFirestore.getInstance().batch();
                    Conversation conversation = Conversation.create(newConDoc.getId(), Conversation.FRIEND_CHAT, participantIds);
                    batch.set(newConDoc, conversation);
                    for (String id : participantIds) {
                        batch.update(newConDoc, "joinedAt." + id, FieldValue.serverTimestamp());
                    }
                    batch.commit().addOnCompleteListener(batchTask -> {
                        if (batchTask.isSuccessful()) {
                            conversation.setId(newConDoc.getId());
                            taskCompletionSource.setResult(conversation);
                        } else {
                            taskCompletionSource.setException(Objects.requireNonNull(batchTask.getException()));
                        }
                    });
                }
            } else {
                taskCompletionSource.setException(Objects.requireNonNull(querySnapshotTask.getException()));
            }
        });
        return taskCompletionSource.getTask();
    }



    public Task<Void> updateReadLastMessageAt(String conversationId, Date timeStamp) {

        return conversationsRef.document(conversationId).update("readLastMessageAt." + FirebaseAuth.getInstance().getUid(), timeStamp);
    }


    public Task<Message> sendMessage(String conversationId, Object content, String type){
        TaskCompletionSource<Message> taskCompletionSource = new TaskCompletionSource<Message>();
        if (content instanceof String && ((String) content).isEmpty()) {
            taskCompletionSource.setException(new Exception("Message has null content"));
            return taskCompletionSource.getTask();
        }
        WriteBatch batch = firestore.batch();
        DocumentReference currentConversationRef = conversationsRef.document(conversationId);
        CollectionReference reference = currentConversationRef.collection("messages");
        DocumentReference messDoc = reference.document();
        Message message = new Message();
        message.setId(messDoc.getId());
        message.setType(type);
        message.setContent(content);
        message.setIdSender(FirebaseAuth.getInstance().getUid());
        message.setConversationID(conversationId);
        batch.set(messDoc, message);
        batch.update(currentConversationRef, "lastMessage", messDoc);
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                taskCompletionSource.setResult(message);
            } else {
                taskCompletionSource.setException(Objects.requireNonNull(task.getException()));
            }
        });
        return taskCompletionSource.getTask();
    }

    public Task<Message> sendPostLink(String conversationId, String content) {
        return sendMessage(conversationId, content, Message.POST_LINK);
    }
    public Task<Message> sendTextMessage(String conversationId, String content, Context context) {
        TaskCompletionSource<Message> taskCompletionSource = new TaskCompletionSource<>();
        if (content.isEmpty()) {
            taskCompletionSource.setException(new Exception("Message has null content"));
            return taskCompletionSource.getTask();
        }
        WriteBatch batch = firestore.batch();
        DocumentReference currentConversationRef = conversationsRef.document(conversationId);
        CollectionReference reference = currentConversationRef.collection("messages");
        DocumentReference messDoc = reference.document();
        Message message = new Message();
        message.setId(messDoc.getId());
        message.setType(Message.TEXT);
        message.setContent(content);
        message.setIdSender(FirebaseAuth.getInstance().getUid());
        message.setConversationID(conversationId);
        batch.set(messDoc, message);
        batch.update(currentConversationRef, "lastMessage", messDoc);
        batch.commit().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                taskCompletionSource.setResult(message);
                // Phát âm thanh chuông chỉ trên thiết bị của người nhận ở đây
                //playNotificationSound(context);
            } else {
                taskCompletionSource.setException(Objects.requireNonNull(task.getException()));
            }
        });
        return taskCompletionSource.getTask();
    }

    private void playNotificationSound(Context context) {
        // Phát âm thanh chuông chỉ trên thiết bị của người nhận ở đây
        MediaPlayer notificationPlayer = MediaPlayer.create(context, R.raw.ring); // Thay R.raw.notification_sound bằng tệp âm thanh chuông của bạn
        notificationPlayer.start();
    }
    public Task<Message> sendShortMessage(String conversationId, String content) {
        return sendMessage(conversationId, content, Message.SHORT_VIDEO);
    }
    public Task<Message> sendMediaMessage(String conversationId, ArrayList<Uri> uris) {
        TaskCompletionSource<Message> taskCompletionSource = new TaskCompletionSource<Message>();
        if (uris.isEmpty()) {
            taskCompletionSource.setException(new Exception("Media message has null content"));
            return taskCompletionSource.getTask();
        }
        FirebaseService.getInstance().uploadTaskFiles("conversation-medias/" + conversationId , uris).whenComplete(new BiConsumer<List<String>, Throwable>() {
            @Override
            public void accept(List<String> strings, Throwable throwable) {
                if (throwable != null) {
                    taskCompletionSource.setException((Exception) throwable);
                } else {
                    sendMessage(conversationId, strings, Message.MEDIA).addOnCompleteListener(new OnCompleteListener<Message>() {
                        @Override
                        public void onComplete(@NonNull Task<Message> task) {
                            if (task.isSuccessful()) {
                                taskCompletionSource.setResult(task.getResult());
                            } else {
                                taskCompletionSource.setException(Objects.requireNonNull(task.getException()));
                            }
                        }
                    });
                }
            }
        });
        return taskCompletionSource.getTask();
    }
    public Task<Void> deleteConversation(String conversationId) {
        return conversationsRef.document(conversationId).delete();
    }

    public Task<Conversation> getConversationById(String id) {
        TaskCompletionSource<Conversation> taskCompletionSource = new TaskCompletionSource<>();
        conversationsRef.document(id).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                taskCompletionSource.setResult(task.getResult().toObject(Conversation.class));
            } else {
                taskCompletionSource.setException(Objects.requireNonNull(task.getException()));
            }
        });
        return taskCompletionSource.getTask();
    }
    public Task<Void> deleteMessage(String conversationId, String messageId) {
        DocumentReference messageRef = firestore.collection("conversations").document(conversationId).collection("messages").document(messageId);
        return messageRef.delete();
    }
}
