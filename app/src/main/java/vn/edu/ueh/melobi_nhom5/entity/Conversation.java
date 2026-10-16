
package vn.edu.ueh.melobi_nhom5.entity;

import java.util.Date;

public class Conversation {

    // Khai bao thuoc tinh
    private String conversationId;
    private String user1Id;
    private String user2Id;
    private String lastMessage;
    private Date updatedAt;

    // Constructor rong
    public Conversation() {
    }

    // Constructor day du
    public Conversation(String conversationId,
                        String user1Id,
                        String user2Id,
                        String lastMessage,
                        Date updatedAt) {
        this.conversationId = conversationId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.lastMessage = lastMessage;
        this.updatedAt = updatedAt;
    }

    // Getter va Setter
    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId1() {
        return user1Id;
    }

    public void setUserId1(String userId1) {
        this.user1Id = userId1;
    }

    public String getUserId2() {
        return user2Id;
    }

    public void setUserId2(String userId2) {
        this.user2Id = userId2;
    }

    public String getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }

    public String getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
