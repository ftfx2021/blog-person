package org.example.springboot.controller.es;

import co.elastic.clients.elasticsearch._types.SortMode;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationVariant;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
//import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.entity.es.JobTest;
import org.example.springboot.service.es.ESJobTestService;
import org.example.springboot.util.EsResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController()
@RequestMapping("ESTest")
public class TestController {
    @Autowired
    private ESJobTestService esJobTestService;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    private RangeQuery.Builder r;

    @PostMapping("/save")
    public Result<?> save(@RequestBody JobTest jobTest) {

        return Result.success(esJobTestService.save(jobTest));
    }
    @GetMapping("/id")
    public Result<?> getById(@RequestParam String id) {
        JobTest jobTest = elasticsearchOperations.get(id, JobTest.class);
        return Result.success(jobTest);

    }
    @DeleteMapping
    public Result<?> delete(@RequestBody(required = false) JobTest jobTest) {

//        String delete = elasticsearchOperations.delete("1001", JobTest.class);//返回id
        jobTest.setId("1001");

        String delete = elasticsearchOperations.delete(jobTest);

        return Result.success(delete);
    }
    @GetMapping
    public Result<?> getByXXX() {

        SearchHits<JobTest> allData = elasticsearchOperations.search(
            new CriteriaQuery(new Criteria()), 
            JobTest.class
        );
        log.info("索引中总数据量: {}", allData.getTotalHits());
        log.info("所有数据: {}", allData.getSearchHits().stream()
            .map(SearchHit::getContent)
            .toList());
        
        // 执行条件查询
        Criteria criteria = new Criteria("baseSalary").lessThan("8000").greaterThan("5000");
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        log.info("查询条件: type = 全职");
        
        SearchHits<JobTest> search = elasticsearchOperations.search(criteriaQuery, JobTest.class);
        log.info("查询结果数量: {}", search.getTotalHits());
        
        List<JobTest> jobList = search.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        log.info("jobList={}", jobList);
        return Result.success(jobList);

    }

    /**
     * 测试方法：插入测试数据
     */
    @PostMapping("/test-data")
    public Result<?> insertTestData() {
        JobTest jobTest = new JobTest();
        jobTest.setId("test-001");
        jobTest.setType("全职");
        jobTest.setTitle("Java开发工程师");
        jobTest.setBaseSalary(10000);
        jobTest.setMoreSalary(5000);
        
        String id = esJobTestService.save(jobTest);
        log.info("插入测试数据成功，ID: {}", id);
        
        return Result.success("测试数据插入成功，ID: " + id);
    }

    /**
     * 批量生成测试数据
     * @param count 生成数据的数量，默认20条
     */
    @PostMapping("/generate-data")
    public Result<?> generateTestData(@RequestParam(defaultValue = "100") int count) {
        log.info("========== 开始生成测试数据，数量: {} ==========", count);
        
        String[] types = {"全职", "兼职", "实习", "外包"};
        String[] positions = {
            "Java开发工程师", "前端开发工程师", "Python开发工程师", 
            "测试工程师", "产品经理", "UI设计师", 
            "运维工程师", "数据分析师", "算法工程师",
            "Go开发工程师", "Android开发", "iOS开发"
        };
        String[] descriptions = {
            "JAVA负责公司核心业务系统开发",
            "参与大型JAVA互联网项目研发",
            "独立完成JAVA模块设计与开发",
            "优化JAVA系统性能和用户体验",
            "参与JAVA技术方案评审和设计",
            "负责代码审查和JAVA技术分享"
        };
        
        List<String> savedIds = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        
        for (int i = 1; i <= count; i++) {
            JobTest jobTest = new JobTest();
            jobTest.setId("auto-" + System.currentTimeMillis() + "-" + i);
            
            // 随机分配职位类型
            jobTest.setType(types[new Random().nextInt(types.length)]);
            
            // 随机分配职位名称
            jobTest.setTitle(positions[new Random().nextInt(positions.length)]);
            
            // 随机分配职位描述
            jobTest.setDescription(descriptions[new Random().nextInt(descriptions.length)]);
            if(i % 2 == 0) {
                jobTest.setStatus(1);
            }
            else{
                jobTest.setStatus(0);
            }

            
            // 随机生成薪资（5000-20000之间）
            int baseSalary = 5000 + new Random().nextInt(15000);
            jobTest.setBaseSalary(baseSalary);
            jobTest.setMoreSalary(new Random().nextInt(5000));
            
            // 生成不同的创建时间（最近30天内随机）
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -new Random().nextInt(30));
            calendar.add(Calendar.HOUR_OF_DAY, -new Random().nextInt(24));
            jobTest.setCreateTime(calendar.getTime());
            
            // 保存数据
            String id = esJobTestService.save(jobTest);
            savedIds.add(id);
            
            // 每10条输出一次进度
            if (i % 10 == 0) {
                log.info("已生成 {}/{} 条数据", i, count);
            }
        }
        
