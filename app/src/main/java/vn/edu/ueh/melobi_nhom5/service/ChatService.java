package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.entity.Message;
import vn.edu.ueh.melobi_nhom5.entity.Notification;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;

/**
 * ChatService theo tài liệu: createConversation, sendMessage, getMessages,
 * getConversations, markAsRead. Ghi Firestore để tin nhắn vẫn tới khi đối phương offline.
 */
public class ChatService {

    private static final String COL_CONVERSATIONS = "conversations";
    private static final String COL_MESSAGES = "messages";
    private static final int LAST_MESSAGE_PREVIEW_LIMIT = 100;
    private static final int CONTENT_LIMIT = 2000;
    private static final int BATCH_LIMIT = 500;

    private final FirebaseFirestore db;
    private final NotificationService notificationService;

    public ChatService() {
        db = FirebaseFirestore.getInstance();
        notificationService = new NotificationService();
    }

    public static String conversationIdFor(String user1Id, String user2Id) {
        if (user1Id == null || user2Id == null) {
            return null;
        }
        String first = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        String second = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;
        String key = first.length() + ":" + first + second;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    public Task<Message> ensureConversationThenSend(String senderId,
                                                    String otherUserId,
                                                    String conversationId,
                                                    String content) {
        String resolvedConversationId = conversationId;
        if (isInvalidId(resolvedConversationId) && !isInvalidId(senderId) && !isInvalidId(otherUserId)) {
            resolvedConversationId = conversationIdFor(senderId, otherUserId);
        }
        if (isInvalidId(otherUserId)) {
            return sendQueuedMessage(resolvedConversationId, senderId, content);
        }
        String finalConversationId = resolvedConversationId;
        return createConversation(senderId, otherUserId).continueWithTask(task -> {
            task.getResult();
            return sendQueuedMessage(finalConversationId, senderId, content);
        });
    }

    public Task<Conversation> createConversation(String user1Id, String user2Id) {
        if (!NetworkUtils.isOnline()) {
            return Tasks.forException(NetworkUtils.offlineException());
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return Tasks.forException(new IllegalStateException("Chua dang nhap"));
        }
        if (isInvalidId(user1Id) || isInvalidId(user2Id)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }
        if (user1Id.equals(user2Id)) {
            return Tasks.forException(new IllegalArgumentException("Khong the chat voi chinh minh"));
        }

        String first = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        String second = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;
        String conversationId = conversationIdFor(user1Id, user2Id);
        DocumentReference ref = db.collection(COL_CONVERSATIONS).document(conversationId);

        Conversation conversation = new Conversation(conversationId, first, second, "", new Date());
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("user1Id", first);
        data.put("user2Id", second);
        data.put("participants", Arrays.asList(first, second));
        data.put("updatedAt", new Date());

        return ref.set(data, SetOptions.merge()).continueWith(task -> {
            task.getResult();
            return conversation;
        });
    }

    public Task<Message> sendMessage(String conversationId, String senderId, String content) {
        return sendMessage(conversationId, senderId, content, null);
    }

    public Task<Message> sendMessage(String conversationId, String senderId, String content, String imageUrl) {
        if (!NetworkUtils.isOnline()) {
            return Tasks.forException(NetworkUtils.offlineException());
        }
        return sendQueuedMessage(conversationId, senderId, content, imageUrl);
    }

    public Task<Message> sendQueuedMessage(String conversationId, String senderId, String content) {
        return sendQueuedMessage(conversationId, senderId, content, null);
    }

    public Task<Message> sendQueuedMessage(String conversationId, String senderId, String content, String imageUrl) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return Tasks.forException(new IllegalStateException("Chua dang nhap"));
        }
        if (isInvalidId(conversationId) || isInvalidId(senderId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        String text = content != null ? content.trim() : "";
        if (text.isEmpty() && !hasImage) {
            return Tasks.forException(new IllegalArgumentException("Tin nhan khong duoc de trong"));
        }
        if (text.length() > CONTENT_LIMIT) {
            return Tasks.forException(new IllegalArgumentException("Tin nhan vuot qua 2000 ky tu"));
        }

        DocumentReference conversationRef = db.collection(COL_CONVERSATIONS).document(conversationId);
        DocumentReference messageRef = conversationRef.collection(COL_MESSAGES).document();
        String previewSource = !text.isEmpty() ? text : "[Ảnh]";
        String preview = previewSource.length() > LAST_MESSAGE_PREVIEW_LIMIT
                ? previewSource.substring(0, LAST_MESSAGE_PREVIEW_LIMIT)
                : previewSource;
        Date now = new Date();

        Message message = new Message(
                messageRef.getId(),
                conversationId,
                senderId,
                text,
                now,
                false
        );

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", message.getMessageId());
        messageData.put("conversationId", conversationId);
        messageData.put("senderId", senderId);
        messageData.put("content", text);
        if (hasImage) {
            messageData.put("imageUrl", imageUrl);
            message.setImageUrl(imageUrl);
        }
        messageData.put("sentAt", now);
        messageData.put("isRead", false);

        Map<String, Object> conversationUpdate = new HashMap<>();
        conversationUpdate.put("lastMessage", preview);
        conversationUpdate.put("updatedAt", now);
        conversationUpdate.put("conversationId", conversationId);

        final String[] receiverId = new String[1];
        return conversationRef.get().continueWithTask(task -> {
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot != null && snapshot.exists()) {
                String user1Id = firstNonEmpty(snapshot.getString("user1Id"), snapshot.getString("userId1"));
                String user2Id = firstNonEmpty(snapshot.getString("user2Id"), snapshot.getString("userId2"));
                if (!isBlank(user1Id)) {
                    conversationUpdate.put("user1Id", user1Id);
                }
                if (!isBlank(user2Id)) {
                    conversationUpdate.put("user2Id", user2Id);
                }
                if (!isBlank(user1Id) && !isBlank(user2Id)) {
                    conversationUpdate.put("participants", Arrays.asList(user1Id, user2Id));
                }
                if (senderId.equals(user1Id)) {
                    receiverId[0] = user2Id;
                } else if (senderId.equals(user2Id)) {
                    receiverId[0] = user1Id;
                }
            }
            WriteBatch batch = db.batch();
            batch.set(messageRef, messageData);
            batch.set(conversationRef, conversationUpdate, SetOptions.merge());
            return batch.commit();
        }).continueWith(task -> {
            task.getResult();
            if (!isBlank(receiverId[0])) {
                notificationService.createNotification(
                        receiverId[0],
                        senderId,
                        Notification.TYPE_MESSAGE,
                        conversationId,
                        "CONVERSATION"
                );
            }
            return message;
        });
    }

