package vn.edu.ueh.melobi_nhom5.entity;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class Message {

    private String messageId;
    private String conversationId;
    private String senderId;
    private String content;
    private String imageUrl;
    private Date sentAt;
    private boolean isRead;
    private boolean pending;

    public Message() {
    }

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    @PropertyName("isRead")
    public boolean getIsRead() {
        return isRead;
    }

    @PropertyName("isRead")
    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    @Exclude
    public boolean isRead() {
        return isRead;
    }

    @Exclude
    public boolean isPending() {
        return pending;
    }

    @Exclude
    public void setPending(boolean pending) {
        this.pending = pending;
    }
}
