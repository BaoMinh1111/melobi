package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.entity.Message;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;

/**
 * ChatService theo tài liệu: createConversation, sendMessage, getMessages,
 * getConversations, markAsRead. Logic gốc lấy từ nhánh feature/paht.
 */
public class ChatService {

    private static final String COL_CONVERSATIONS = "conversations";
    private static final String COL_MESSAGES = "messages";
    private static final int LAST_MESSAGE_PREVIEW_LIMIT = 100;
    private static final int CONTENT_LIMIT = 2000;
    private static final int BATCH_LIMIT = 500;

    private final FirebaseFirestore db;

    public ChatService() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<Conversation> createConversation(String user1Id, String user2Id) {
        if (isInvalidId(user1Id) || isInvalidId(user2Id)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }
        if (user1Id.equals(user2Id)) {
            return Tasks.forException(new IllegalArgumentException("Khong the chat voi chinh minh"));
        }
        if (!NetworkUtils.isOnline()) {
            return Tasks.forException(new IllegalStateException("Khong co ket noi Internet. Vui long thu lai."));
        }

        String first = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        String second = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;
        String key = first.length() + ":" + first + second;
        String conversationId = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
        DocumentReference ref = db.collection(COL_CONVERSATIONS).document(conversationId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(ref);
            if (snapshot.exists()) {
                Conversation existing = mapConversation(snapshot);
                if (existing == null) {
                    throw new IllegalStateException("Du lieu conversation khong hop le");
                }
                return existing;
            }

            Conversation conversation = new Conversation(
                    conversationId,
                    first,
                    second,
                    "",
                    new Date()
            );
            transaction.set(ref, conversation);
            return conversation;
        });
    }

    public Task<Message> sendMessage(String conversationId, String senderId, String content) {
        if (isInvalidId(conversationId) || isInvalidId(senderId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }
        if (content == null || content.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Tin nhan khong duoc de trong"));
        }
        if (!NetworkUtils.isOnline()) {
            return Tasks.forException(new IllegalStateException("Khong co ket noi Internet. Vui long thu lai."));
        }

        String text = content.trim();
        if (text.length() > CONTENT_LIMIT) {
            return Tasks.forException(new IllegalArgumentException("Tin nhan vuot qua 2000 ky tu"));
        }

        DocumentReference conversationRef = db.collection(COL_CONVERSATIONS).document(conversationId);
        DocumentReference messageRef = conversationRef.collection(COL_MESSAGES).document();
        String preview = text.length() > LAST_MESSAGE_PREVIEW_LIMIT
                ? text.substring(0, LAST_MESSAGE_PREVIEW_LIMIT)
                : text;

        return db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(conversationRef);
            if (!snapshot.exists()) {
                throw new IllegalArgumentException("Conversation khong ton tai");
            }

            Conversation conversation = mapConversation(snapshot);
            if (conversation == null
                    || (!senderId.equals(conversation.getUser1Id())
                    && !senderId.equals(conversation.getUser2Id()))) {
                throw new IllegalArgumentException("Nguoi gui khong thuoc conversation");
            }

            Date now = new Date();
            Message message = new Message(
                    messageRef.getId(),
                    conversationId,
                    senderId,
                    text,
                    now,
                    false
            );

            transaction.set(messageRef, message);
            transaction.update(
                    conversationRef,
                    "lastMessage", preview,
                    "updatedAt", now
            );
            return message;
        });
    }

    public Task<List<Message>> getMessages(String conversationId) {
        if (isInvalidId(conversationId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }
        if (!NetworkUtils.isOnline()) {
            return Tasks.forException(new IllegalStateException("Khong co ket noi Internet. Vui long thu lai."));
        }

        return db.collection(COL_CONVERSATIONS)
                .document(conversationId)
                .collection(COL_MESSAGES)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> {
                    QuerySnapshot snapshot = task.getResult();
                    List<Message> messages = new ArrayList<>();
                    if (snapshot == null) {
                        return messages;
                    }
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Message message = doc.toObject(Message.class);
                        if (message != null) {
                            message.setMessageId(doc.getId());
                            messages.add(message);
                        }
                    }
                    return messages;
                });
    }

    public Task<List<Conversation>> getConversations(String userId) {
        if (isInvalidId(userId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }
        if (!NetworkUtils.isOnline()) {
            return Tasks.forException(new IllegalStateException("Khong co ket noi Internet. Vui long thu lai."));
        }

        Task<QuerySnapshot> asUser1 = db.collection(COL_CONVERSATIONS)
                .whereEqualTo("user1Id", userId)
                .get();
        Task<QuerySnapshot> asUser2 = db.collection(COL_CONVERSATIONS)
                .whereEqualTo("user2Id", userId)
                .get();
        Task<QuerySnapshot> asLegacyUser1 = db.collection(COL_CONVERSATIONS)
                .whereEqualTo("userId1", userId)
                .get();
        Task<QuerySnapshot> asLegacyUser2 = db.collection(COL_CONVERSATIONS)
                .whereEqualTo("userId2", userId)
                .get();

        return Tasks.whenAllSuccess(asUser1, asUser2, asLegacyUser1, asLegacyUser2)
                .continueWith(task -> {
                    Map<String, Conversation> unique = new LinkedHashMap<>();
                    List<Object> snapshots = task.getResult();
                    if (snapshots != null) {
                        for (Object item : snapshots) {
                            if (!(item instanceof QuerySnapshot)) {
                                continue;
                            }
                            for (DocumentSnapshot doc : ((QuerySnapshot) item).getDocuments()) {
                                Conversation conversation = mapConversation(doc);
                                if (conversation != null && conversation.getConversationId() != null) {
                                    unique.put(conversation.getConversationId(), conversation);
                                }
                            }
                        }
                    }

                    List<Conversation> conversations = new ArrayList<>(unique.values());
                    Collections.sort(conversations, (left, right) -> {
                        Date leftDate = left.getUpdatedAt();
                        Date rightDate = right.getUpdatedAt();
                        if (leftDate == null && rightDate == null) {
                            return 0;
                        }
                        if (leftDate == null) {
                            return 1;
                        }
                        if (rightDate == null) {
                            return -1;
                        }
                        return rightDate.compareTo(leftDate);
                    });
                    return conversations;
                });
    }

    public Task<Void> markAsRead(String conversationId) {
        if (isInvalidId(conversationId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }
        if (!NetworkUtils.isOnline()) {
            return Tasks.forException(new IllegalStateException("Khong co ket noi Internet. Vui long thu lai."));
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || isInvalidId(firebaseUser.getUid())) {
            return Tasks.forException(new IllegalStateException("Chua dang nhap"));
        }
        String currentUserId = firebaseUser.getUid();
        DocumentReference conversationRef = db.collection(COL_CONVERSATIONS).document(conversationId);

        return conversationRef.get().continueWithTask(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot == null || !snapshot.exists()) {
                return Tasks.forException(new IllegalArgumentException("Conversation khong ton tai"));
            }

            Conversation conversation = mapConversation(snapshot);
            if (conversation == null
                    || (!currentUserId.equals(conversation.getUser1Id())
                    && !currentUserId.equals(conversation.getUser2Id()))) {
                return Tasks.forException(new IllegalArgumentException("Ban khong thuoc conversation"));
            }

            return conversationRef.collection(COL_MESSAGES)
                    .whereEqualTo("isRead", false)
                    .get();
        }).continueWithTask(task -> {
            QuerySnapshot snapshot = task.getResult();
            if (snapshot == null || snapshot.isEmpty()) {
                return Tasks.forResult(null);
            }

            List<DocumentReference> unreadFromOthers = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                Message message = doc.toObject(Message.class);
                String senderId = message != null ? message.getSenderId() : doc.getString("senderId");
                if (senderId == null || senderId.equals(currentUserId)) {
                    continue;
                }
                unreadFromOthers.add(doc.getReference());
            }
            if (unreadFromOthers.isEmpty()) {
                return Tasks.forResult(null);
            }

            List<Task<Void>> commits = new ArrayList<>();
            WriteBatch batch = db.batch();
            int count = 0;
            for (DocumentReference ref : unreadFromOthers) {
                batch.update(ref, "isRead", true);
                count++;
                if (count == BATCH_LIMIT) {
                    commits.add(batch.commit());
                    batch = db.batch();
                    count = 0;
                }
            }
            if (count > 0) {
                commits.add(batch.commit());
            }
            return Tasks.whenAll(commits);
        });
    }

    private Conversation mapConversation(DocumentSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        Conversation conversation = snapshot.toObject(Conversation.class);
        if (conversation == null) {
            conversation = new Conversation();
        }
        conversation.setConversationId(snapshot.getId());

        if (isInvalidId(conversation.getUser1Id())) {
            String value = firstNonEmpty(snapshot.getString("user1Id"), snapshot.getString("userId1"));
            if (value != null) {
                conversation.setUser1Id(value);
            }
        }
        if (isInvalidId(conversation.getUser2Id())) {
            String value = firstNonEmpty(snapshot.getString("user2Id"), snapshot.getString("userId2"));
            if (value != null) {
                conversation.setUser2Id(value);
            }
        }
        return conversation;
    }

    private String firstNonEmpty(String first, String second) {
        if (!isInvalidId(first)) {
            return first;
        }
        if (!isInvalidId(second)) {
            return second;
        }
        return null;
    }

    private boolean isInvalidId(String id) {
        return id == null || id.trim().isEmpty();
    }
}
