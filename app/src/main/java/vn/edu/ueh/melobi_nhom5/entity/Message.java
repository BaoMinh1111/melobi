
package vn.edu.ueh.melobi_nhom5.entity;

import java.util.Date;

public class Message {

    // Khai bao cac thuoc tinh
    private String messageId;
    private String conversationId;
    private String senderId;
    private String content;
    private Date sentAt;
    private boolean isRead;

    // Constructor rong
    public Message() {
    }

    // Constructor day du tham so
    public Message(String messageId,
                   String conversationId,
                   String senderId,
                   String content,
                   Date sentAt,
                   boolean isRead) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.sentAt = sentAt;
        this.isRead = isRead;
    }

    // Getter va Setter

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    public boolean isRead() {
        return isRead;
    }
}
