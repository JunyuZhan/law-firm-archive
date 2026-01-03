package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateArticleCommand;
import com.lawfirm.application.knowledge.dto.*;
import com.lawfirm.application.knowledge.service.KnowledgeArticleAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 经验文章接口
 */
@Tag(name = "经验分享", description = "经验文章发布、查询相关接口")
@RestController
@RequestMapping("/knowledge/article")
@RequiredArgsConstructor
public class KnowledgeArticleController {

    private final KnowledgeArticleAppService articleAppService;

    @Operation(summary = "分页查询文章")
    @GetMapping
    public Result<PageResult<KnowledgeArticleDTO>> listArticles(KnowledgeArticleQueryDTO query) {
        return Result.success(articleAppService.listArticles(query));
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/{id}")
    public Result<KnowledgeArticleDTO> getArticleById(@PathVariable Long id) {
        return Result.success(articleAppService.getArticleById(id));
    }

    @Operation(summary = "创建文章")
    @PostMapping
    @OperationLog(module = "经验分享", action = "创建文章")
    public Result<KnowledgeArticleDTO> createArticle(@RequestBody CreateArticleCommand command) {
        return Result.success(articleAppService.createArticle(command));
    }

    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    @OperationLog(module = "经验分享", action = "更新文章")
    public Result<KnowledgeArticleDTO> updateArticle(@PathVariable Long id, @RequestBody CreateArticleCommand command) {
        return Result.success(articleAppService.updateArticle(id, command));
    }

    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    @OperationLog(module = "经验分享", action = "删除文章")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleAppService.deleteArticle(id);
        return Result.success();
    }

    @Operation(summary = "发布文章")
    @PostMapping("/{id}/publish")
    @OperationLog(module = "经验分享", action = "发布文章")
    public Result<KnowledgeArticleDTO> publishArticle(@PathVariable Long id) {
        return Result.success(articleAppService.publishArticle(id));
    }

    @Operation(summary = "归档文章")
    @PostMapping("/{id}/archive")
    @OperationLog(module = "经验分享", action = "归档文章")
    public Result<Void> archiveArticle(@PathVariable Long id) {
        articleAppService.archiveArticle(id);
        return Result.success();
    }

    @Operation(summary = "点赞文章")
    @PostMapping("/{id}/like")
    public Result<Void> likeArticle(@PathVariable Long id) {
        articleAppService.likeArticle(id);
        return Result.success();
    }

    @Operation(summary = "获取我的文章")
    @GetMapping("/my")
    public Result<List<KnowledgeArticleDTO>> getMyArticles() {
        return Result.success(articleAppService.getMyArticles());
    }

    @Operation(summary = "收藏文章")
    @PostMapping("/{id}/collect")
    @OperationLog(module = "经验分享", action = "收藏文章")
    public Result<Void> collectArticle(@PathVariable Long id) {
        articleAppService.collectArticle(id);
        return Result.success();
    }

    @Operation(summary = "取消收藏文章")
    @DeleteMapping("/{id}/collect")
    @OperationLog(module = "经验分享", action = "取消收藏文章")
    public Result<Void> uncollectArticle(@PathVariable Long id) {
        articleAppService.uncollectArticle(id);
        return Result.success();
    }

    @Operation(summary = "获取我的收藏文章")
    @GetMapping("/collected")
    public Result<List<KnowledgeArticleDTO>> getMyCollectedArticles() {
        return Result.success(articleAppService.getMyCollectedArticles());
    }
}
