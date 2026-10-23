package vn.edu.ueh.melobi_nhom5.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.model.Question;

/**
 * Đọc/ghi Question và Answer xuống SQLite để xem khi mất mạng.
 */
public class ContentLocalDao {
    private static final long ONE_DAY_MS = 24L * 60L * 60L * 1000L;

    private final DatabaseHelper helper;

    public ContentLocalDao(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    public void saveQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            for (Question question : questions) {
                upsertQuestion(db, question, now);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void saveQuestionDetail(Question question, List<Answer> answers) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (question != null) {
                upsertQuestion(db, question, System.currentTimeMillis());
            }
            if (answers != null) {
                for (Answer answer : answers) {
                    upsertAnswer(db, answer);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Question> getQuestionsViewedLastDay() {
        long since = System.currentTimeMillis() - ONE_DAY_MS;
        List<Question> questions = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_QUESTION,
                null,
                DatabaseHelper.COL_Q_LAST_VIEWED_AT + " >= ?",
                new String[]{String.valueOf(since)},
                null,
                null,
                DatabaseHelper.COL_Q_LAST_VIEWED_AT + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                questions.add(readQuestion(cursor));
            }
        } finally {
            cursor.close();
        }
        return questions;
    }

    public List<Answer> getAnswersByQuestionId(String questionId) {
        List<Answer> answers = new ArrayList<>();
        if (questionId == null || questionId.trim().isEmpty()) {
            return answers;
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_ANSWER,
                null,
                DatabaseHelper.COL_A_QUESTION_ID + " = ?",
                new String[]{questionId},
                null,
                null,
                DatabaseHelper.COL_A_CREATED_AT + " DESC"
        );
        try {
            while (cursor.moveToNext()) {
                answers.add(readAnswer(cursor));
            }
        } finally {
            cursor.close();
        }
        return answers;
    }

    private void upsertQuestion(SQLiteDatabase db, Question question, long viewedAt) {
        if (question == null || question.getQuestionId() == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_Q_ID, question.getQuestionId());
        values.put(DatabaseHelper.COL_Q_USER_ID, question.getUserId());
        values.put(DatabaseHelper.COL_Q_MAJOR_ID, question.getMajorId());
        values.put(DatabaseHelper.COL_Q_TITLE, question.getTitle());
        values.put(DatabaseHelper.COL_Q_CONTENT, question.getContent());
        values.put(DatabaseHelper.COL_Q_IMAGE_URL, question.getImageUrl());
        values.put(DatabaseHelper.COL_Q_STATUS, question.getStatus());
        values.put(DatabaseHelper.COL_Q_LIKE_COUNT, question.getLikeCount());
        values.put(DatabaseHelper.COL_Q_ANSWER_COUNT, question.getAnswerCount());
        values.put(DatabaseHelper.COL_Q_CREATED_AT, question.getCreatedAt());
        values.put(DatabaseHelper.COL_Q_UPDATED_AT, question.getUpdatedAt());
        values.put(DatabaseHelper.COL_Q_SYNC_STATUS, DatabaseHelper.SYNC_STATUS_SYNCED);
        values.put(DatabaseHelper.COL_Q_AUTHOR_NAME, question.getUserName());
        values.put(DatabaseHelper.COL_Q_AUTHOR_AVATAR, question.getUserAvatarUrl());
        values.put(DatabaseHelper.COL_Q_LAST_VIEWED_AT, viewedAt);
        db.insertWithOnConflict(DatabaseHelper.TABLE_QUESTION, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void upsertAnswer(SQLiteDatabase db, Answer answer) {
        if (answer == null || answer.getAnswerId() == null) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_A_ID, answer.getAnswerId());
        values.put(DatabaseHelper.COL_A_QUESTION_ID, answer.getQuestionId());
        values.put(DatabaseHelper.COL_A_USER_ID, answer.getUserId());
        values.put(DatabaseHelper.COL_A_CONTENT, answer.getContent());
        values.put(DatabaseHelper.COL_A_STATUS, answer.getStatus());
        values.put(DatabaseHelper.COL_A_LIKE_COUNT, answer.getLikes());
        values.put(DatabaseHelper.COL_A_COMMENT_COUNT, answer.getCommentCount());
        values.put(DatabaseHelper.COL_A_CREATED_AT, answer.getCreatedAt());
        values.put(DatabaseHelper.COL_A_SYNC_STATUS, DatabaseHelper.SYNC_STATUS_SYNCED);
        values.put(DatabaseHelper.COL_A_AUTHOR_NAME, answer.getUserName());
        values.put(DatabaseHelper.COL_A_AUTHOR_AVATAR, answer.getUserAvatarUrl());
        db.insertWithOnConflict(DatabaseHelper.TABLE_ANSWER, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private Question readQuestion(Cursor cursor) {
        Question question = new Question();
        question.setQuestionId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_ID)));
        question.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_USER_ID)));
        question.setMajorId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_MAJOR_ID)));
        question.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_TITLE)));
        question.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_CONTENT)));
        question.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_IMAGE_URL)));
        question.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_STATUS)));
        question.setLikeCount(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_LIKE_COUNT)));
        question.setAnswerCount(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_ANSWER_COUNT)));
        question.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_CREATED_AT)));
        question.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_UPDATED_AT)));
        question.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_AUTHOR_NAME)));
        question.setUserAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_AUTHOR_AVATAR)));
        return question;
    }

    private Answer readAnswer(Cursor cursor) {
        Answer answer = new Answer();
        answer.setAnswerId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_ID)));
        answer.setQuestionId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_QUESTION_ID)));
        answer.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_USER_ID)));
        answer.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_CONTENT)));
        answer.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_STATUS)));
        answer.setLikes(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_LIKE_COUNT)));
        answer.setCommentCount(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_COMMENT_COUNT)));
        answer.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_CREATED_AT)));
        answer.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_AUTHOR_NAME)));
        answer.setUserAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_AUTHOR_AVATAR)));
        return answer;
    }
}
