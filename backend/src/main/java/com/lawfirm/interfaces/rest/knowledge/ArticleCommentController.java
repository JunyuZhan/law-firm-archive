package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateArticleCommentCommand;
import com.lawfirm.application.knowledge.dto.ArticleCommentDTO;
import com.lawfirm.application.knowledge.service.ArticleCommentAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章评论接口（M10-022）
 * 知识库是知识共享平台：
 * - 全员可查看、创建评论
 * - 只能删除自己的评论（服务层验证所有权）
 */
@Tag(name = "文章评论", description = "文章评论相关接口")
@RestController
@RequestMapping("/knowledge/article-comment")
@RequiredArgsConstructor
public class ArticleCommentController {

    private final ArticleCommentAppService commentAppService;

    @Operation(summary = "创建评论")
    @PostMapping
    @RequirePermission("knowledge:article:list")
    @OperationLog(module = "文章评论", action = "创建评论")
    public Result<ArticleCommentDTO> createComment(@RequestBody CreateArticleCommentCommand command) {
        return Result.success(commentAppService.createComment(command));
    }

    @Operation(summary = "获取文章的所有评论")
    @GetMapping("/article/{articleId}")
    @RequirePermission("knowledge:article:list")
    public Result<List<ArticleCommentDTO>> getArticleComments(@PathVariable Long articleId) {
        return Result.success(commentAppService.getArticleComments(articleId));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    @RequirePermission("knowledge:article:list")
    @OperationLog(module = "文章评论", action = "删除评论")
    public Result<Void> deleteComment(@PathVariable Long id) {
        // 服务层验证：只能删除自己的评论
        commentAppService.deleteComment(id);
        return Result.success();
    }

    @Operation(summary = "点赞评论")
    @PostMapping("/{id}/like")
    @RequirePermission("knowledge:article:list")
    @OperationLog(module = "文章评论", action = "点赞评论")
    public Result<Void> likeComment(@PathVariable Long id) {
        commentAppService.likeComment(id);
        return Result.success();
    }
}

