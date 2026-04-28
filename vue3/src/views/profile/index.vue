<template>
  <div class="profile-layout">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h2>个人中心</h2>
        </div>
      </template>
      
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 基本信息 Tab -->
        <el-tab-pane label="个人资料" name="basic">
          <div class="profile-info">
            <div class="avatar-container">
              <el-avatar :size="100" :src="avatarUrl" @error="() => false" />
              <el-upload
                class="avatar-uploader"
                action="#"
                :auto-upload="true"
                :show-file-list="false"
                :http-request="customUploadAvatar"
                :before-upload="beforeAvatarUpload"
              >
                <el-button size="small" type="primary">更换头像</el-button>
              </el-upload>
            </div>

            <div class="info-form">
              <el-form
                ref="userFormRef"
                :model="userForm"
                :rules="rules"
                label-width="100px"
                status-icon
              >
                <el-form-item label="用户名" prop="username">
                  <el-input v-model="userForm.username" disabled />
                </el-form-item>

                <el-form-item label="姓名" prop="name">
                  <el-input v-model="userForm.name" />
                </el-form-item>

                <el-form-item label="性别" prop="sex">
                  <el-radio-group v-model="userForm.sex">
                    <el-radio label="男">男</el-radio>
                    <el-radio label="女">女</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="电子邮箱" prop="email">
                  <el-input v-model="userForm.email" />
                </el-form-item>

                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="userForm.phone" />
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="submitUserInfo">保存修改</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <!-- 修改密码 Tab -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="120px"
            style="max-width: 500px; margin: 0 auto"
            status-icon
          >
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                show-password
                placeholder="请输入旧密码"
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                show-password
                placeholder="请输入新密码"
              />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                show-password
                placeholder="请再次输入新密码"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="submitPassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 我的点赞 Tab -->
        <el-tab-pane label="我的点赞" name="likes">
          <div class="my-likes">
            <el-table
              v-loading="likesLoading"
              :data="likedArticles"
              style="width: 100%"
              empty-text="暂无点赞内容"
            >
              <el-table-column prop="title" label="文章标题">
                <template #default="{ row }">
                  <router-link :to="`/article/${row.id}`">{{ row.title }}</router-link>
                </template>
              </el-table-column>
              
              <el-table-column prop="authorName" label="作者"></el-table-column>
              
              <el-table-column prop="createTime" label="点赞时间">
                <template #default="{ row }">
                  {{ formatDate(row.createTime) }}
                </template>
              </el-table-column>
              
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button 
                    size="small" 
                    type="danger" 
                    @click="cancelLike(row.id)"
                    :loading="cancelingLike === row.id"
                  >
                    取消点赞
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="likesCurrentPage"
                v-model:page-size="likesPageSize"
                :page-sizes="[10, 20, 30, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="likesTotal"
                @size-change="fetchLikedArticles"
                @current-change="fetchLikedArticles"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 我的收藏 Tab -->
        <el-tab-pane label="我的收藏" name="collections">
          <div class="my-collections">
            <div v-loading="collectionsLoading">
              <el-empty v-if="collections.length === 0" description="暂无收藏内容" />
              
              <div v-else class="collection-list">
                <el-card v-for="item in collections" :key="item.id" class="collection-item">
                  <div class="article-info">
                    <div class="article-cover" v-if="item.coverImage">
                      <el-image :src="item.coverImage" fit="cover" />
                    </div>
                    <div class="article-content">
                      <h3 class="article-title">
                        <router-link :to="`/article/${item.id}`">{{ item.title }}</router-link>
                      </h3>
                      <div class="article-summary" v-if="item.summary">{{ item.summary }}</div>
                      <div class="article-meta">
                        <span><el-icon><User /></el-icon> {{ item.authorName }}</span>
                        <span><el-icon><Calendar /></el-icon> {{ formatDate(item.createTime) }}</span>
                        <span><el-icon><View /></el-icon> {{ item.viewCount }} 阅读</span>
                        <span><el-icon><Star /></el-icon> {{ item.likeCount }} 点赞</span>
                        <span><el-icon><ChatDotRound /></el-icon> {{ item.commentCount }} 评论</span>
                      </div>
                    </div>
                  </div>
                  <div class="article-actions">
                    <span class="collect-time">收藏于：{{ formatDate(item.collectTime) }}</span>
                    <el-button 
                      size="small" 
                      type="danger" 
                      @click="cancelCollect(item.id)"
                      :loading="cancelingCollect === item.id"
                    >
                      取消收藏
                    </el-button>
                  </div>
                </el-card>
              </div>
              
              <!-- 分页 -->
              <div class="pagination-container" v-if="collectionsTotal > 0">
                <el-pagination
                  v-model:current-page="collectionsCurrentPage"
                  v-model:page-size="collectionsPageSize"
                  :page-sizes="[10, 20, 30, 50]"
                  layout="total, sizes, prev, pager, next, jumper"
                  :total="collectionsTotal"
                  @size-change="fetchCollections"
                  @current-change="fetchCollections"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 我的评论 Tab -->
        <el-tab-pane label="我的评论" name="comments">
          <div class="my-comments">
            <el-table
              v-loading="commentsLoading"
              :data="comments"
              style="width: 100%"
              empty-text="暂无评论内容"
            >
              <el-table-column prop="content" label="评论内容" min-width="300" show-overflow-tooltip>
                <template #default="{ row }">
                  <div class="comment-content">{{ row.content }}</div>
                </template>
              </el-table-column>
              
              <el-table-column prop="articleTitle" label="文章标题" min-width="200">
                <template #default="{ row }">
                  <router-link :to="`/article/${row.articleId}`">{{ row.articleTitle }}</router-link>
                </template>
              </el-table-column>
              
              <el-table-column prop="createTime" label="评论时间" width="180">
                <template #default="{ row }">
                  {{ formatDate(row.createTime) }}
                </template>
              </el-table-column>
              
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    size="small" 
                    type="danger" 
                    @click="deleteComment(row.id)"
                    :loading="deletingComment === row.id"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="commentsCurrentPage"
                v-model:page-size="commentsPageSize"
                :page-sizes="[10, 20, 30, 50]"
                layout="total, sizes, prev, pager, next, jumper"
                :total="commentsTotal"
                @size-change="fetchComments"
                @current-change="fetchComments"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from "vue";
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/store/user";
import { getCurrentUser, updateUser, updatePassword, getUserComments, deleteUserComment } from "@/api/user";
import { getUserCollectedArticlesPage, toggleArticleCollect, getUserLikedArticlesPage, toggleArticleLike } from "@/api/ArticleApi";
import { uploadToTemp } from '@/api/fileManagement';
import { User, Calendar, View, Star, ChatDotRound } from '@element-plus/icons-vue';
import DateUtils from '@/utils/dateUtils';
import { getFileUrl } from '@/utils/fileUtils';

