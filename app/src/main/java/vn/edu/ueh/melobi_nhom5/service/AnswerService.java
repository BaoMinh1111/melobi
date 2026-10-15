package vn.edu.ueh.melobi_nhom5.service;

import android.text.TextUtils;

import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.repository.AnswerRepository;

public class AnswerService {
    private final AnswerRepository answerRepository;

    public AnswerService() {
        this.answerRepository = new AnswerRepository();
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
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
