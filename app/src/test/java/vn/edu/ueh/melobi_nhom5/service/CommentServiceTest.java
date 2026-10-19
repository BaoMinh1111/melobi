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

import vn.edu.ueh.melobi_nhom5.model.Comment;
import vn.edu.ueh.melobi_nhom5.repository.CommentRepository;

public class CommentServiceTest {

    private TestCommentRepository fakeRepository;
    private CommentService commentService;

    static class TestCommentRepository extends CommentRepository {
        Comment addedComment;
        Exception errorToThrow;
        List<Comment> commentsToReturn;
        boolean addCommentCalled = false;
        boolean getCommentsCalled = false;
        boolean updateCommentCalled = false;
        boolean deleteCommentCalled = false;

        @Override
        public void addComment(Comment comment, Callback<Comment> callback) {
            addCommentCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                this.addedComment = comment;
                if (comment.getId() == null || comment.getId().isEmpty()) {
                    comment.setId("C_GENERATED_ID");
                }
                callback.onSuccess(comment);
            }
        }

        @Override
        public void getCommentsByAnswerId(String answerId, Callback<List<Comment>> callback) {
            getCommentsCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                callback.onSuccess(commentsToReturn != null ? commentsToReturn : new ArrayList<>());
            }
        }

        @Override
        public void updateComment(String commentId, String newContent, Callback<Void> callback) {
            updateCommentCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                callback.onSuccess(null);
            }
        }

        @Override
        public void deleteComment(Comment comment, Callback<Void> callback) {
            deleteCommentCalled = true;
            if (errorToThrow != null) {
                callback.onError(errorToThrow);
            } else {
                callback.onSuccess(null);
            }
        }
    }

    @Before
    public void setUp() {
        fakeRepository = new TestCommentRepository();
        commentService = new CommentService(fakeRepository);
    }

    @Test
    public void testCreateComment_Success() {
        Comment inputComment = new Comment();
        inputComment.setAnswerId("A100");
        inputComment.setUserId("U100");
        inputComment.setContent("Bình luận thử nghiệm");

        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicReference<Comment> resultRef = new AtomicReference<>();

        commentService.createComment(inputComment, new CommentService.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                successCalled.set(true);
                resultRef.set(result);
            }

            @Override
            public void onError(Exception e) {
            }
        });

        assertTrue("addComment của Repository phải được gọi", fakeRepository.addCommentCalled);
        assertTrue("onSuccess phải được gọi khi thêm bình luận thành công", successCalled.get());
        assertNotNull(resultRef.get());
        assertEquals("C_GENERATED_ID", resultRef.get().getId());
        assertEquals("Bình luận thử nghiệm", resultRef.get().getContent());
    }

    @Test
    public void testCreateComment_NullComment_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        commentService.createComment((Comment) null, new CommentService.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi comment null", errorCalled.get());
    }

    @Test
    public void testCreateComment_EmptyAnswerId_TriggersError() {
        Comment comment = new Comment();
        comment.setAnswerId("");
        comment.setUserId("U1");
        comment.setContent("Bình luận");

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        commentService.createComment(comment, new CommentService.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi answerId rỗng", errorCalled.get());
    }

    @Test
    public void testCreateComment_EmptyUserId_TriggersError() {
        Comment comment = new Comment();
        comment.setAnswerId("A1");
        comment.setUserId("  ");
        comment.setContent("Bình luận");

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        commentService.createComment(comment, new CommentService.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
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
    public void testCreateComment_EmptyContent_TriggersError() {
        Comment comment = new Comment();
        comment.setAnswerId("A1");
        comment.setUserId("U1");
        comment.setContent("  ");

        AtomicBoolean errorCalled = new AtomicBoolean(false);

        commentService.createComment(comment, new CommentService.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
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
    public void testGetCommentsByAnswer_Success() {
        List<Comment> fakeList = new ArrayList<>();
        fakeList.add(new Comment("C1", "A1", "U1", "User1", "", "Nội dung 1", 1000L, 1000L));
        fakeRepository.commentsToReturn = fakeList;

        AtomicBoolean successCalled = new AtomicBoolean(false);

        commentService.getCommentsByAnswer("A1", new CommentService.Callback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> result) {
                successCalled.set(true);
                assertEquals(1, result.size());
            }

            @Override
            public void onError(Exception e) {
            }
        });

        assertTrue(fakeRepository.getCommentsCalled);
        assertTrue("onSuccess phải được gọi khi lấy bình luận", successCalled.get());
    }

    @Test
    public void testGetCommentsByAnswer_EmptyAnswerId_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        commentService.getCommentsByAnswer("", new CommentService.Callback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue("onError phải được gọi khi answerId rỗng", errorCalled.get());
    }

    @Test
    public void testUpdateComment_EmptyContent_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        commentService.updateComment("C1", "   ", new CommentService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue(errorCalled.get());
    }

    @Test
    public void testDeleteComment_NullComment_TriggersError() {
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        commentService.deleteComment(null, new CommentService.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(Exception e) {
                errorCalled.set(true);
                assertTrue(e instanceof IllegalArgumentException);
            }
        });

        assertTrue(errorCalled.get());
    }
}
