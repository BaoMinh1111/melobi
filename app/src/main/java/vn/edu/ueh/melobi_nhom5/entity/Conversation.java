
package vn.edu.ueh.melobi_nhom5.entity;

import java.util.Date;

public class Conversation {

    // Khai bao thuoc tinh
    private String conversationId;
    private String userId1;
    private String userId2;
    private String lastMessage;
    private Date updatedAt;

    // Constructor rong
    public Conversation() {
    }

    // Constructor day du
    public Conversation(String conversationId,
                        String userId1,
                        String userId2,
                        String lastMessage,
                        Date updatedAt) {
        this.conversationId = conversationId;
        this.userId1 = userId1;
        this.userId2 = userId2;
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
        return userId1;
    }

    public void setUserId1(String userId1) {
        this.userId1 = userId1;
    }

    public String getUserId2() {
        return userId2;
    }

    public void setUserId2(String userId2) {
        this.userId2 = userId2;
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
