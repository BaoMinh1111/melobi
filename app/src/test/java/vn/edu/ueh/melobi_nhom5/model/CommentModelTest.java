package vn.edu.ueh.melobi_nhom5.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class CommentModelTest {

    @Test
    public void testGettersAndSetters() {
        Comment comment = new Comment();
        comment.setId("C1");
        comment.setAnswerId("A1");
        comment.setUserId("U1");
        comment.setUserName("Alice");
        comment.setUserAvatarUrl("http://avatar.jpg");
        comment.setContent("Great answer!");
        comment.setCreatedAt(10000L);
        comment.setUpdatedAt(20000L);

        assertEquals("C1", comment.getId());
        assertEquals("C1", comment.getCommentId());
        assertEquals("A1", comment.getAnswerId());
        assertEquals("U1", comment.getUserId());
        assertEquals("Alice", comment.getUserName());
        assertEquals("http://avatar.jpg", comment.getUserAvatarUrl());
        assertEquals("Great answer!", comment.getContent());
        assertEquals(10000L, comment.getCreatedAt());
        assertEquals(20000L, comment.getUpdatedAt());
    }

    @Test
    public void testParametrizedConstructor() {
        Comment comment = new Comment("C2", "A2", "U2", "Bob", "http://bob.png", "Thanks!", 500L, 600L);

        assertEquals("C2", comment.getId());
        assertEquals("A2", comment.getAnswerId());
        assertEquals("U2", comment.getUserId());
        assertEquals("Bob", comment.getUserName());
        assertEquals("http://bob.png", comment.getUserAvatarUrl());
        assertEquals("Thanks!", comment.getContent());
        assertEquals(500L, comment.getCreatedAt());
        assertEquals(600L, comment.getUpdatedAt());
    }
}
