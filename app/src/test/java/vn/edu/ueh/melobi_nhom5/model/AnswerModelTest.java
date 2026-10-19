package vn.edu.ueh.melobi_nhom5.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AnswerModelTest {

    @Test
    public void testDefaultConstructor() {
        Answer answer = new Answer();
        assertNotNull(answer.getLikedUserIds());
        assertTrue(answer.getLikedUserIds().isEmpty());
        assertEquals(Answer.STATUS_ACTIVE, answer.getStatus());
        assertFalse(answer.isHidden());
    }

    @Test
    public void testIsLikedBy() {
        Answer answer = new Answer();
        assertFalse(answer.isLikedBy("U1"));

        List<String> likedUsers = new ArrayList<>();
        likedUsers.add("U1");
        likedUsers.add("U2");
        answer.setLikedUserIds(likedUsers);

        assertTrue(answer.isLikedBy("U1"));
        assertTrue(answer.isLikedBy("U2"));
        assertFalse(answer.isLikedBy("U3"));
        assertFalse(answer.isLikedBy(null));
    }

    @Test
    public void testHideAnswer() {
        Answer answer = new Answer();
        assertFalse(answer.isHidden());

        answer.hideAnswer();

        assertTrue(answer.isHidden());
        assertEquals(Answer.STATUS_HIDDEN, answer.getStatus());
    }

    @Test
    public void testGettersAndSetters() {
        Answer answer = new Answer();
        answer.setId("ANS_1");
        answer.setQuestionId("Q_1");
        answer.setUserId("U_1");
        answer.setUserName("John");
        answer.setUserAvatarUrl("http://avatar.url");
        answer.setContent("Answer content");
        answer.setCreatedAt(1000L);
        answer.setUpdatedAt(2000L);
        answer.setLikes(10);
        answer.setCommentCount(3);

        assertEquals("ANS_1", answer.getId());
        assertEquals("ANS_1", answer.getAnswerId());
        assertEquals("Q_1", answer.getQuestionId());
        assertEquals("U_1", answer.getUserId());
        assertEquals("John", answer.getUserName());
        assertEquals("http://avatar.url", answer.getUserAvatarUrl());
        assertEquals("Answer content", answer.getContent());
        assertEquals(1000L, answer.getCreatedAt());
        assertEquals(2000L, answer.getUpdatedAt());
        assertEquals(10, answer.getLikes());
        assertEquals(10, answer.getLikeCount());
        assertEquals(3, answer.getCommentCount());
    }
}
