<template>
  <div class="search-result-container">
    <!-- 搜索框 -->
    <div class="search-header">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文章..."
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="handleSearch" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 搜索结果统计 -->
    <div class="search-stats" v-if="searchPerformed">
      <span v-if="total > 0">
        找到 <strong>{{ total }}</strong> 篇相关文章
        <span v-if="keyword" class="keyword-highlight">"{{ keyword }}"</span>
      </span>
      <span v-else class="no-result">
        未找到相关文章
      </span>
    </div>

    <!-- 加载中 -->
    <div v-loading="loading" class="loading-container" v-if="loading">
      <el-empty description="搜索中..." />
    </div>

    <!-- 搜索结果列表 -->
    <div v-else-if="articleList.length > 0" class="article-list">
      <div
        v-for="article in articleList"
        :key="article.id"
        class="article-item"
        @click="goToArticle(article.id)"
      >
        <!-- 文章标题（高亮） -->
        <h3 class="article-title" v-html="article.title"></h3>

        <!-- 文章摘要（高亮） -->
        <div class="article-summary" v-html="article.summary || article.content"></div>

        <!-- 文章元信息 -->
        <div class="article-meta">
          <span v-if="article.authorName" class="meta-item">
            <el-icon><User /></el-icon>
            {{ article.authorName }}
          </span>
          <span v-if="article.categoryName" class="meta-item">
            <el-icon><Folder /></el-icon>
            {{ article.categoryName }}
          </span>
          <span v-if="article.createTime" class="meta-item">
            <el-icon><Calendar /></el-icon>
            {{ formatDate(article.createTime) }}
          </span>
          <span class="meta-item">
            <el-icon><View /></el-icon>
            {{ article.viewCount || 0 }}
          </span>
          <span class="meta-item">
            <el-icon><ChatDotRound /></el-icon>
            {{ article.commentCount || 0 }}
          </span>
        </div>

        <!-- 标签 -->
        <div v-if="article.tags && article.tags.length > 0" class="article-tags">
          <el-tag
            v-for="tag in article.tags"
            :key="tag"
            size="small"
            type="info"
            class="tag-item"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="searchPerformed && !loading" class="empty-state">
      <el-empty description="暂无搜索结果">
        <el-button type="primary" @click="clearSearch">清空搜索</el-button>
      </el-empty>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Search, User, Folder, Calendar, View, ChatDotRound } from '@element-plus/icons-vue';
import { searchArticles } from '@/api/SearchApi';

const route = useRoute();
const router = useRouter();

// 搜索状态
const searchKeyword = ref('');
const keyword = ref('');
const loading = ref(false);
const searchPerformed = ref(false);

// 文章列表
const articleList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

// 初始化
onMounted(() => {
  // 从URL参数获取搜索关键词
  const urlKeyword = route.query.keyword;
  if (urlKeyword) {
    searchKeyword.value = urlKeyword;
    keyword.value = urlKeyword;
    performSearch();
  }
});

// 执行搜索
const performSearch = () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词');
    return;
  }

  keyword.value = searchKeyword.value;
  loading.value = true;
  searchPerformed.value = true;

  searchArticles(
    {
      keyword: keyword.value,
      current: currentPage.value,
      size: pageSize.value
    },
    {
      onSuccess: (res) => {
        articleList.value = res.records || [];
        total.value = res.total || 0;
        loading.value = false;

        // 调试：查看返回的数据
        console.log('搜索结果:', res.records);
        if (res.records && res.records.length > 0) {
          console.log('第一条标题:', res.records[0].title);
          console.log('第一条内容:', res.records[0].content);
          console.log('第一条摘要:', res.records[0].summary);
        }

        // 更新URL参数
        router.replace({
          query: {
            keyword: keyword.value,
            page: currentPage.value
          }
        });
      },
      onError: (err) => {
        loading.value = false;
        ElMessage.error('搜索失败: ' + (err.message || '未知错误'));
      }
    }
  );
};

// 搜索按钮点击
const handleSearch = () => {
  currentPage.value = 1; // 重置到第一页
  performSearch();
};

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = '';
  keyword.value = '';
  articleList.value = [];
  total.value = 0;
  searchPerformed.value = false;
  router.replace({ query: {} });
};

// 分页变化
const handlePageChange = (page) => {
  currentPage.value = page;
  performSearch();
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const handleSizeChange = (size) => {
  pageSize.value = size;
  currentPage.value = 1;
  performSearch();
};

// 跳转到文章详情
const goToArticle = (articleId) => {
  router.push(`/article/${articleId}`);
};

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
};
</script>

<style scoped>
.search-result-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 搜索框 */
.search-header {
  margin-bottom: 30px;
}

.search-input {
  max-width: 800px;
}

/* 搜索统计 */
.search-stats {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 14px;
  color: #606266;
}

.keyword-highlight {
  color: #409eff;
  font-weight: bold;
}

.no-result {
  color: #909399;
}

/* 加载状态 */
.loading-container {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 文章列表 */
.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.article-item {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s;
}

.article-item:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

/* 文章标题 */
.article-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
}

/* 高亮样式 - 使用更高的优先级 */
.article-title :deep(em),
.article-title em {
  color: #ff6b6b !important;
  font-style: normal !important;
  font-weight: bold !important;
  background: #ffe0e0 !important;
  padding: 2px 6px !important;
  border-radius: 4px !important;
  box-shadow: 0 1px 3px rgba(255, 107, 107, 0.2) !important;
}

/* 文章摘要 */
.article-summary {
  margin-bottom: 16px;
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  max-height: 5.4em;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

/* 摘要高亮样式 */
.article-summary :deep(em),
.article-summary em {
  color: #ff6b6b !important;
  font-style: normal !important;
  font-weight: bold !important;
  background: #ffe0e0 !important;
  padding: 2px 6px !important;
  border-radius: 4px !important;
  box-shadow: 0 1px 3px rgba(255, 107, 107, 0.2) !important;
}

/* 文章元信息 */
.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 标签 */
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  cursor: pointer;
}

/* 空状态 */
.empty-state {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 分页 */
.pagination-container {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-result-container {
    padding: 15px;
  }

  .article-item {
    padding: 16px;
  }

  .article-title {
    font-size: 18px;
  }

  .article-meta {
    font-size: 12px;
    gap: 12px;
  }
}
</style>
