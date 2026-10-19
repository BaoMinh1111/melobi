
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.LinkedHashMap;
import java.util.Map;

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

    // Lay cac cuoc tro chuyen cua mot nguoi dung
    public Task<List<Conversation>> getConversations(String userId) {
        validateId(userId);

        Task<QuerySnapshot> first = db
                .collection("conversations")
                .whereEqualTo("userId1", userId)
                .get();

        Task<QuerySnapshot> second = db
                .collection("conversations")
                .whereEqualTo("userId2", userId)
                .get();

        Task<List<QuerySnapshot>> combined =
                Tasks.whenAllSuccess(first, second);

        return combined.continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            Map<String, Conversation> unique =
                    new LinkedHashMap<>();

            for (QuerySnapshot snapshot : task.getResult()) {
                for (DocumentSnapshot doc :
                        snapshot.getDocuments()) {

                    Conversation conversation =
                            doc.toObject(Conversation.class);

                    if (conversation != null) {
                        conversation.setConversationId(doc.getId());
                        unique.put(doc.getId(), conversation);
                    }
                }
            }

            List<Conversation> result =
                    new ArrayList<>(unique.values());

            // Cuoc tro chuyen moi nhat o tren
            result.sort((a, b) -> {
                long timeA = a.getUpdatedAt() == null
                        ? 0L : a.getUpdatedAt().getTime();

                long timeB = b.getUpdatedAt() == null
                        ? 0L : b.getUpdatedAt().getTime();

                return Long.compare(timeB, timeA);
            });

            return result;
        });
    }

    // Danh dau tin nhan da doc cho nguoi nhan
    public Task<Void> markAsRead(
            String conversationId,
            String readerId) {

        validateId(conversationId);
        validateId(readerId);

        DocumentReference conversationRef = db
                .collection("conversations")
                .document(conversationId);

        // Kiem tra nguoi doc co thuoc conversation
        return conversationRef.get().continueWithTask(convTask -> {
            if (!convTask.isSuccessful()) {
                throw convTask.getException();
            }

            Conversation conversation =
                    convTask.getResult().toObject(Conversation.class);

            if (conversation == null) {
                throw new IllegalArgumentException(
                        "Conversation khong ton tai"
                );
            }

            boolean isMember =
                    readerId.equals(conversation.getUserId1())
                            || readerId.equals(conversation.getUserId2());

            if (!isMember) {
                throw new IllegalArgumentException(
                        "Nguoi doc khong thuoc conversation"
                );
            }

            return conversationRef.collection("messages")
                    .whereEqualTo("isRead", false)
                    .get()
                    .continueWithTask(messageTask -> {

                        if (!messageTask.isSuccessful()) {
                            throw messageTask.getException();
                        }

                        List<Task<Void>> commits = new ArrayList<>();
                        WriteBatch batch = db.batch();
                        int pending = 0;

                        for (DocumentSnapshot doc :
                                messageTask.getResult().getDocuments()) {

                            String senderId =
                                    doc.getString("senderId");

                            // Khong danh dau tin cua chinh minh
                            if (senderId == null
                                    || readerId.equals(senderId)) {
                                continue;
                            }

                            batch.update(
                                    doc.getReference(),
                                    "isRead",
                                    true
                            );

                            pending++;

                            // Chia batch de tranh vuot gioi han
                            if (pending == 400) {
                                commits.add(batch.commit());
                                batch = db.batch();
                                pending = 0;
                            }
                        }

                        if (pending > 0) {
                            commits.add(batch.commit());
                        }

                        if (commits.isEmpty()) {
                            return Tasks.forResult(null);
                        }

                        return Tasks.whenAll(commits);
                    });
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
/*1 số lỗi sau:
* Thiếu: getConversations(userId)
* Thiếu: markAsRead(conversationId)
* Thiếu Nhận tin nhắn theo thời gian thực: getMessages dùng get() một lần, nên người kia gửi tin thì màn hình không tự cập nhật. Cần addSnapshotListener trả về 
ListenerRegistration, và Activity phải gỡ listener ở onStop/onDestroy
* Bổ sung Phân trang tin nhắn: getMessages tải toàn bộ lịch sử. Cuộc trò chuyện dài sẽ chậm và tốn lượt đọc. Lấy 30-50 tin mới nhất (orderBy sentAt DESC + limit), 
cuộn lên thì tải thêm
* Thời gian lấy từ đồng hồ máy người dùng. new Date() dùng cho sentAt và updatedAt. Hai điện thoại lệch giờ nhau vài phút thì tin nhắn hiện sai thứ tự trong khung chat, 
và danh sách cuộc trò chuyện sắp xếp sai. Với chat đây là lỗi dễ thấy. Dùng giờ của server (@ServerTimestamp cho Message.sentAt, FieldValue.serverTimestamp() cho 
updatedAt)
* Ném lỗi đồng bộ thay vì trả Task lỗi. validateId và kiểm tra “chat với chính mình”, “tin nhắn rỗng” đều throw ngay trong hàm, nên Activity phải bọc try/catch quanh 
từng lần gọi. Các service của Nghi trả Tasks.forException(...), nên làm giống để mọi nơi xử lý lỗi bằng addOnFailureListener.
* String.isBlank() ở validateId. Project đặt minSdk = 29, mà isBlank() chỉ có sẵn từ Android API 33, nên máy cũ có thể crash. Thay bằng id == null || id.trim().isEmpty()
* throw task.getException() có thể ném null. Nếu task bị hủy thì getException() là null, dòng throw sẽ gây NullPointerException. Gọi task.getResult() (tự ném đúng lỗi) 
như QuestionService đang làm.
* lastMessage lưu nguyên văn tin nhắn. Tin dài sẽ làm document cuộc trò chuyện phình ra, mà danh sách chat chỉ cần vài chục ký tự. Cắt còn khoảng 100 ký tự khi 
cập nhật. Đồng thời nên có giới hạn độ dài content (ví dụ 2000 ký tự).
* Gửi tin nhắn thất bại khi mất mạng. Transaction của Firestore yêu cầu có mạng, nên sendMessage lỗi ngay khi offline. Nếu muốn tin nhắn tự gửi khi có mạng lại, thay 
transaction bằng WriteBatch (ghi tin + cập nhật cuộc trò chuyện), vì ghi thường được Firestore xếp hàng chờ. Đổi lại sẽ mất bước kiểm tra “cuộc trò chuyện tồn tại” 
trong transaction*/
