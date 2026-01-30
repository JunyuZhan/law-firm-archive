package com.lawfirm.interfaces.rest.knowledge;

import com.lawfirm.application.knowledge.command.CreateArticleCommand;
import com.lawfirm.application.knowledge.dto.KnowledgeArticleDTO;
import com.lawfirm.application.knowledge.dto.KnowledgeArticleQueryDTO;
import com.lawfirm.application.knowledge.service.KnowledgeArticleAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 经验文章接口 知识库是知识共享平台： - 全员可查看任何人的文章 - 只能编辑/删除自己创建的文章（服务层验证所有权） */
@Tag(name = "经验分享", description = "经验文章发布、查询相关接口")
@RestController
@RequestMapping("/knowledge/article")
@RequiredArgsConstructor
public class KnowledgeArticleController {

  /** 经验文章应用服务. */
  private final KnowledgeArticleAppService articleAppService;

  /**
   * 分页查询文章
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询文章")
  @GetMapping
  @RequirePermission("knowledge:article:list")
  public Result<PageResult<KnowledgeArticleDTO>> listArticles(
      final KnowledgeArticleQueryDTO query) {
    return Result.success(articleAppService.listArticles(query));
  }

  /**
   * 获取文章详情
   *
   * @param id 文章ID
   * @return 文章详情
   */
  @Operation(summary = "获取文章详情")
  @GetMapping("/{id}")
  @RequirePermission("knowledge:article:list")
  public Result<KnowledgeArticleDTO> getArticleById(@PathVariable final Long id) {
    return Result.success(articleAppService.getArticleById(id));
  }

  /**
   * 创建文章
   *
   * @param command 创建命令
   * @return 创建的文章
   */
  @Operation(summary = "创建文章")
  @PostMapping
  @RequirePermission("knowledge:article:list")
  @OperationLog(module = "经验分享", action = "创建文章")
  public Result<KnowledgeArticleDTO> createArticle(
      @RequestBody final CreateArticleCommand command) {
    return Result.success(articleAppService.createArticle(command));
  }

  /**
   * 更新文章
   *
   * @param id 文章ID
   * @param command 更新命令
   * @return 更新后的文章
   */
  @Operation(summary = "更新文章")
  @PutMapping("/{id}")
  @RequirePermission("knowledge:article:list")
  @OperationLog(module = "经验分享", action = "更新文章")
  public Result<KnowledgeArticleDTO> updateArticle(
      @PathVariable final Long id, @RequestBody final CreateArticleCommand command) {
    // 服务层验证：只能编辑自己的文章
    return Result.success(articleAppService.updateArticle(id, command));
  }

  /**
   * 删除文章
   *
   * @param id 文章ID
   * @return 操作结果
   */
  @Operation(summary = "删除文章")
  @DeleteMapping("/{id}")
  @RequirePermission("knowledge:article:list")
  @OperationLog(module = "经验分享", action = "删除文章")
  public Result<Void> deleteArticle(@PathVariable final Long id) {
    // 服务层验证：只能删除自己的文章
    articleAppService.deleteArticle(id);
    return Result.success();
  }

  /**
   * 发布文章
   *
   * @param id 文章ID
   * @return 发布后的文章
   */
  @Operation(summary = "发布文章")
  @PostMapping("/{id}/publish")
  @RequirePermission("knowledge:article:list")
  @OperationLog(module = "经验分享", action = "发布文章")
  public Result<KnowledgeArticleDTO> publishArticle(@PathVariable final Long id) {
    // 服务层验证：只能发布自己的文章
    return Result.success(articleAppService.publishArticle(id));
  }

  /**
   * 归档文章
   *
   * @param id 文章ID
   * @return 操作结果
   */
  @Operation(summary = "归档文章")
  @PostMapping("/{id}/archive")
  @RequirePermission("knowledge:article:list")
  @OperationLog(module = "经验分享", action = "归档文章")
  public Result<Void> archiveArticle(@PathVariable final Long id) {
    articleAppService.archiveArticle(id);
    return Result.success();
  }

  /**
   * 点赞文章
   *
   * @param id 文章ID
   * @return 操作结果
   */
  @Operation(summary = "点赞文章")
  @PostMapping("/{id}/like")
  @RequirePermission("knowledge:article:list")
  public Result<Void> likeArticle(@PathVariable final Long id) {
    articleAppService.likeArticle(id);
    return Result.success();
  }

  @Operation(summary = "获取我的文章")
  @GetMapping("/my")
  @RequirePermission("knowledge:article:list")
  public Result<List<KnowledgeArticleDTO>> getMyArticles() {
    return Result.success(articleAppService.getMyArticles());
  }

  /**
   * 收藏文章
   *
   * @param id 文章ID
   * @return 空结果
   */
  @Operation(summary = "收藏文章")
  @PostMapping("/{id}/collect")
  @RequirePermission("knowledge:article:list")
  @OperationLog(module = "经验分享", action = "收藏文章")
  public Result<Void> collectArticle(@PathVariable final Long id) {
    articleAppService.collectArticle(id);
    return Result.success();
  }

  /**
   * 取消收藏文章
   *
   * @param id 文章ID
   * @return 空结果
   */
  @Operation(summary = "取消收藏文章")
  @DeleteMapping("/{id}/collect")
  @RequirePermission("knowledge:article:list")
  @OperationLog(module = "经验分享", action = "取消收藏文章")
  public Result<Void> uncollectArticle(@PathVariable final Long id) {
    articleAppService.uncollectArticle(id);
    return Result.success();
  }

  @Operation(summary = "获取我的收藏文章")
  @GetMapping("/collected")
  @RequirePermission("knowledge:article:list")
  public Result<List<KnowledgeArticleDTO>> getMyCollectedArticles() {
    return Result.success(articleAppService.getMyCollectedArticles());
  }
}