const route = useRoute();
const userStore = useUserStore();
const activeTab = ref("basic");
const tempFileId = ref(null); // 存储临时文件ID

// 使用DateUtils格式化日期
const formatDate = (date) => {
  return DateUtils.formatDateTime(date);
}

// ===== 个人资料 =====
// 表单引用
const userFormRef = ref(null);
const passwordFormRef = ref(null);

// 用户表单数据
const userForm = reactive({
  id: "",
  username: "",
  name: "",
  email: "",
  phone: "",
  sex: "",
  avatar: "",
});

// 头像地址（处理完整URL）
const avatarUrl = computed(() => {
  return getFileUrl(userForm.avatar) || '';
});

// 密码表单数据
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
});

// 表单校验规则
const rules = {
  name: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  email: [
    { required: true, message: "请输入邮箱地址", trigger: "blur" },
    {
      type: "email",
      message: "请输入正确的邮箱地址",
      trigger: ["blur", "change"],
    },
  ],
  phone: [
    { required: false, trigger: "blur" },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: "请输入正确的手机号码",
      trigger: ["blur", "change"],
    },
  ],
};

// 密码表单校验规则
const passwordRules = {
  oldPassword: [
    { required: true, message: "请输入旧密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
  newPassword: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码长度不能小于6个字符", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请再次输入新密码", trigger: "blur" },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error("两次输入的密码不一致"));
        } else {
          callback();
        }
      },
      trigger: ["blur", "change"],
    },
  ],
};

