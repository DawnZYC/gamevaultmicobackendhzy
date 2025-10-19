package com.sg.nusiss.gamevaultmicobackendhzy.service.forum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sg.nusiss.gamevaultmicobackendhzy.entity.forum.ForumContent;
import com.sg.nusiss.gamevaultmicobackendhzy.mapper.forum.ForumContentMapper;
import com.sg.nusiss.gamevaultmicobackendhzy.mapper.forum.ForumMetricMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 帖子业务服务类
 * 处理帖子相关的业务逻辑
 */
@Service
public class ForumPostService {

    private static final Logger logger = LoggerFactory.getLogger(ForumPostService.class);

    @Autowired
    private ForumContentMapper contentMapper;

    @Autowired
    private ForumMetricMapper metricMapper;

    @Autowired
    private ForumContentLikeService contentLikeService;

    /**
     * 创建新帖子
     */
    public ForumContent createPost(String title, String body, Long authorId) {
        logger.info("创建新帖子 - 作者ID: {}, 标题: {}", authorId, title);

        // 参数验证
        if (title == null || title.trim().isEmpty()) {
            logger.warn("创建帖子失败 - 标题为空, 作者ID: {}", authorId);
            throw new IllegalArgumentException("帖子标题不能为空");
        }
        if (body == null || body.trim().isEmpty()) {
            logger.warn("创建帖子失败 - 内容为空, 作者ID: {}", authorId);
            throw new IllegalArgumentException("帖子内容不能为空");
        }
        if (authorId == null) {
            logger.warn("创建帖子失败 - 作者ID为空");
            throw new IllegalArgumentException("作者ID不能为空");
        }

        try {
            // 创建帖子实体
            ForumContent post = new ForumContent("post", title.trim(), body.trim(), authorId);

            // 保存到数据库
            int result = contentMapper.insert(post);
            if (result > 0) {
                logger.info("帖子创建成功 - 帖子ID: {}, 作者ID: {}", post.getContentId(), authorId);
                // 初始化统计数据
                initializePostMetrics(post.getContentId());
                return post;
            } else {
                logger.error("创建帖子失败 - 数据库插入返回0, 作者ID: {}", authorId);
                throw new RuntimeException("创建帖子失败");
            }
        } catch (Exception e) {
            logger.error("创建帖子异常 - 作者ID: {}, 异常信息: {}", authorId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据ID获取帖子详情
     */
    public ForumContent getPostById(Long id, Long currentUserId) {
        ForumContent post = contentMapper.findById(id);  // 已经包含统计数据

        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        logger.info("从数据库查到的点赞数: {}", post.getLikeCount());
        // 只需要查询点赞状态
        if (currentUserId != null) {
            boolean isLiked = contentLikeService.isLiked(id, currentUserId);
            post.setIsLikedByCurrentUser(isLiked);
        }

        return post;
    }

    /**
     * 获取帖子列表（带当前用户的点赞状态）
     */
    public List<ForumContent> getPostList(int page, int size, Long currentUserId) {
        List<ForumContent> posts = contentMapper.findActivePosts(page * size, size);


        // 设置点赞状态
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream()
                    .map(ForumContent::getContentId)
                    .collect(Collectors.toList());

            Map<Long, Boolean> likeStatus = contentLikeService
                    .batchCheckLikeStatus(currentUserId, postIds);

            posts.forEach(post ->
                    post.setIsLikedByCurrentUser(
                            likeStatus.getOrDefault(post.getContentId(), false)
                    )
            );
        }

        return posts;
    }
    /**
     * 统计帖子总数
     */
    public int getPostCount() {
        return contentMapper.countActivePosts();
    }

    /**
     * 搜索帖子
     */
    public List<ForumContent> searchPosts(String keyword, int page, int size, Long currentUserId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPostList(page, size, currentUserId);
        }

        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        int offset = page * size;
        List<ForumContent> posts = contentMapper.searchPosts(keyword.trim(), offset, size);

        // 设置点赞状态
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream()
                    .map(ForumContent::getContentId)
                    .collect(Collectors.toList());

            Map<Long, Boolean> likeStatus = contentLikeService
                    .batchCheckLikeStatus(currentUserId, postIds);

            posts.forEach(post ->
                    post.setIsLikedByCurrentUser(
                            likeStatus.getOrDefault(post.getContentId(), false)
                    )
            );
        }
        return posts;
    }

