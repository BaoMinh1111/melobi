package vn.edu.ueh.melobi_nhom5.service;

import android.text.TextUtils;

import java.util.List;

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

    public void createAnswer(String questionId, String userId, String userName, String userAvatarUrl, String content, Callback<Answer> callback) {
        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setUserName(TextUtils.isEmpty(userName) ? "Thành viên" : userName);
        answer.setUserAvatarUrl(userAvatarUrl != null ? userAvatarUrl : "");
        answer.setContent(content != null ? content.trim() : "");
        createAnswer(answer, callback);
    }

    public void createAnswer(Answer answer, Callback<Answer> callback) {
        if (answer == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Câu trả lời không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(answer.getQuestionId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu hỏi không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(answer.getUserId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID người dùng không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(answer.getContent()) || answer.getContent().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Nội dung câu trả lời không được để trống"));
            return;
        }

        answer.setContent(answer.getContent().trim());
        if (TextUtils.isEmpty(answer.getUserName())) {
            answer.setUserName("Thành viên");
        }
        if (answer.getUserAvatarUrl() == null) {
            answer.setUserAvatarUrl("");
        }

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

    public void getAnswersByQuestion(String questionId, Callback<List<Answer>> callback) {
        if (TextUtils.isEmpty(questionId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu hỏi không hợp lệ"));
            return;
        }

        answerRepository.getAnswersByQuestionId(questionId, new AnswerRepository.Callback<List<Answer>>() {
            @Override
            public void onSuccess(List<Answer> result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }
}
