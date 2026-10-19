package vn.edu.ueh.melobi_nhom5.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.repository.AnswerRepository;

public class AnswerServiceTest {

    private TestAnswerRepository fakeRepository;
    private AnswerService answerService;

    static class TestAnswerRepository extends AnswerRepository {
        Answer addedAnswer;
        Exception errorToThrow;
        List<Answer> answersToReturn;
        boolean addAnswerCalled = false;
        boolean getAnswersCalled = false;
        boolean updateAnswerCalled = false;
        boolean deleteAnswerCalled = false;
        boolean toggleLikeCalled = false;

        @Override
        public void addAnswer(Answer answer, Callback<Answer> callback) {
            addAnswerCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                this.addedAnswer = answer;
                if (answer.getId() == null || answer.getId().isEmpty()) {
                    answer.setId("A_GENERATED_ID");
                }
                callback.onSuccess(answer);
            }
        }

        @Override
        public void getAnswersByQuestionId(String questionId, Callback<List<Answer>> callback) {
            getAnswersCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                callback.onSuccess(answersToReturn != null ? answersToReturn : new ArrayList<>());
            }
        }

        @Override
        public void updateAnswer(String answerId, String newContent, Callback<Void> callback) {
            updateAnswerCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                callback.onSuccess(null);
            }
        }

        @Override
        public void deleteAnswer(Answer answer, Callback<Void> callback) {
            deleteAnswerCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                callback.onSuccess(null);
            }
        }

        @Override
        public void toggleLike(String answerId, String userId, Callback<Answer> callback) {
            toggleLikeCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                Answer ans = new Answer();
                ans.setId(answerId);
                callback.onSuccess(ans);
            }
        }
    }

    @Before
    public void setUp() {
        fakeRepository = new TestAnswerRepository();
        answerService = new AnswerService(fakeRepository);
    }

    @Test
    public void testCreateAnswer_Success() {
        Answer inputAnswer = new Answer();
        inputAnswer.setQuestionId("Q100");
        inputAnswer.setUserId("U100");
        inputAnswer.setContent("Đây là câu trả lời thử nghiệm");

        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<Answer> resultRef = new AtomicReference<>();

        answerService.createAnswer(inputAnswer, new AnswerService.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
                successCalled.set(true);
                resultRef.set(result);
            }

            @Override
            public void onError(Exception e) {
            }
        });

        assertTrue("addAnswer của Repository phải được gọi", fakeRepository.addAnswerCalled);
        assertTrue("onSuccess phải được gọi khi thêm câu trả lời thành công", successCalled.get());
        assertNotNull(resultRef.get());
        assertEquals("A_GENERATED_ID", resultRef.get().getId());
        assertEquals("Đây là câu trả lời thử nghiệm", resultRef.get().getContent());
    }

    @Test
    public void testCreateAnswer_NullAnswer_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.createAnswer((Answer) null, new AnswerService.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi answer null", errorCalled.get());
    }

    @Test
    public void testCreateAnswer_EmptyQuestionId_TriggersError() {
        Answer answer = new Answer();
        answer.setQuestionId("");
        answer.setUserId("U1");
        answer.setContent("Nội dung");

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.createAnswer(answer, new AnswerService.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi questionId rỗng", errorCalled.get());
    }

    @Test
    public void testCreateAnswer_EmptyUserId_TriggersError() {
        Answer answer = new Answer();
        answer.setQuestionId("Q1");
        answer.setUserId("   ");
        answer.setContent("Nội dung");

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.createAnswer(answer, new AnswerService.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi userId rỗng", errorCalled.get());
    }

    @Test
    public void testCreateAnswer_EmptyContent_TriggersError() {
        Answer answer = new Answer();
        answer.setQuestionId("Q1");
        answer.setUserId("U1");
        answer.setContent("   ");

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.createAnswer(answer, new AnswerService.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi content rỗng", errorCalled.get());
    }

    @Test
    public void testGetAnswersByQuestion_Success() {
        List<Answer> fakeList = new ArrayList<>();
        fakeList.add(new Answer("A1", "Q1", "U1", "Name1", "", "Content1", 1000L, 1000L, 5, 0, null, Answer.STATUS_ACTIVE));
        fakeRepository.answersToReturn = fakeList;

        AtomicBoolean successCalled = new AtomicBoolean(false);

        answerService.getAnswersByQuestion("Q1", new AnswerService.Callback<List<Answer>>() {
            @Override
            public void onSuccess(List<Answer> result) {
                successCalled.set(true);
                assertEquals(1, result.size());
            }

            @Override
            public void onError(Exception e) {
            }
        });

        assertTrue(fakeRepository.getAnswersCalled);
        assertTrue("onSuccess phải được gọi khi lấy danh sách câu trả lời", successCalled.get());
    }

    @Test
    public void testGetAnswersByQuestion_EmptyId_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.getAnswersByQuestion("", new AnswerService.Callback<List<Answer>>() {
            @Override
            public void onSuccess(List<Answer> result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi questionId rỗng", errorCalled.get());
    }

    @Test
    public void testUpdateAnswer_InvalidParams_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.updateAnswer("", "Nội dung mới", new AnswerService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
            }
        });

        assertTrue(errorCalled.get());
    }

    @Test
    public void testDeleteAnswer_NullAnswer_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.deleteAnswer(null, new AnswerService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
            }
        });

        assertTrue(errorCalled.get());
    }

    @Test
    public void testToggleLike_InvalidParams_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        answerService.toggleLike("A1", "", new AnswerService.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
            }
        });

        assertTrue(errorCalled.get());
    }
}
