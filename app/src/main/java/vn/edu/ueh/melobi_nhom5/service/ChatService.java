
package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.entity.Message;

public class ChatService {

    private final FirebaseFirestore db;

    public ChatService() {
        db = FirebaseFirestore.getInstance();
    }

    // Tao hoac lay lai cuoc tro chuyen cua hai user
    public Task<Conversation> createConversation(
            String userId1, String userId2) {

        validateId(userId1);
        validateId(userId2);

        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException(
                    "Khong the chat voi chinh minh"
            );
        }

        // ID co dinh, khong tao trung conversation
        String first = userId1.compareTo(userId2) < 0
                ? userId1 : userId2;

        String second = userId1.compareTo(userId2) < 0
                ? userId2 : userId1;

        String key = first.length() + ":" + first + second;

        String conversationId = UUID.nameUUIDFromBytes(
                key.getBytes(StandardCharsets.UTF_8)
        ).toString();

        DocumentReference ref = db
                .collection("conversations")
                .document(conversationId);

        return db.runTransaction(transaction -> {

            DocumentSnapshot snapshot = transaction.get(ref);

            if (snapshot.exists()) {
                Conversation existing =
                        snapshot.toObject(Conversation.class);

                if (existing == null) {
                    throw new IllegalStateException(
                            "Du lieu conversation khong hop le"
                    );
                }

                existing.setConversationId(conversationId);
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

    // Gui tin nhan
    public Task<Message> sendMessage(
            String conversationId,
            String senderId,
            String content) {

        validateId(conversationId);
        validateId(senderId);

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Tin nhan khong duoc de trong"
            );
        }

        String text = content.trim();

        DocumentReference conversationRef = db
                .collection("conversations")
                .document(conversationId);

        DocumentReference messageRef = conversationRef
                .collection("messages")
                .document();

        return db.runTransaction(transaction -> {

            DocumentSnapshot snapshot =
                    transaction.get(conversationRef);

            if (!snapshot.exists()) {
                throw new IllegalArgumentException(
                        "Conversation khong ton tai"
                );
            }

            Conversation conversation =
                    snapshot.toObject(Conversation.class);

            if (conversation == null
                    || (!senderId.equals(conversation.getUserId1())
                    && !senderId.equals(conversation.getUserId2()))) {
                throw new IllegalArgumentException(
                        "Nguoi gui khong thuoc conversation"
                );
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

            // Luu tin nhan va cap nhat conversation
            transaction.set(messageRef, message);

            transaction.update(
                    conversationRef,
                    "lastMessage", text,
                    "updatedAt", now
            );

            return message;
        });
    }

    // Lay lich su tin nhan theo thoi gian tang dan
    public Task<List<Message>> getMessages(
            String conversationId) {

        validateId(conversationId);

        return db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> {

                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    List<Message> messages = new ArrayList<>();

                    for (DocumentSnapshot doc :
                            task.getResult().getDocuments()) {

                        Message message =
                                doc.toObject(Message.class);

                        if (message != null) {
                            message.setMessageId(doc.getId());
                            messages.add(message);
                        }
                    }

                    return messages;
                });
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "ID khong duoc de trong"
            );
        }
    }
}