// 获取用户信息
const getUserInfo = () => {
  // 如果用户已登录，从后端重新获取最新的用户信息
  if (userStore.isLoggedIn) {
    getCurrentUser({
      showDefaultMsg: false,
      onSuccess: (response) => {
        // 更新store中的用户信息
        userStore.updateUserInfo(response);

        // 直接更新表单数据
        userForm.id = response.id || "";
        userForm.username = response.username || "";
        userForm.name = response.name || "";
        userForm.email = response.email || "";
        userForm.phone = response.phone || "";
        userForm.sex = response.sex || "男";
        userForm.avatar = response.avatar || "";

        console.log("用户信息加载成功:", userForm);
      },
      onError: (error) => {
        console.error("获取用户信息失败", error);
        ElMessage.error("获取用户信息失败");
      }
    });
  }
};

// 上传头像前的校验
const beforeAvatarUpload = (file) => {
  const isJPG = file.type === "image/jpeg";
  const isPNG = file.type === "image/png";
  const isLt2M = file.size / 1024 / 1024 < 2;

  if (!isJPG && !isPNG) {
    ElMessage.error("头像只能是 JPG 或 PNG 格式!");
    return false;
  }
  if (!isLt2M) {
    ElMessage.error("头像大小不能超过 2MB!");
    return false;
  }
  return true;
};

// 自定义头像上传方法
const customUploadAvatar = (options) => {
  const { file } = options;

  // 新系统：上传到临时目录
  uploadToTemp(file, {
    successMsg: '头像上传成功',
    onSuccess: (tempUrl) => {
      userForm.avatar = tempUrl;
      console.log('头像已上传到临时目录:', tempUrl);
      options.onSuccess(tempUrl);
    },
    onError: (error) => {
      console.error('头像上传失败:', error);
      options.onError(error);
    }
  });
};

// 确认头像文件（策略A第二阶段）
const confirmAvatarFile = () => {
  return new Promise((resolve, reject) => {
    confirmTempFile(tempFileId.value, {
      businessType: FILE_BUSINESS_TYPES.USER_AVATAR,
      businessId: userForm.id.toString(),
      businessField: 'avatar',
      isTemp: false
    }, {
      onSuccess: (fileInfo) => {
        // 更新用户头像路径
        updateUser(userForm.id, { avatar: fileInfo.filePath }, {
          showDefaultMsg: false,
          onSuccess: () => {
            // 更新本地用户信息
            userStore.updateUserInfo({
              ...userStore.userInfo,
              name: userForm.name,
              email: userForm.email,
              phone: userForm.phone,
              sex: userForm.sex,
              avatar: fileInfo.filePath
            });
            
            userForm.avatar = fileInfo.filePath;
            tempFileId.value = null; // 清除临时文件ID
            resolve();
          },
          onError: (error) => {
            console.error('头像路径更新失败:', error);
            reject(error);
          }
        });
      },
      onError: (error) => {
        console.error('头像文件确认失败:', error);
        reject(error);
      }
    });
  });
};

// 提交用户信息更新（策略A：先更新用户信息，再确认头像文件）
const submitUserInfo = () => {
  if (!userFormRef.value) return;

  userFormRef.value.validate((valid) => {
    if (valid) {
      // 首先更新基本用户信息
      updateUser(userForm.id, {
        name: userForm.name,
        email: userForm.email,
        phone: userForm.phone,
        sex: userForm.sex,
      }, {
        showDefaultMsg: false,
        onSuccess: (data) => {
          // 如果有临时文件需要确认
          if (tempFileId.value) {
            confirmAvatarFile().then(() => {
              ElMessage.success("个人信息更新成功!");
            }).catch((error) => {
              console.error("头像确认失败:", error);
              ElMessage.error("头像更新失败，但基本信息已保存");
            });
          } else {
            // 没有头像更新，只更新基本信息
            userStore.updateUserInfo({
              ...userStore.userInfo,
              name: userForm.name,
              email: userForm.email,
              phone: userForm.phone,
              sex: userForm.sex,
            });
            ElMessage.success("个人信息更新成功!");
          }
        },
        onError: (error) => {
          console.error("用户信息更新失败", error);
          ElMessage.error("用户信息更新失败");
        }
      });
    }
  });
};