        log.info("========== 测试数据生成完成 ==========");
        log.info("成功生成 {} 条数据", savedIds.size());
        
        return Result.success(new HashMap<String, Object>() {{
            put("message", "测试数据生成成功");
            put("totalCount", savedIds.size());
            put("generatedIds", savedIds);
        }});
    }

    /**
     * 清空所有测试数据
     */
    @DeleteMapping("/clear-all")
    public Result<?> clearAllData() {
        log.info("========== 开始清空所有测试数据 ==========");
        
        try {
            // 删除索引
            boolean deleted = elasticsearchOperations.indexOps(JobTest.class).delete();
            
            if (deleted) {
                // 重新创建索引
                boolean created = elasticsearchOperations.indexOps(JobTest.class).create();
                // 创建映射
                elasticsearchOperations.indexOps(JobTest.class).putMapping();
                
                log.info("索引删除并重建成功");
                return Result.success("所有测试数据已清空，索引已重建");
            } else {
                log.warn("索引删除失败，可能索引不存在");
                return Result.error("索引删除失败");
            }
        } catch (Exception e) {
            log.error("清空数据失败: {}", e.getMessage(), e);
            return Result.error("清空数据失败: " + e.getMessage());
        }
    }

    /**
     * 对比查询：查询所有数据 vs 条件查询
     * 演示两种查询方式的区别
     */
    @GetMapping("/compare-queries")
    public Result<?> compareQueries() {
        log.info("========== 开始对比查询 ==========");
        
        // ========== 查询1：查询所有数据（无条件） ==========
        log.info("【查询1】查询所有数据（无条件）");
        Criteria allCriteria = new Criteria(); // 空条件
        CriteriaQuery allQuery = new CriteriaQuery(allCriteria);
        
        SearchHits<JobTest> allData = elasticsearchOperations.search(allQuery, JobTest.class);
        List<JobTest> allList = allData.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        
        log.info("查询1结果 - 总数量: {}", allData.getTotalHits());
        log.info("查询1结果 - 数据详情: {}", allList);
        
        // ========== 查询2：条件查询（baseSalary 范围） ==========
        log.info("【查询2】条件查询: 5000 < baseSalary < 8000");
        Criteria rangeCriteria = new Criteria("baseSalary")
                .greaterThan(5000)
                .lessThan(8000);
        CriteriaQuery rangeQuery = new CriteriaQuery(rangeCriteria);
        
        SearchHits<JobTest> rangeData = elasticsearchOperations.search(rangeQuery, JobTest.class);
        List<JobTest> rangeList = rangeData.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        
        log.info("查询2结果 - 总数量: {}", rangeData.getTotalHits());
        log.info("查询2结果 - 数据详情: {}", rangeList);
        
        // ========== 对比结果 ==========
        log.info("========== 查询对比总结 ==========");
        log.info("查询1（所有数据）: {} 条", allData.getTotalHits());
        log.info("查询2（条件过滤）: {} 条", rangeData.getTotalHits());
        log.info("过滤掉的数据: {} 条", allData.getTotalHits() - rangeData.getTotalHits());
        
        // 构建返回结果
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("allDataCount", allData.getTotalHits());
            put("allDataList", allList);
            put("filteredDataCount", rangeData.getTotalHits());
            put("filteredDataList", rangeList);
            put("excludedCount", allData.getTotalHits() - rangeData.getTotalHits());
        }});
    }

    /**
     * 演示多种查询条件的对比
     * 包括：无条件、单条件、多条件AND、多条件OR、子查询
     */
    @GetMapping("/query-examples")
    public Result<?> queryExamples() {
        log.info("========== 查询示例演示 ==========");
        
        // 示例1：无条件查询（查询所有）
        log.info("\n【示例1】无条件查询 - 查询所有数据");
        SearchHits<JobTest> result1 = elasticsearchOperations.search(
            new CriteriaQuery(new Criteria()), 
            JobTest.class
        );
        log.info("结果数量: {}", result1.getTotalHits());
        
        // 示例2：单条件查询
        log.info("\n【示例2】单条件查询 - type = '全职'");
        Criteria criteria2 = new Criteria("type").is("全职");
        SearchHits<JobTest> result2 = elasticsearchOperations.search(
            new CriteriaQuery(criteria2), 
            JobTest.class
        );
        log.info("结果数量: {}", result2.getTotalHits());
        
        // 示例3：范围查询
        log.info("\n【示例3】范围查询 - 5000 < baseSalary < 8000");
        Criteria criteria3 = new Criteria("baseSalary")
            .greaterThan(5000)
            .lessThan(8000);
        SearchHits<JobTest> result3 = elasticsearchOperations.search(
            new CriteriaQuery(criteria3), 
            JobTest.class
        );
        log.info("结果数量: {}", result3.getTotalHits());
        
        // 示例4：多条件AND查询
        log.info("\n【示例4】多条件AND查询 - type = '全职' AND baseSalary > 6000");
        Criteria criteria4 = new Criteria("type").is("全职")
            .and("baseSalary").greaterThan(6000);
        SearchHits<JobTest> result4 = elasticsearchOperations.search(
            new CriteriaQuery(criteria4), 
            JobTest.class
        );
        log.info("结果数量: {}", result4.getTotalHits());
        
        // 示例5：多条件OR查询
        log.info("\n【示例5】多条件OR查询 - type = '全职' OR type = '兼职'");
        Criteria criteria5 = new Criteria("type").is("全职")
            .or("type").is("兼职");
        SearchHits<JobTest> result5 = elasticsearchOperations.search(
            new CriteriaQuery(criteria5), 
            JobTest.class
        );
        log.info("结果数量: {}", result5.getTotalHits());
        
        // 示例6：子查询（嵌套逻辑）
        log.info("\n【示例6】子查询 - baseSalary > 6000 AND (type = '全职' OR type = '兼职')");
        Criteria criteria6 = new Criteria("baseSalary").greaterThan(6000)
            .subCriteria(
                new Criteria("type").is("全职")
                    .or("type").is("兼职")
            );
        SearchHits<JobTest> result6 = elasticsearchOperations.search(
            new CriteriaQuery(criteria6), 
            JobTest.class
        );

        // 汇总结果
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("示例1_无条件查询", result1.getTotalHits());
            put("示例2_单条件查询", result2.getTotalHits());
            put("示例3_范围查询", result3.getTotalHits());
            put("示例4_多条件AND", result4.getTotalHits());
            put("示例5_多条件OR", result5.getTotalHits());
            put("示例6_子查询", result6.getTotalHits());
        }});
    }

    @GetMapping("string")
    public Result<?> string() {
        StringQuery stringQuery = new StringQuery("{\"match\":{\"type\":\"全职\"}}");
        SearchHits<JobTest> search = elasticsearchOperations.search(stringQuery, JobTest.class);
        List<JobTest> list = search.getSearchHits().stream().map(SearchHit::getContent).toList();
        log.info("result: {}", list);
        return Result.success(list);
    }

    @GetMapping("native")
    public Result<?> quaryNative(){

        NativeQuery build = NativeQuery.builder()
                .withQuery(q -> q
                .match(m -> m
                        .field("type")
                        .query("全职"))
        ).build();
        SearchHits<JobTest> search = elasticsearchOperations.search(build, JobTest.class);
        List<JobTest> list = search.getSearchHits().stream().map(SearchHit::getContent).toList();
        return Result.success(list);
    }

    /**
     * 分页查询示例1：使用 CriteriaQuery 进行分页
     * 
     * @param page 当前页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/page-criteria")
    public Result<?> pageByCriteria(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(required = false) Integer maxSalary,
            @RequestParam(required = false) String title
    ) {
        log.info("========== CriteriaQuery 分页查询 ==========");
        log.info("请求参数 - 页码: {}, 每页大小: {}", page, size);

        // 创建排序和分页

        Sort sort = Sort.by(Sort.Order.desc("allSalary"), Sort.Order.asc("moreSalary"));
        PageRequest pageRequest = PageRequest.of(page - 1, size, sort);

        // 直接链式调用
        //可以直接add，不用管前面的是否为空
        Criteria criteria = new Criteria();

        if (StringUtils.isNotBlank(type)) {
            criteria = criteria.and("type").is(type);
        }
        if (minSalary != null) {
            criteria = criteria.and("allSalary").greaterThan(minSalary);
        }
        if (maxSalary != null) {
            criteria = criteria.and("allSalary").lessThan(maxSalary);
        }
        if (StringUtils.isNotBlank(title)) {
            criteria = criteria.and("title").is(title);
        }

        // 构建查询并执行
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        criteriaQuery.setPageable(pageRequest);

        SearchHits<JobTest> search = elasticsearchOperations.search(criteriaQuery, JobTest.class);
        List<JobTest> list = search.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return Result.success(list);
    }

    /**
     * 分页查询示例2：使用 NativeQuery 进行分页 + 排序
     * 
     * @param page 当前页码（从0开始）
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/page-native")
    public Result<?> pageByNative(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(required = false) Integer maxSalary) {
        
        log.info("========== NativeQuery 分页查询 + 排序 ==========");


        Sort sort = Sort.by(Sort.Order.desc("allSalary"), Sort.Order.asc("moreSalary"));
        PageRequest pageRequest = PageRequest.of(page - 1, size, sort);


        RangeQuery ltMaxSalary = RangeQuery.of(r -> r.number(
                n -> n.field("allSalary")
                        .lt(maxSalary.doubleValue())
        ));
        RangeQuery gtMinSalary = RangeQuery.of(r -> r.number(
                n -> n.field("allSalary")
                        .gt(minSalary.doubleValue())
        ));
        // 构建 NativeQuery
        NativeQueryBuilder builder = NativeQuery.builder();
        NativeQuery query = builder.withQuery(q -> q.bool(b -> {
                    if (StringUtils.isNotBlank(type)) {
                        b.must(m -> m.term(v -> v.field("type").value(type)));
                    }
                    if (maxSalary != null) {
                        b.must(ltMaxSalary);
                    }
                    if (minSalary != null) {
                        b.must(gtMinSalary);
                    }
                    return b;
                }
        ))
                .withPageable(PageRequest.of(page, size))
                .build();
        SearchHits<JobTest> search = elasticsearchOperations.search(query, JobTest.class);
        List<JobTest> queryRes = search.getSearchHits().stream().map(SearchHit::getContent).toList();

        
        log.info("查询结果 - 总数: {}, 当前页数据量: {}", search.getTotalHits(), queryRes.size());
        
        // 构建分页返回结果
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("content", queryRes);
            put("totalElements", search.getTotalHits());
            put("totalPages", (search.getTotalHits() + size - 1) / size);
            put("currentPage", page);

            put("hasNext", (long) (page + 1) * size < search.getTotalHits());
            put("hasPrevious", page > 0);
        }});
    }

    /**
     * 分页查询示例3：复杂条件 + 分页 + 多字段排序
     * 
     * @param page 当前页码
     * @param size 每页大小
     * @param minSalary 最低薪资
     * @param maxSalary 最高薪资
     * @return 分页结果
     */
    @GetMapping("/page-complex")
    public Result<?> pageComplex(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(required = false) Integer maxSalary) {
        
        log.info("========== 复杂条件分页查询 ==========");
        log.info("请求参数 - 页码: {}, 每页大小: {}, 薪资范围: {} - {}", 
                page, size, minSalary, maxSalary);
        
        // 创建多字段排序：先按 baseSalary 降序，再按 createTime 降序
        Sort sort = Sort.by(
                Sort.Order.desc("baseSalary"),
                Sort.Order.desc("createTime")
        );
        
        // 创建分页对象
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 构建复杂查询条件
        Criteria criteria = new Criteria("type").is("全职");
        
        // 动态添加薪资范围条件
        if (minSalary != null) {
            criteria = criteria.and("baseSalary").greaterThanEqual(minSalary);
        }
        if (maxSalary != null) {
            criteria = criteria.and("baseSalary").lessThanEqual(maxSalary);
        }
        
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(pageable);
        
        // 执行查询
        SearchHits<JobTest> searchHits = elasticsearchOperations.search(query, JobTest.class);
        List<JobTest> list = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();
        
        log.info("查询结果 - 总数: {}, 当前页数据量: {}", searchHits.getTotalHits(), list.size());
        
        // 构建分页返回结果
        return Result.success(new java.util.HashMap<String, Object>() {{
            put("content", list);
            put("totalElements", searchHits.getTotalHits());
            put("totalPages", (searchHits.getTotalHits() + size - 1) / size);
            put("currentPage", page);
            put("pageSize", size);
            put("queryConditions", new java.util.HashMap<String, Object>() {{
                put("type", "全职");
                put("minSalary", minSalary);
                put("maxSalary", maxSalary);
            }});
            put("hasNext", (page + 1) * size < searchHits.getTotalHits());
            put("hasPrevious", page > 0);
        }});
    }


    @GetMapping("/pageWithHeighLight")
    public Result<?> pageWithHeighLight(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(required = false) Integer maxSalary
    ){

        Sort by = Sort.by(Sort.Order.desc("baseSalary"), Sort.Order.desc("createTime"));
        Pageable pageable = PageRequest.of(page, size, by);

        //===================================全局配置高亮参数===================================
        HighlightParameters highlightParameters = HighlightParameters
                .builder()
                .withPreTags("<em>")
                .withPostTags("</em>").build();
        List<HighlightField> highlightFields = new ArrayList<>(List.of(
                new HighlightField("title"),
                new HighlightField("description")));
        //===================================单独配置某个高亮参数===================================
        HighlightFieldParameters typeFieldParameter = HighlightFieldParameters.builder().withPreTags("<em style=\"background:red\">").withPostTags("</em>").build();
        HighlightField typeHighlight = new HighlightField("type", typeFieldParameter);
        highlightFields.add(typeHighlight);
        Highlight highlight = new Highlight(highlightParameters,highlightFields);
        HighlightQuery highlightQuery = new HighlightQuery(highlight,JobTest.class);

        //===================================限制返回结果数量===================================
        //一条结果+附加5条（附加的用other_type作为key）
        //  "inner_hits": {
        //        "other_articles": {
        //          "hits": []  // 该作者只有1篇文章
        //        }
        //}
        FieldCollapse fieldCollapse = FieldCollapse.of(b -> b
                .field("type")  //默认只返回一条结果
                .innerHits(ih -> ih   //结果附加信息
                        .name("other_type")  //附加信息的key，用于返回结果展示
                        .size(5)  //附加信息返回5条
//                        .highlight()  //  注意需要为 innerHits 单独配置高亮
                        .sort(sb->sb.field(f->f.field("createTime").order(SortOrder.Desc))) //注意此处的字段名要和配置中的一致（如果没有配置，则默认是驼峰命名）

        ));//用于返回结果展示






        //===================================过滤===================================
        Query queryFilter = Query.of(q -> q
                .term(t -> t.field("status").value(1)));
        //===================================配置搜索条件===================================
        NativeQueryBuilder nativeQueryBuilder = new NativeQueryBuilder();
        MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(b -> b
                .fields("title^3", "description^2", "type^1").query(keyword));

        NativeQuery nativeQuery = nativeQueryBuilder
                .withQuery(q -> q
                        .bool(b -> b.should(multiMatchQuery)))
                .withHighlightQuery(highlightQuery)
                .withFieldCollapse(fieldCollapse)
         //       .withAggregation()//结果聚合，类似Mysql的GroupBy
                .withFilter(queryFilter)
                .build();//过滤

        ;

        SearchHits<JobTest> searchHits = elasticsearchOperations.search(nativeQuery, JobTest.class);
        // 1. 获取列表（自动包含高亮）
        List<SearchHit<JobTest>> searchHits1 = searchHits.getSearchHits();

        List<JobTest> list = searchHits.getSearchHits()
                .stream()
                .map(hit -> {
                    JobTest job = hit.getContent();
                    // 处理高亮（如果有的话替换原字段）
                    List<String> titleHL = hit.getHighlightField("title");
                    if (!titleHL.isEmpty()) {
                        job.setTitle(titleHL.get(0));
                    }
                    return job;
                })
                .toList();


        Map<String, Object> pageResultWithHighlight = EsResultUtil.buildPageResultWithHighlight(searchHits);
        Map<String, Object> resultWithInnerHitsAndHighlight = EsResultUtil.buildPageResultWithHighlightAndInnerHits(searchHits,"other_type","title","description","type");
        // 3. 返回
        return Result.success(Map.of(
                "pageResultWithHighlight",pageResultWithHighlight, //原生结果
                "total", searchHits.getTotalHits(),
                "resultWithInnerHitsAndHighlight", resultWithInnerHitsAndHighlight
        ));


    }

    /**
     * 分页查询示例4：全文搜索 + 分页演示
     * 展示如何使用关键词进行全文搜索并获取相关性评分
     */
    @GetMapping("/page-demo")
    public Result<?> keywordByCriteria(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String keyword) {
        
        log.info("========== 全文搜索 + 分页功能演示 ==========");
        log.info("搜索关键词: {}, 页码: {}, 每页大小: {}", keyword, page, size);
        log.info("搜索字段: title, description, type");
        
        // 决定排序方式
        Pageable pageable;
        Sort sort = Sort.by(Sort.Order.desc("baseSalary"), Sort.Order.desc("createTime"));
        pageable = PageRequest.of(page - 1, size);

        
        // 构建查询条件
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(keyword)) {
            // 全文搜索：在 title、description、type 三个字段中搜索关键词
            // 使用 OR 关系，只要任一字段匹配即可
            criteria = new Criteria()
                    .or("title").contains(keyword) //
                    .or("description").matches(keyword)//不分词
                    .or("type").is(keyword); //精确查询
        }
        
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(pageable);
        query.setSort(sort);
        
        // 执行查询
        SearchHits<JobTest> searchHits = elasticsearchOperations.search(query, JobTest.class);

        long totalElements = searchHits.getTotalHits();
        int totalPages = (int) ((totalElements + size - 1) / size);

        List<JobTest> list = searchHits.getSearchHits().stream().map(SearchHit::getContent).toList();
        PageImpl<JobTest> pageRes = new PageImpl<JobTest>(list, pageable, totalElements);

        return Result.success(pageRes);
    }
    // 先查询看有没有数据
    @GetMapping("/test-data")
    public Result<?> testData() {
        NativeQuery query = new NativeQueryBuilder()
                .withQuery(Query.of(q -> q.matchAll(b->b)))
                .withMaxResults(10)
                .build();

        SearchHits<JobTest> hits = elasticsearchOperations.search(query, JobTest.class);

        log.info("总数据量: {}", hits.getTotalHits());

        // 检查 type 字段的值
        hits.forEach(hit -> {
            JobTest job = hit.getContent();
            log.info("ID: {}, type: {}", job.getId(), job.getType());
        });

        return Result.success(hits.getSearchHits());
    }

}
