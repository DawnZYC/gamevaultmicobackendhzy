package com.sg.nusiss.gamevaultmicobackendhzy.dto.forum;

import com.sg.nusiss.gamevaultmicobackendhzy.entity.forum.ForumContent;
import com.sg.nusiss.gamevaultmicobackendhzy.entity.forum.ForumUser;

import java.time.LocalDateTime;

/**
 * 回复响应数据传输对象
 * 用于向前端返回回复信息（包含统计数据、作者信息和回复关系）
 */
public class ReplyResponseDTO {

    private Long replyId;           // 回复ID (对应 contentId)
    private Long parentId;          // 父帖子ID
    private Long replyTo;           // 🔥 回复的目标回复ID (楼中楼)
    private String replyToName;     // 🔥 被回复用户的名称
    private String body;            // 回复内容
    private String bodyPlain;       // 纯文本内容
    private Long authorId;          // 作者ID
    private String authorName;      // 作者用户名
    private String authorNickname;  // 作者昵称
    private String authorAvatarUrl; // 作者头像
    private Integer likeCount;      // 点赞数
    private Boolean isLiked;        // 当前用户是否点赞
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // 默认构造函数
    public ReplyResponseDTO() {}

    /**
     * 从 ForumContent 实体创建 DTO
     */
    public static ReplyResponseDTO fromContent(ForumContent content) {
        ReplyResponseDTO dto = new ReplyResponseDTO();
        dto.replyId = content.getContentId();
        dto.parentId = content.getParentId();
        dto.replyTo = content.getReplyTo();  // 🔥 设置 replyTo
        dto.body = content.getBody();
        dto.bodyPlain = content.getBodyPlain();
        dto.authorId = content.getAuthorId();
        dto.createdDate = content.getCreatedDate();
        dto.updatedDate = content.getUpdatedDate();
        dto.likeCount = content.getLikeCount() != null ? content.getLikeCount() : 0;
        dto.isLiked = content.getIsLikedByCurrentUser() != null ? content.getIsLikedByCurrentUser() : false;
        return dto;
    }

    /**
     * 从 ForumContent 和 ForumUser 创建完整的 DTO
     */
    public static ReplyResponseDTO fromContentAndUser(ForumContent content, ForumUser author) {
        ReplyResponseDTO dto = fromContent(content);
        if (author != null) {
            dto.authorName = author.getUsername();
            dto.authorNickname = author.getNickname();
            dto.authorAvatarUrl = author.getAvatarUrl();
        }
        return dto;
    }

    /**
     * 从 ForumContent、作者信息和被回复用户信息创建完整的 DTO
     */
    public static ReplyResponseDTO fromContentAndUsers(
            ForumContent content,
            ForumUser author,
            ForumUser replyToUser
    ) {
        ReplyResponseDTO dto = fromContentAndUser(content, author);
        if (replyToUser != null && content.getReplyTo() != null) {
            // 优先使用昵称，如果没有则使用用户名
            dto.replyToName = replyToUser.getNickname() != null ?
                    replyToUser.getNickname() : replyToUser.getUsername();
        }
        return dto;
    }

    // Getters and Setters
    public Long getReplyId() {
        return replyId;
    }

    public void setReplyId(Long replyId) {
        this.replyId = replyId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(Long replyTo) {
        this.replyTo = replyTo;
    }

    public String getReplyToName() {
        return replyToName;
    }

    public void setReplyToName(String replyToName) {
        this.replyToName = replyToName;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBodyPlain() {
        return bodyPlain;
    }

    public void setBodyPlain(String bodyPlain) {
        this.bodyPlain = bodyPlain;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorNickname() {
        return authorNickname;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Boolean getIsLiked() {
        return isLiked;
    }

    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public String toString() {
        return "ReplyResponseDTO{" +
                "replyId=" + replyId +
                ", parentId=" + parentId +
                ", replyTo=" + replyTo +
                ", replyToName='" + replyToName + '\'' +
                ", authorName='" + authorName + '\'' +
                ", likeCount=" + likeCount +
                ", createdDate=" + createdDate +
                '}';
    }
}