// 提交密码修改
const submitPassword = () => {
  if (!passwordFormRef.value) return;

  passwordFormRef.value.validate((valid) => {
    if (valid) {
      updatePassword(userForm.id, {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      }, {
        successMsg: "密码修改成功",
        onSuccess: (data) => {
          // 清空表单
          passwordForm.oldPassword = "";
          passwordForm.newPassword = "";
          passwordForm.confirmPassword = "";

          // 提示用户重新登录
          ElMessageBox.confirm("密码已修改，需要重新登录", "提示", {
            confirmButtonText: "重新登录",
            cancelButtonText: "取消",
            type: "warning",
          }).then(() => {
            // 清除用户信息并跳转到登录页
            userStore.clearUserInfo();
            window.location.href = "/login";
          });
        },
        onError: (error) => {
          console.error("密码信息保存失败", error);
          ElMessage.error("密码信息保存失败");
        }
      });
    }
  });
};

// ===== 我的点赞 =====
const likedArticles = ref([]);
const likesLoading = ref(false);
const cancelingLike = ref(null);
const likesCurrentPage = ref(1);
const likesPageSize = ref(10);
const likesTotal = ref(0);

// 获取用户点赞的文章列表
const fetchLikedArticles = () => {
  likesLoading.value = true;
  getUserLikedArticlesPage({
    currentPage: likesCurrentPage.value,
    size: likesPageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      likedArticles.value = data && data.records ? data.records : [];
      likesTotal.value = data && data.total ? data.total : 0;
      likesLoading.value = false;
    },
    onError: (error) => {
      console.error('获取点赞文章列表失败:', error);
      likedArticles.value = [];
      likesTotal.value = 0;
      likesLoading.value = false;
    }
  });
};

// 取消点赞
const cancelLike = (articleId) => {
  ElMessageBox.confirm('确定要取消点赞该文章吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cancelingLike.value = articleId;
    toggleArticleLike(articleId, {
      successMsg: '已取消点赞',
      onSuccess: () => {
        // 从列表中移除该文章
        likedArticles.value = likedArticles.value.filter(item => item.id !== articleId);
        // 如果当前页没有数据且不是第一页，则跳转到上一页
        if (likedArticles.value.length === 0 && likesCurrentPage.value > 1) {
          likesCurrentPage.value--;
          fetchLikedArticles();
        } else {
          // 更新总数
          likesTotal.value = Math.max(0, likesTotal.value - 1);
        }
        cancelingLike.value = null;
      },
      onError: (error) => {
        console.error('取消点赞失败:', error);
        cancelingLike.value = null;
      }
    });
  }).catch(() => {
    // 用户取消操作
  });
};

// ===== 我的收藏 =====
const collections = ref([]);
const collectionsLoading = ref(false);
const cancelingCollect = ref(null);
const collectionsCurrentPage = ref(1);
const collectionsPageSize = ref(10);
const collectionsTotal = ref(0);

// 获取用户收藏列表
const fetchCollections = () => {
  collectionsLoading.value = true;
  getUserCollectedArticlesPage({
    currentPage: collectionsCurrentPage.value,
    size: collectionsPageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      collections.value = data && data.records ? data.records : [];
      collectionsTotal.value = data && data.total ? data.total : 0;
      collectionsLoading.value = false;
    },
    onError: (error) => {
      console.error('获取收藏列表失败:', error);
      collections.value = [];
      collectionsTotal.value = 0;
      collectionsLoading.value = false;
    }
  });
};

// 取消收藏
const cancelCollect = (articleId) => {
  ElMessageBox.confirm('确定要取消收藏该文章吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    cancelingCollect.value = articleId;
    toggleArticleCollect(articleId, {
      successMsg: '已取消收藏',
      onSuccess: () => {
        // 从列表中移除该文章
        collections.value = collections.value.filter(item => item.id !== articleId);
        // 如果当前页没有数据且不是第一页，则跳转到上一页
        if (collections.value.length === 0 && collectionsCurrentPage.value > 1) {
          collectionsCurrentPage.value--;
          fetchCollections();
        } else {
          // 更新总数
          collectionsTotal.value = Math.max(0, collectionsTotal.value - 1);
        }
        cancelingCollect.value = null;
      },
      onError: (error) => {
        console.error('取消收藏失败:', error);
        cancelingCollect.value = null;
      }
    });
  }).catch(() => {
    // 用户取消操作
  });
};



// ===== 我的评论 =====
const comments = ref([]);
const commentsLoading = ref(false);
const deletingComment = ref(null);
const commentsCurrentPage = ref(1);
const commentsPageSize = ref(10);
const commentsTotal = ref(0);