    /**
     * 统计搜索结果数量
     */
    public int getSearchCount(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getPostCount();
        }
        return contentMapper.countSearchPosts(keyword.trim());
    }

    /**
     * 增加帖子浏览量
     */
    public void incrementViewCount(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }

        // 验证帖子存在
        ForumContent post = contentMapper.findById(postId);
        if (post == null || !post.isPost()) {
            throw new RuntimeException("帖子不存在");
        }

        // 增加浏览量
        metricMapper.incrementMetric(postId, "view_count", 1);
    }



    /**
     * 软删除帖子
     */
    public void deletePost(Long postId, Long userId) {
        if (postId == null) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }

        // 验证帖子存在
        ForumContent post = contentMapper.findById(postId);
        if (post == null || !post.isPost()) {
            throw new RuntimeException("帖子不存在");
        }

        // 权限验证：只有作者可以删除自己的帖子（这里简化处理）
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("没有权限删除此帖子");
        }

        // 执行软删除
        contentMapper.softDelete(postId);
    }

    /**
     * 更新帖子
     */
    public ForumContent updatePost(Long postId, String title, String body, Long userId) {
        // 参数验证
        if (postId == null) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }

        ForumContent post = contentMapper.findById(postId);
        if (post == null || !post.isPost()) {
            throw new RuntimeException("帖子不存在");
        }

        // 权限验证
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("没有权限编辑此帖子");
        }

        // 更新内容
        if (title != null && !title.trim().isEmpty()) {
            post.setTitle(title.trim());
        }
        if (body != null && !body.trim().isEmpty()) {
            post.setBody(body.trim());
        }
        post.setUpdatedDate(LocalDateTime.now());

        // 保存更新
        int result = contentMapper.update(post);
        if (result > 0) {
            return post;
        } else {
            throw new RuntimeException("更新帖子失败");
        }
    }

    /**
     * 初始化帖子的统计数据
     */
    private void initializePostMetrics(Long postId) {
        try {
            metricMapper.setMetricValue(postId, "view_count", 0);
            metricMapper.setMetricValue(postId, "like_count", 0);
            metricMapper.setMetricValue(postId, "reply_count", 0);
        } catch (Exception e) {
            // 统计数据初始化失败不影响帖子创建
            System.err.println("初始化帖子统计数据失败: " + e.getMessage());
        }
    }


    /**
     * 根据作者ID获取活跃帖子列表（未删除）
     */
    public List<ForumContent> getPostsByAuthorId(Long authorId, int page, int size, Long currentUserId) {
        if (authorId == null) {
            throw new IllegalArgumentException("作者ID不能为空");
        }

        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        int offset = page * size;
        logger.info("查询用户活跃帖子 - 用户ID: {}, 页码: {}, 每页: {}", authorId, page, size);

        // 查询帖子列表（SQL查询已经包含了统计数据：view_count, like_count, reply_count）
        List<ForumContent> posts = contentMapper.selectActiveByAuthorId(authorId, offset, size);

        // 不需要再次获取统计数据，因为SQL查询已经通过JOIN获取了正确的数据
        // SQL中使用了：
        // - like_count: 从 user_content_relations 表实时统计
        // - view_count: 从 content_metrics 表获取
        // - reply_count: 从 content_metrics 表获取

        // 如果用户登录了，批量查询点赞状态
        if (currentUserId != null && !posts.isEmpty()) {
            List<Long> postIds = posts.stream()
                    .map(ForumContent::getContentId)
                    .collect(Collectors.toList());

            Map<Long, Boolean> likeStatus = contentLikeService
                    .batchCheckLikeStatus(currentUserId, postIds);

            posts.forEach(post ->
                    post.setIsLikedByCurrentUser(
                            likeStatus.getOrDefault(post.getContentId(), false)
                    )
            );
        }

        return posts;
    }

    /**
     * 统计作者的活跃帖子数（未删除）
     */
    public int getPostCountByAuthorId(Long authorId) {
        if (authorId == null) {
            throw new IllegalArgumentException("作者ID不能为空");
        }

        logger.info("统计用户活跃帖子数 - 用户ID: {}", authorId);

        // 调用统计活跃帖子的方法（只统计未删除的）
        return contentMapper.countActiveByAuthorId(authorId);
    }


    /**
     * 创建回复（支持楼中楼）
     * @param parentId 父内容ID(帖子ID)
     * @param body 回复内容
     * @param authorId 作者ID
     * @param replyTo 回复的目标回复ID(可选,如果是回复帖子则为null)
     */
    public ForumContent createReply(Long parentId, String body, Long authorId, Long replyTo) {
        logger.info("创建回复 - 父内容ID: {}, 作者ID: {}, replyTo: {}", parentId, authorId, replyTo);

        // 参数验证
        if (parentId == null) {
            throw new IllegalArgumentException("父内容ID不能为空");
        }
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        if (authorId == null) {
            throw new IllegalArgumentException("作者ID不能为空");
        }

        // 验证父内容存在(必须是帖子)
        ForumContent parent = contentMapper.findById(parentId);
        if (parent == null) {
            throw new RuntimeException("父内容不存在");
        }
        if (!"post".equals(parent.getContentType())) {
            throw new RuntimeException("只能回复帖子");
        }

        // 🔥 如果有 replyTo,验证目标回复是否存在
        if (replyTo != null) {
            ForumContent targetReply = contentMapper.findById(replyTo);
            if (targetReply == null) {
                logger.warn("目标回复不存在 - replyTo: {}", replyTo);
                throw new RuntimeException("目标回复不存在");
            }
            // 确保目标回复属于同一个帖子
            if (!targetReply.getParentId().equals(parentId)) {
                logger.warn("目标回复不属于该帖子 - 目标回复的parentId: {}, 当前parentId: {}",
                        targetReply.getParentId(), parentId);
                throw new RuntimeException("目标回复不属于该帖子");
            }
            logger.info("验证通过 - 回复目标: {}, 属于帖子: {}", replyTo, parentId);
        }

        try {
            // 🔥 创建回复实体,使用支持 replyTo 的构造函数
            ForumContent reply;
            if (replyTo != null) {
                reply = new ForumContent("reply", body.trim(), authorId, parentId, replyTo);
                logger.info("创建楼中楼回复 - 回复目标ID: {}", replyTo);
            } else {
                reply = new ForumContent("reply", body.trim(), authorId, parentId);
                logger.info("创建根回复(直接回复帖子)");
            }

            // 保存到数据库
            int result = contentMapper.insert(reply);
            if (result > 0) {
                logger.info("回复创建成功 - 回复ID: {}, 父内容ID: {}, replyTo: {}",
                        reply.getContentId(), parentId, replyTo);

                // 初始化回复的统计数据
                initializeReplyMetrics(reply.getContentId());

                // 更新父内容(帖子)的回复数 +1
                metricMapper.incrementMetric(parentId, "reply_count", 1);

                return reply;
            } else {
                throw new RuntimeException("创建回复失败");
            }
        } catch (Exception e) {
            logger.error("创建回复异常 - parentId: {}, authorId: {}, replyTo: {}",
                    parentId, authorId, replyTo, e);
            throw e;
        }
    }


    /**
     * 获取帖子的回复列表（分页）
     */
    public List<ForumContent> getRepliesByPostId(Long postId, int page, int size, Long currentUserId) {
        if (postId == null) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }

        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        int offset = page * size;

        // 查询回复列表
        List<ForumContent> replies = contentMapper.findChildren(postId, offset, size);

        // 为每个回复加载统计数据（如果 SQL 没有 JOIN）
        for (ForumContent reply : replies) {
            if (reply.getLikeCount() == null) {
                Integer likeCount = metricMapper.getMetricValue(reply.getContentId(), "like_count");
                reply.setLikeCount(likeCount != null ? likeCount : 0);
            }
        }

        // 🔥 新增：设置当前用户的点赞状态
        if (currentUserId != null && !replies.isEmpty()) {
            // 收集所有回复的ID
            List<Long> replyIds = replies.stream()
                    .map(ForumContent::getContentId)
                    .collect(Collectors.toList());

            // 批量查询点赞状态
            Map<Long, Boolean> likeStatus = contentLikeService
                    .batchCheckLikeStatus(currentUserId, replyIds);

            // 设置每个回复的点赞状态
            replies.forEach(reply ->
                    reply.setIsLikedByCurrentUser(
                            likeStatus.getOrDefault(reply.getContentId(), false)
                    )
            );
        }

        return replies;
    }
    /**
     * 统计某帖子的回复总数
     */
    public int getReplyCountByPostId(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        return contentMapper.countChildren(postId);
    }
    /**
     * 删除回复
     */
    @Transactional
    public void deleteReply(Long replyId, Long userId) {
        if (replyId == null) {
            throw new IllegalArgumentException("回复ID不能为空");
        }

        ForumContent reply = contentMapper.findById(replyId);
        if (reply == null || !reply.isReply()) {
            throw new RuntimeException("回复不存在");
        }

        // 权限验证
        if (!reply.getAuthorId().equals(userId)) {
            throw new RuntimeException("没有权限删除此回复");
        }

        // 软删除回复
        contentMapper.softDelete(replyId);

        // 更新父内容的回复数 -1
        metricMapper.incrementMetric(reply.getParentId(), "reply_count", -1);
    }



    /**
     * 根据ID获取内容(帖子或回复)
     */
    public ForumContent getContentById(Long contentId) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容ID不能为空");
        }
        return contentMapper.findById(contentId);
    }
    /**
     * 初始化回复的统计数据
     */
    private void initializeReplyMetrics(Long replyId) {
        try {
            metricMapper.setMetricValue(replyId, "like_count", 0);
        } catch (Exception e) {
            logger.error("初始化回复统计数据失败: {}", e.getMessage());
        }
    }
}