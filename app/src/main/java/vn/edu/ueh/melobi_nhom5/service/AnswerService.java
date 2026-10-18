package vn.edu.ueh.melobi_nhom5.service;

import android.text.TextUtils;

import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.repository.AnswerRepository;
import vn.edu.ueh.melobi_nhom5.entity.Like;
import vn.edu.ueh.melobi_nhom5.entity.Notification;

public class AnswerService {
    private final AnswerRepository answerRepository;
    private final NotificationService notificationService = new NotificationService();

    public AnswerService() {
        this.answerRepository = new AnswerRepository();
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
    public void toggleLike(String answerId, String answerOwnerId, String questionId,
                           String userId, Callback<Answer> callback) {
        answerRepository.toggleLike(answerId, userId, new AnswerRepository.Callback<Answer>() {
            @Override public void onSuccess(Answer result) {
                if (result.isLikedBy(userId)) {
                    notificationService.createNotification(answerOwnerId, userId,
                            Notification.TYPE_LIKE, questionId, Like.TYPE_QUESTION);
                }
                if (callback != null) callback.onSuccess(result);
            }
            @Override public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }
    /**
     * Tạo và lưu câu trả lời mới vào Firestore
     */
    public void createAnswer(String questionId, String userId, String userName, String userAvatarUrl, String content, Callback<Answer> callback) {
        if (TextUtils.isEmpty(questionId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu hỏi không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(userId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID người dùng không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(content) || content.trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Nội dung câu trả lời không được để trống"));
            return;
        }

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setUserName(TextUtils.isEmpty(userName) ? "Thành viên" : userName);
        answer.setUserAvatarUrl(userAvatarUrl != null ? userAvatarUrl : "");
        answer.setContent(content.trim());

        answerRepository.addAnswer(answer, new AnswerRepository.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }
}
/*1 số vấn đề:
* Không nhất quán với thiết kế chung
* Callback bị khai báo trùng: AnswerService.Callback và AnswerRepository.Callback giống nhau, nên đoạn bọc callback ở cuối 
(new AnswerRepository.Callback<Answer>() { ... }) chỉ chuyển tiếp y nguyên và không làm gì thêm
-> cách sửa: dùng một interface chung (đặt trong package common hoặc util), rồi truyền callback thẳng vào repository.
* Dùng TextUtils (Android): lớp này sẽ không chạy trong unit test thuần JVM (trả mặc định hoặc lỗi “not mocked”)
* Luồng callback: các lỗi validate được trả về đồng bộ (ngay trong lời gọi), còn lỗi từ Firestore trả về bất đồng bộ
* Khó test và khó thay thế: new AnswerRepository() trong constructor làm service gắn chặt với Firestore
-> Cách sửa: truyền repository qua constructor (AnswerService(AnswerRepository repo, NotificationService ns, QuestionService qs)) để dễ dùng mock.*/
