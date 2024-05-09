package com.example.fimae.repository;

import com.example.fimae.adapters.NewCommentAdapter;
import com.example.fimae.models.Comment;
import com.example.fimae.models.CommentItemAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommentRepository {
    public static String POST_COLLECTION = "posts";
    public static String SHORT_COLLECTION = "shorts";
    private static CommentRepository commentRepository;
    public static CommentRepository getInstance(){
        if(commentRepository == null) commentRepository = new CommentRepository();
        return commentRepository;
    }
    public CollectionReference getCommentRef(String postId, String collection){
        CollectionReference ref =  FirebaseFirestore.getInstance().collection(collection)
                .document(postId).collection("comments");
        return ref;
    }

    public Task<Boolean> postComment(String postId, String collection, Comment comment) {
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        DocumentReference commentId = getCommentRef(postId, collection).document();
        comment.setId(commentId.getId());
        comment.setLikes(new HashMap<>());
        comment.setChildren(new ArrayList<>());
        comment.setTimeCreated(new Date());
        comment.setPublisher(FirebaseAuth.getInstance().getUid());
        commentId.set(comment)
                .addOnSuccessListener(aVoid -> {
                    // Increment the numberOfComments field in the corresponding Post document
                    DocumentReference postRef = FirebaseFirestore.getInstance().collection("posts").document(postId);
                    postRef.update("numberOfComments", FieldValue.increment(0))
                            .addOnSuccessListener(aVoid1 -> {
                                taskCompletionSource.setResult(true);
                            })
                            .addOnFailureListener(e -> {
                                taskCompletionSource.setResult(false);
                            });
                })
                .addOnFailureListener(e -> {
                    taskCompletionSource.setResult(false);
                });

        return taskCompletionSource.getTask();
    }

    public Task<Boolean> editComment(String postId, Comment comment, String content, String collection) {
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        Map<String, Object> updates = new HashMap<>();
            updates.put("content", content);
            updates.put("timeEdited", FieldValue.serverTimestamp());
            getCommentRef(postId, collection).document(comment.getId()).update(updates).addOnCompleteListener(ss -> {
                taskCompletionSource.setResult(true);
            }).addOnFailureListener(e -> {
                taskCompletionSource.setResult(false);
            });
            return taskCompletionSource.getTask();
    }

    public Task<Boolean> deleteComment(String postId, Comment comment, String collection){
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();
        getCommentRef(postId, collection).document(comment.getId()).delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Xóa thành công
                // Giảm số lượng bình luận của bài đăng đi 1
                DocumentReference postRef = FirebaseFirestore.getInstance().collection("posts").document(postId);
                postRef.update("numberOfComments", FieldValue.increment(0))
                        .addOnSuccessListener(aVoid -> {
                            taskCompletionSource.setResult(true);
                        })
                        .addOnFailureListener(e -> {
                            taskCompletionSource.setResult(false);
                        });
            } else {
                // Xóa không thành công
                taskCompletionSource.setResult(false);
            }
        });
        return taskCompletionSource.getTask();
    }

    private int findCommentItemAdapterById(List<CommentItemAdapter> commentItemAdapters, String targetId) {
        for (CommentItemAdapter commentItem : commentItemAdapters) {
            if (commentItem.getComment().getId().equals(targetId)) {
                return commentItemAdapters.indexOf(commentItem);
            }
        }
        return -1; // Comment with the specified ID not found
    }
    public void getSubComment(CommentItemAdapter commentItem, String collection){
        Comment rootComment = commentItem.getComment();
        getCommentRef(rootComment.getPostId(), collection).orderBy("timeCreated").addSnapshotListener((value, error) -> {
            if (error != null || value == null) {
                return;
            }
            for(DocumentChange dc: value.getDocumentChanges()){
                Comment comment = dc.getDocument().toObject(Comment.class);
                if(comment.getParentId().equals("") || !comment.getParentId().equals(rootComment.getId())) break;
                switch (dc.getType()){
                    case ADDED:
                        commentItem.addNewSubComment(comment);
                        break;
                    case MODIFIED:
                        break;
                    case REMOVED:
                        commentItem.removeSubComment(comment);
                }
            }
        });
    }

    public void getComment(String postId, List<CommentItemAdapter> comments, NewCommentAdapter newCommentAdapter, String collection) {
        getCommentRef(postId, collection).orderBy("timeCreated").addSnapshotListener((value, error) -> {
            if (error != null) {
                return;
            }
            assert value != null;
            for (DocumentChange dc : value.getDocumentChanges()) {
                Comment comment = dc.getDocument().toObject(Comment.class);
                if(!comment.getParentId().equals("")) return;
                switch (dc.getType()) {
                    case ADDED:
                        CommentItemAdapter commentItemAdapter = new CommentItemAdapter(comment, collection);
                        comments.add(commentItemAdapter);
                        newCommentAdapter.addUpdate();
                        break;
                    case MODIFIED:
                        break;
                    case REMOVED:
                        int h = findCommentItemAdapterById(comments, comment.getId());
                        comments.remove(h);
                        newCommentAdapter.notifyItemRemoved(h);
                }
            }
        });
    }
}