// 获取用户评论列表
const fetchComments = () => {
  commentsLoading.value = true;
  getUserComments({
    currentPage: commentsCurrentPage.value,
    size: commentsPageSize.value
  }, {
    showDefaultMsg: false,
    onSuccess: (data) => {
      comments.value = data && data.records ? data.records : [];
      commentsTotal.value = data && data.total ? data.total : 0;
      commentsLoading.value = false;
    },
    onError: (error) => {
      console.error('获取评论列表失败:', error);
      comments.value = [];
      commentsTotal.value = 0;
      commentsLoading.value = false;
    }
  });
};

// 删除评论
const deleteComment = (commentId) => {
  ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    deletingComment.value = commentId;
    deleteUserComment(commentId, {
      successMsg: '评论删除成功',
      onSuccess: () => {
        // 从列表中移除该评论
        comments.value = comments.value.filter(item => item.id !== commentId);
        // 如果当前页没有数据且不是第一页，则跳转到上一页
        if (comments.value.length === 0 && commentsCurrentPage.value > 1) {
          commentsCurrentPage.value--;
          fetchComments();
        } else {
          // 更新总数
          commentsTotal.value = Math.max(0, commentsTotal.value - 1);
        }
        deletingComment.value = null;
      },
      onError: (error) => {
        console.error('删除评论失败:', error);
        deletingComment.value = null;
      }
    });
  }).catch(() => {
    // 用户取消操作
  });
};

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 0: return 'warning';  // 待审核
    case 1: return 'success';  // 已通过
    case 2: return 'danger';   // 已拒绝
    default: return 'info';
  }
};

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 0: return '待审核';
    case 1: return '已通过';
    case 2: return '已拒绝';
    default: return '未知';
  }
};

// 监听标签页变化，加载对应数据
watch(activeTab, (newTab) => {
  switch(newTab) {
    case 'likes':
      if (likedArticles.value.length === 0) {
        fetchLikedArticles();
      }
      break;
    case 'collections':
      if (collections.value.length === 0) {
        fetchCollections();
      }
      break;
    case 'comments':
      if (comments.value.length === 0) {
        fetchComments();
      }
      break;
  }
});

// 组件挂载时获取用户信息
onMounted(() => {
  getUserInfo();
  
  // 根据URL参数切换标签页
  const tabParam = route.query.tab;
  if (tabParam && ['basic', 'password', 'likes', 'collections', 'comments'].includes(tabParam)) {
    activeTab.value = tabParam;
  }
});
</script>

<style scoped>
.profile-layout {
  min-height: calc(100vh - 60px);
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;

}

.profile-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

@media (min-width: 768px) {
  .profile-info {
    flex-direction: row;
  }
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.avatar-uploader {
  text-align: center;
  margin-top: 10px;
}

.info-form {
  flex: 1;
}

/* 我的点赞 */
.my-likes {
  padding: 10px 0;
}

/* 我的收藏 */
.my-collections {
  padding: 10px 0;
}

.collection-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.collection-item {
  transition: all 0.3s;
}

.collection-item:hover {
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.article-info {
  display: flex;
  gap: 15px;
}

.article-cover {
  width: 180px;
  height: 120px;
  overflow: hidden;
  border-radius: 4px;
  flex-shrink: 0;
}

.article-cover .el-image {
  width: 100%;
  height: 100%;
}

.article-content {
  flex: 1;
  overflow: hidden;
}

.article-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: bold;
}

.article-title a {
  color: #303133;
  text-decoration: none;
}

.article-title a:hover {
  color: #409eff;
}

.article-summary {
  color: #606266;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  color: #909399;
  font-size: 13px;
}

.article-meta span {
  display: flex;
  align-items: center;
}

.article-meta .el-icon {
  margin-right: 4px;
}

.article-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.collect-time {
  color: #909399;
  font-size: 13px;
}

/* 我的评论 */
.my-comments {
  padding: 10px 0;
}

.comment-content {
  white-space: normal;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 公共样式 */
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 标签页样式优化 */
:deep(.el-tabs__item) {
  font-size: 16px;
  padding: 0 20px;
}

:deep(.el-tabs__header) {
  margin-bottom: 20px;
}
</style> 