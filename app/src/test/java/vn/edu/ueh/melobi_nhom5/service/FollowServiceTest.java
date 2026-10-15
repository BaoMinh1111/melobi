
package vn.edu.ueh.melobi_nhom5.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class FollowServiceTest {

    @Test
    public void cannotFollowSelf() {
        FollowService service = new FollowService();

        try {
            service.followUser("U01", "U01");
            fail("Phai chan follow chinh minh");
        } catch (IllegalArgumentException expected) {
            // Dung nhu mong doi
        }
    }

    @Test
    public void followAndUnfollow() {
        FollowService service = new FollowService();

        assertTrue(service.followUser("U01", "U02"));
        assertTrue(service.isFollowing("U01", "U02"));

        assertFalse(service.followUser("U01", "U02"));

        assertTrue(service.unfollowUser("U01", "U02"));
        assertFalse(service.isFollowing("U01", "U02"));

        assertFalse(service.unfollowUser("U01", "U02"));
    }
}