    public Task<List<Message>> getMessages(String conversationId) {
        if (isInvalidId(conversationId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }

        return db.collection(COL_CONVERSATIONS)
                .document(conversationId)
                .collection(COL_MESSAGES)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> mapMessages(task.getResult()));
    }

    public Task<List<Conversation>> getConversations(String userId) {
        if (isInvalidId(userId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }

        Task<QuerySnapshot> asUser1 = queryConversations("user1Id", userId);
        Task<QuerySnapshot> asUser2 = queryConversations("user2Id", userId);
        Task<QuerySnapshot> asParticipant = db.collection(COL_CONVERSATIONS)
                .whereArrayContains("participants", userId)
                .get()
                .continueWith(task -> task.isSuccessful() ? task.getResult() : null);

        return Tasks.whenAll(asUser1, asUser2, asParticipant)
                .continueWith(task -> {
                    List<Object> snapshots = new ArrayList<>();
                    snapshots.add(asUser1.getResult());
                    snapshots.add(asUser2.getResult());
                    snapshots.add(asParticipant.getResult());
                    return mergeConversations(snapshots);
                });
    }

    public Task<Void> markAsRead(String conversationId) {
        if (isInvalidId(conversationId)) {
            return Tasks.forException(new IllegalArgumentException("ID khong duoc de trong"));
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || isInvalidId(firebaseUser.getUid())) {
            return Tasks.forException(new IllegalStateException("Chua dang nhap"));
        }
        String currentUserId = firebaseUser.getUid();
        DocumentReference conversationRef = db.collection(COL_CONVERSATIONS).document(conversationId);

        return conversationRef.collection(COL_MESSAGES)
                .whereEqualTo("isRead", false)
                .get()
                .continueWithTask(task -> {
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot == null || snapshot.isEmpty()) {
                        return Tasks.forResult(null);
                    }

                    List<DocumentReference> unreadFromOthers = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String senderId = doc.getString("senderId");
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

    public ListenerRegistration listenMessages(String conversationId,
                                               EventListener<QuerySnapshot> listener) {
        return db.collection(COL_CONVERSATIONS)
                .document(conversationId)
                .collection(COL_MESSAGES)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .addSnapshotListener(MetadataChanges.INCLUDE, listener);
    }

    public List<ListenerRegistration> listenConversations(String userId,
                                                          OnSuccessListener<List<Conversation>> onSuccess,
                                                          OnFailureListener onFailure) {
        QuerySnapshot[] snapshots = new QuerySnapshot[3];
        List<ListenerRegistration> registrations = new ArrayList<>();
        registrations.add(listenConversationQuery(
                db.collection(COL_CONVERSATIONS).whereEqualTo("user1Id", userId),
                snapshots, 0, true, onSuccess, onFailure));
        registrations.add(listenConversationQuery(
                db.collection(COL_CONVERSATIONS).whereEqualTo("user2Id", userId),
                snapshots, 1, true, onSuccess, onFailure));
        registrations.add(listenConversationQuery(
                db.collection(COL_CONVERSATIONS).whereArrayContains("participants", userId),
                snapshots, 2, false, onSuccess, onFailure));
        return registrations;
    }

    private ListenerRegistration listenConversationQuery(Query query,
                                                         QuerySnapshot[] snapshots,
                                                         int index,
                                                         boolean reportError,
                                                         OnSuccessListener<List<Conversation>> onSuccess,
                                                         OnFailureListener onFailure) {
        return query.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
            if (error != null) {
                if (reportError && onFailure != null) {
                    onFailure.onFailure(error);
                }
                return;
            }
            snapshots[index] = snapshot;
            List<Object> items = new ArrayList<>();
            for (QuerySnapshot item : snapshots) {
                if (item != null) {
                    items.add(item);
                }
            }
            if (onSuccess != null) {
                onSuccess.onSuccess(mergeConversations(items));
            }
        });
    }

    private Task<QuerySnapshot> queryConversations(String field, String userId) {
        return db.collection(COL_CONVERSATIONS)
                .whereEqualTo(field, userId)
                .get()
                .continueWith(task -> task.isSuccessful() ? task.getResult() : null);
    }

    public static List<Message> mapMessages(QuerySnapshot snapshot) {
        List<Message> messages = new ArrayList<>();
        if (snapshot == null) {
            return messages;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Message message = doc.toObject(Message.class);
            if (message == null) {
                message = new Message();
                message.setContent(doc.getString("content"));
                message.setSenderId(doc.getString("senderId"));
                message.setConversationId(doc.getString("conversationId"));
                Boolean read = doc.getBoolean("isRead");
                message.setIsRead(Boolean.TRUE.equals(read));
                Date sentAt = doc.getDate("sentAt");
                if (sentAt != null) {
                    message.setSentAt(sentAt);
                }
            }
            message.setMessageId(doc.getId());
            if (message.getImageUrl() == null || message.getImageUrl().trim().isEmpty()) {
                message.setImageUrl(doc.getString("imageUrl"));
            }
            messages.add(message);
        }
        return messages;
    }

    public static List<Conversation> mergeConversations(List<Object> snapshots) {
        Map<String, Conversation> unique = new LinkedHashMap<>();
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
    }

    public static Conversation mapConversation(DocumentSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }

        Conversation conversation = snapshot.toObject(Conversation.class);
        if (conversation == null) {
            conversation = new Conversation();
        }
        conversation.setConversationId(snapshot.getId());

        if (isBlank(conversation.getUser1Id())) {
            String value = firstNonEmpty(snapshot.getString("user1Id"), snapshot.getString("userId1"));
            if (value != null) {
                conversation.setUser1Id(value);
            }
        }
        if (isBlank(conversation.getUser2Id())) {
            String value = firstNonEmpty(snapshot.getString("user2Id"), snapshot.getString("userId2"));
            if (value != null) {
                conversation.setUser2Id(value);
            }
        }
        return conversation;
    }

    private String buildConversationId(String user1Id, String user2Id) {
        return conversationIdFor(user1Id, user2Id);
    }

    private static String firstNonEmpty(String first, String second) {
        if (!isBlank(first)) {
            return first;
        }
        if (!isBlank(second)) {
            return second;
        }
        return null;
    }

    private boolean isInvalidId(String id) {
        return isBlank(id);
    }

    private static boolean isBlank(String id) {
        return id == null || id.trim().isEmpty();
    }
}
