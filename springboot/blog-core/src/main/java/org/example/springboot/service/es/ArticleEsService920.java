//package org.example.springboot.service.es;
//
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.elasticsearch._types.FieldValue;
//import co.elastic.clients.elasticsearch._types.SortOrder;
//import co.elastic.clients.elasticsearch.core.*;
//import co.elastic.clients.elasticsearch.core.search.HighlightField;
//import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
//import co.elastic.clients.util.NamedValue;
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.baomidou.mybatisplus.core.toolkit.StringUtils;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.example.springboot.entity.*;
//import org.example.springboot.entity.es.ArticleDocument;
//import org.example.springboot.mapper.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Range;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//
//@Service
//@Slf4j
//public class ArticleEsService {
//
//    private static final String INDEX_NAME = "article_index";
//    @Resource
//    private ArticleMapper articleMapper;
//
//
//    @Resource
//    private UserMapper userMapper;
//    @Resource
//    private ArticleTagMapper articleTagMapper;
//    @Resource
//    private TagMapper tagMapper;
//    @Resource
//    private CategoryMapper categoryMapper;
//
//
//    @Resource
//    private ElasticsearchClient client;
//
//    /**
//     * 创建索引
//     */
//    public void createIndex() throws IOException {
//        if(indexExist()){
//            log.info("索引：{}已经存在",INDEX_NAME);
//            return;
//        }
//        client.indices().create(c -> c
//                .index(INDEX_NAME)
//                .settings(s -> s
//                        .numberOfShards("3")
//                        .numberOfReplicas("1")
//                )
//                .mappings(m -> m
//                        // 文章ID - keyword类型，用于精确匹配
//                        .properties("id", p -> p
//                                .keyword(k -> k)
//                        )
//                        // 文章标题 - text类型，支持中文分词
//                        .properties("title", p -> p
//                                .text(t -> t
//                                        .analyzer("ik_max_word")
//                                        .searchAnalyzer("ik_smart")
//                                )
//                        )
//                        // 文章内容 - text类型，支持中文分词
//                        .properties("content", p -> p
//                                .text(t -> t
//                                        .analyzer("ik_max_word")
//                                        .searchAnalyzer("ik_smart")
//                                )
//                        )
//                        // 文章摘要 - text类型，支持中文分词
//                        .properties("summary", p -> p
//                                .text(t -> t
//                                        .analyzer("ik_max_word")
//                                        .searchAnalyzer("ik_smart")
//                                )
//                        )
//                        // 作者ID - long类型
//                        .properties("userId", p -> p
//                                .long_(l -> l)
//                        )
//                        // 浏览量 - integer类型
//                        .properties("viewCount", p -> p
//                                .integer(i -> i)
//                        )
//                        // 点赞数 - integer类型
//                        .properties("likeCount", p -> p
//                                .integer(i -> i)
//                        )
//                        // 评论数 - integer类型
//                        .properties("commentCount", p -> p
//                                .integer(i -> i)
//                        )
//                        // 状态 - integer类型
//                        .properties("status", p -> p
//                                .integer(i -> i)
//                        )
//                        // 创建时间 - date类型（支持ISO 8601格式）
//                        .properties("createTime", p -> p
//                                .date(d -> d
//                                        .format("yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
//                                )
//                        )
//                        // 更新时间 - date类型（支持ISO 8601格式）
//                        .properties("updateTime", p -> p
//                                .date(d -> d
//                                        .format("strict_date_optional_time||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis")
//                                )
//                        )
//                        // 是否推荐 - integer类型
//                        .properties("isRecommend", p -> p
//                                .integer(i -> i)
//                        )
//                        // 分类名称 - keyword类型，用于精确匹配和聚合
//                        .properties("categoryName", p -> p
//                                .keyword(k -> k)
//                        )
//                        // 作者名称 - keyword类型，用于精确匹配
//                        .properties("authorName", p -> p
//                                .keyword(k -> k)
//                        )
//                        // 标签列表 - keyword类型数组，用于精确匹配和聚合
//                        .properties("tags", p -> p
//                                .keyword(k -> k)
//                        )
//                )
//        );
//        log.info("索引：{}创建成功", INDEX_NAME);
//    }
//    /**
//     *删除索引
//     */
//    public void deleteIndex()throws IOException {
//        if(!indexExist()){
//            log.warn("索引{}不存在",INDEX_NAME);
//        }
//        DeleteIndexResponse delete = client.indices().delete(s -> s.index(INDEX_NAME));
//        log.info("删除结果：{}",delete.acknowledged());
//
//    }
//
//    /**
//     * 索引是否存在
//     */
//
//    public boolean indexExist() throws IOException {
//        boolean value = client.indices().exists(e -> e.index(INDEX_NAME)).value();
//        log.info("索引{}存在？{}",INDEX_NAME,value);
//        return value;
//    }
//    /**
//     * 添加单个文档
//     */
//    public void addDocument(ArticleDocument document) throws IOException {
//        IndexResponse index = client.index(i -> i
//                .index(INDEX_NAME)
//                .id(document.getId())
//                .document(document));
//        log.info("ES文章创建:{}",index.result());
//
//    }
//
//    /**
//     * 更新文档
//     */
//    public void updateDocument(String id, ArticleDocument document) throws IOException {
//        UpdateResponse<ArticleDocument> update = client.update(u -> u
//                .index(INDEX_NAME)
//                .id(id)
//                .doc(document), ArticleDocument.class
//        );
//        log.info("文档更新结果：{}",update.result());
//    }
//
//    /**
//     * 删除文档
//     */
//    public void deleteDocument(String id) throws IOException {
//        DeleteResponse res = client.delete(d -> d
//                .index(INDEX_NAME)
//                .id(id));
//        log.info("文档删除结果：{}",res.result());
//    }
//    /**
//     * 批量添加文档
//     */
//    public void bulkAddDocuments(List<ArticleDocument> articleDocuments) throws IOException {
//
//        if(Objects.isNull(articleDocuments) || articleDocuments.isEmpty()){
//            log.warn("文章为空");
//            return;
//        }
//        BulkRequest.Builder builder = new BulkRequest.Builder();
//        articleDocuments.forEach(articleDocument -> {
//
//            builder.operations(o->o
//                    .index(i->i
//                            .index(INDEX_NAME)
//                            .id(articleDocument.getId())
//                            .document(articleDocument)
//            ));
//        });
//
//        BulkResponse bulk = client.bulk(builder.build());
//        if(bulk.errors()){
//            bulk.items().forEach(i->{
//                if(i.error()!=null){
//                    log.error(i.error().reason());
//                }
//            });
//        }else{
//            log.info("批量插入ES成功");
//        }
//
//    }
//    /**
//     * 根据id获取文档
//     */
//    public ArticleDocument getDocumentById(String id) throws IOException {
//        GetResponse<ArticleDocument> response = client.get(i -> i
//                .index(INDEX_NAME)
//                .id(id),ArticleDocument.class);
//        if(response.found()){
//            ArticleDocument source = response.source();
//            log.info("ES查询{}成功：{}",id,source.getTitle());
//            return source;
//        }else{
//
//            log.warn("文章{}不存咋",id);
//            return null;
//        }
//    }
//    /**
//     * 全文搜索（支持分页、高亮）
//     */
//    public Page<ArticleDocument> searchDocuments(String keyword, int current, int size) throws IOException {
//        SearchResponse<ArticleDocument> response = client.search(s -> s
//                .index(INDEX_NAME)
//                .query(q -> q
//                        .multiMatch(m -> m
//                                .query(keyword)
//                                .fields("title^3", "content", "summary^2")  // 添加权重
//                        )
//                )
//                .from((int) ((current - 1) * size))
//                .size(size)
//                .highlight(h -> h
//                        // 全局高亮标签配置
//                        .preTags("<em>")
//                        .postTags("</em>")
//                        // 使用NamedValue方式配置高亮字段
//                        .fields(NamedValue.of("title",
//                                HighlightField.of(f -> f
//                                        .numberOfFragments(0)  // 返回整个字段
//                                )))
//                        .fields(NamedValue.of("content",
//                                HighlightField.of(f -> f
//                                        .fragmentSize(150)      // 片段大小
//                                        .numberOfFragments(3)   // 返回3个片段
//                                )))
//                        .fields(NamedValue.of("summary",
//                                HighlightField.of(f -> f
//                                        .numberOfFragments(0)   // 返回整个字段
//                                )))
//                ),
//                ArticleDocument.class
//        );
//
//        log.info("搜索关键词: {}, 命中数量: {}", keyword, response.hits().total().value());
//        Page<ArticleDocument> page = new Page(current, size);
//        page.setTotal(response.hits().total().value());
//
//
//        // 处理搜索结果和高亮
//        List<ArticleDocument> records = response.hits().hits().stream()
//                .map(hit -> {
//                    ArticleDocument doc = hit.source();
//
//                    // 处理高亮
//                    if (hit.highlight() != null) {
//                        Map<String, List<String>> highlights = hit.highlight();
//
//                        // 标题高亮（完整字段）
//                        if (highlights.containsKey("title") && !highlights.get("title").isEmpty()) {
//                            doc.setTitle(String.join("", highlights.get("title")));
//                        }
//
//                        // 内容高亮（多个片段用...连接）
//                        if (highlights.containsKey("content") && !highlights.get("content").isEmpty()) {
//                            doc.setContent(String.join("...", highlights.get("content")));
//                        }
//
//                        // 摘要高亮（完整字段）
//                        if (highlights.containsKey("summary") && !highlights.get("summary").isEmpty()) {
//                            doc.setSummary(String.join("", highlights.get("summary")));
//                        }
//
//                        log.debug("文章ID: {}, 高亮字段: {}", doc.getId(), highlights.keySet());
//                    }
//
//                    return doc;
//                })
//                .collect(Collectors.toList());
//
//        page.setRecords(records);
//        return page;
//    }
//    /**
//     * 高级搜索
//     */
//    public Page<ArticleDocument> advancedSearch(
//            String keyword,
//            String categoryName,
//            List<String> tags,
//            int current,
//            int size
//    ) throws IOException {
//        SearchResponse<ArticleDocument> response = client.search(s -> s
//                        .index(INDEX_NAME)
//                        .query(q -> q
//                                .bool(b -> {
//                                    // 1. 关键词搜索（可选）- 使用should，提高相关性
//                                    if (keyword != null && !keyword.isEmpty()) {
//                                        b.should(sh -> sh
//                                                .multiMatch(m -> m
//                                                        .query(keyword)
//                                                        //^3代表权重是3
//                                                        .fields("title^3", "content", "summary^2")
//                                                )
//                                        );
//                                    }
//
//                                    // 2. 分类过滤（可选）- 使用filter，精确匹配
//                                    if (categoryName != null && !categoryName.isEmpty()) {
//                                        b.filter(f -> f
//                                                .term(t -> t
//                                                        .field("categoryName")
//                                                        .value(categoryName)
//                                                )
//                                        );
//                                    }
//
//                                    // 3. 标签过滤（可选）- 使用filter，匹配任意一个标签
//                                    if (tags != null && !tags.isEmpty()) {
//                                        b.filter(f -> f
//                                                .terms(t -> t
//                                                        .field("tags")
//                                                        .terms(tv -> tv.value(tags.stream()
//                                                                .map(FieldValue::of)
//                                                                .collect(Collectors.toList())))
//                                                )
//                                        );
//                                    }
//
//                                    // 4. 只返回已发布的文章
//                                    b.filter(f -> f
//                                            .term(t -> t
//                                                    .field("status")
//                                                    .value(1)
//                                            )
//                                    );
//
//                                    return b;
//                                })
//                        )
//                        .from((current - 1) * size)
//                        .size(size)
//                        // 按相关性和时间排序
//                        .sort(so -> so
//                                .score(sc -> sc.order(SortOrder.Desc))
//                        )
//                        .sort(so -> so
//                                .field(f -> f
//                                        .field("createTime")
//                                        .order(SortOrder.Desc)
//                                )
//                        )
//                        .highlight(h -> h
//                                .preTags("<em>")
//                                .postTags("</em>")
//                                .fields(NamedValue.of("title", HighlightField.of(f -> f)))
//                                .fields(NamedValue.of("content", HighlightField.of(f -> f)))
//                                .fields(NamedValue.of("summary", HighlightField.of(f -> f)))
//                        ),
//                ArticleDocument.class
//        );
//
//        // 构建分页结果
//        Page<ArticleDocument> page = new Page<>(current, size);
//        page.setTotal(response.hits().total().value());
//
//        List<ArticleDocument> records = response.hits().hits().stream()
//                .map(hit -> {
//                    ArticleDocument doc = hit.source();
//                    // 处理高亮
//                    if (hit.highlight() != null) {
//                        if (hit.highlight().get("title") != null) {
//                            doc.setTitle(String.join("", hit.highlight().get("title")));
//                        }
//                        if (hit.highlight().get("content") != null) {
//                            doc.setContent(String.join("...", hit.highlight().get("content")));
//                        }
//                        if (hit.highlight().get("summary") != null) {
//                            doc.setSummary(String.join("", hit.highlight().get("summary")));
//                        }
//                    }
//                    return doc;
//                })
//                .collect(Collectors.toList());
//
//        page.setRecords(records);
//
//        log.info("高级搜索完成 - 关键词:{}, 分类:{}, 标签:{}, 结果数:{}",
//                keyword, categoryName, tags, page.getTotal());
//
//        return page;
//    }
//    /**
//     * 单篇文章异步同步到ES
//     */
//    @Async
//    public void syncToEsAsync(Article article) throws IOException {
//        ArticleDocument articleDocument = convertToDocument(article);
//        addDocument(articleDocument);
//        log.info("文章{}已异步同步到ES", article.getId());
//    }
//
//    /**
//     * 全量同步：从数据库同步所有已发布文章到ES
//     */
//    public void syncAllFromDatabase() throws IOException {
//        log.info("开始全量同步文章到ES...");
//
//        // 1. 查询所有已发布的文章
//        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(Article::getStatus, 1); // 只同步已发布的文章
//        List<Article> articles = articleMapper.selectList(wrapper);
//
//        if (articles == null || articles.isEmpty()) {
//            log.warn("没有需要同步的文章");
//            return;
//        }
//
//        // 2. 转换为ArticleDocument列表
//        List<ArticleDocument> documents = articles.stream()
//                .map(this::convertToDocument)
//                .collect(Collectors.toList());
//
//        // 3. 批量添加到ES
//        bulkAddDocuments(documents);
//
//        log.info("全量同步完成，共同步{}篇文章", documents.size());
//    }
//
//    /**
//     * 重建索引：删除旧索引，创建新索引，同步所有数据
//     */
//    public void rebuildIndex() throws IOException {
//        log.info("开始重建ES索引...");
//
//        // 1. 删除旧索引（如果存在）
//        if (indexExist()) {
//            deleteIndex();
//            log.info("旧索引已删除");
//        }
//
//        // 2. 创建新索引
//        createIndex();
//        log.info("新索引已创建");
//
//        // 3. 全量同步数据
//        syncAllFromDatabase();
//
//        log.info("索引重建完成");
//    }
//
//    //======================================工具方法=======================================
//    public ArticleDocument convertToDocument(Article article){
//        ArticleDocument articleDocument = new ArticleDocument();
//
//        // 1. 处理标签（防止空列表导致SQL错误）
//        List<ArticleTag> articleTags = articleTagMapper.selectList(
//                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId())
//        );
//        List<String> tagNames = List.of(); // 默认空列表
//        if (articleTags != null && !articleTags.isEmpty()) {
//            List<Long> tagIds = articleTags.stream()
//                    .map(ArticleTag::getTagId)
//                    .collect(Collectors.toList());
//
//            // 只有当tagIds不为空时才查询
//            if (!tagIds.isEmpty()) {
//                tagNames = tagMapper.selectBatchIds(tagIds).stream()
//                        .filter(Objects::nonNull)
//                        .map(Tag::getName)
//                        .collect(Collectors.toList());
//            }
//        }
//
//        // 2. 处理作者信息（修复逻辑错误）
//        Long userId = article.getUserId();
//        if (userId != null) {
//            User user = userMapper.selectById(userId);
//            if (user != null) {
//                String authorName = StringUtils.isNotBlank(user.getName())
//                        ? user.getName()
//                        : user.getUsername();
//                articleDocument.setAuthorName(authorName);
//                articleDocument.setUserId(userId);
//            }
//        }
//
//        // 3. 处理分类信息（修复逻辑错误）
//        Long categoryId = article.getCategoryId();
//        if (categoryId != null) {
//            Category category = categoryMapper.selectById(categoryId);
//            if (category != null) {
//                articleDocument.setCategoryName(category.getName());
//            }
//        }
//
//
//        // 4. 设置基本信息
//        articleDocument.setId(article.getId());
//        articleDocument.setTitle(article.getTitle());
//        articleDocument.setContent(article.getContent());
//        articleDocument.setSummary(article.getSummary());
//        articleDocument.setViewCount(article.getViewCount());
//        articleDocument.setLikeCount(article.getLikeCount());
//        articleDocument.setCommentCount(article.getCommentCount());
//        articleDocument.setStatus(article.getStatus());
//        articleDocument.setIsRecommend(article.getIsRecommend());
//        articleDocument.setCreateTime(article.getCreateTime());
//        articleDocument.setUpdateTime(article.getUpdateTime());
//        articleDocument.setTags(tagNames);
//
//        return articleDocument;
//
//
//
//    }
//
//
//
//
//
//
//
//}
