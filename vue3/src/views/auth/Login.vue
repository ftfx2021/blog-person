<template>
  <Auth 
    :formData="loginForm" 
    :rules="rules" 
    :loading="loading"
    submitText="登录"
    formTitle="欢迎回来"
    @submit="handleSubmit"
  >
    <template #form-items>
      <el-form-item prop="username">
        <el-input 
          v-model="loginForm.username"
          :prefix-icon="User"
          placeholder="请输入用户名">
        </el-input>
      </el-form-item>
      <el-form-item prop="password">
        <el-input 
          v-model="loginForm.password"
          :prefix-icon="Lock"
          type="password"
          placeholder="请输入密码"
          @keyup.enter="handleSubmit(loginFormRef)">
        </el-input>
      </el-form-item>
    </template>

    <template #auth-links>
      <router-link to="/forget" class="forget-link">忘记密码？</router-link>
      <router-link to="/register">立即注册</router-link>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { loginUser } from '@/api/user'
import { User, Lock } from '@element-plus/icons-vue'

import Auth from './Auth.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const handleSubmit = (form) => {
  formRef.value = form.value
  loginFormRef.value = form.value
  handleLogin()
}

const loginFormRef = ref(null)

const handleLogin = () => {
  loginFormRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      loginUser(loginForm, {
        successMsg: "登录成功",
        onSuccess: async (data) => {
          userStore.setUserInfo(data)
          
          // 获取重定向路径（优先级：sessionStorage > query参数 > 默认路径）
          const savedRedirect = sessionStorage.getItem('redirectPath')
          let redirectPath = savedRedirect || route.query.redirect
          
          // 清除保存的重定向路径
          if (savedRedirect) {
            sessionStorage.removeItem('redirectPath')
          }
          
          // 根据返回的角色决定跳转路径
          if (data.roleCode !== 'USER') {
            await router.isReady()
            router.push(redirectPath || '/back/dashboard')
          } else {
            // 普通用户登录，直接跳转到前台
            router.push(redirectPath || '/')
          }
          loading.value = false
        },
        onError: (error) => {
          console.error('登录失败:', error)
          loading.value = false
        }
      })
    }
  })
}
</script>

<style lang="scss" scoped>
// 导入项目字体
@font-face {
  font-family: 'PingFangSC-Medium';
  src: url('@/assets/font/PingFangSC-Medium.woff2') format('woff2');
  font-weight: normal;
  font-style: normal;
}

@font-face {
  font-family: 'JetBrainsMono-Medium';
  src: url('@/assets/font/JetBrainsMono-Medium.woff2') format('woff2');
  font-weight: normal;
  font-style: normal;
}

.forget-link {
  position: relative;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
  &::after {
    content: "";
    position: absolute;
    width: 0;
    height: 2px;
    bottom: -2px;
    left: 0;
    background: linear-gradient(90deg, #6c5ce7, #a29bfe);
    transition: width 0.3s ease;
  }
  
  &:hover::after {
    width: 100%;
  }
}

/* 图标样式 */
:deep(.el-input__prefix) {
  color: #a29bfe;
}

/* 输入框焦点动画 */
:deep(.el-input__wrapper) {
  &.is-focus {
    animation: input-glow 1.5s infinite alternate;
  }
}

@keyframes input-glow {
  from {
    box-shadow: 0 0 2px #6c5ce7;
  }
  to {
    box-shadow: 0 0 8px #a29bfe;
  }
}
</style> 