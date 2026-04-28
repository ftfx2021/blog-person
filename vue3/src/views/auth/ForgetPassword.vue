<template>
  <Auth 
    :formData="forgetForm" 
    :rules="rules" 
    :loading="loading"
    submitText="重置密码"
    formTitle="找回密码"
    @submit="handleSubmit"
  >
    <template #form-items>
      <div class="forget-form-content">
        <p class="form-description">请输入您的注册邮箱和新密码</p>
        
        <el-form-item prop="email">
          <el-input 
            v-model="forgetForm.email"
            :prefix-icon="Message"
            placeholder="请输入注册邮箱">
          </el-input>
        </el-form-item>
        
        <div class="password-group">
          <el-form-item prop="newPassword">
            <el-input 
              v-model="forgetForm.newPassword"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入新密码">
            </el-input>
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <el-input 
              v-model="forgetForm.confirmPassword"
              :prefix-icon="Lock"
              type="password"
              placeholder="请确认新密码"
              @keyup.enter="handleSubmit(formRef)">
            </el-input>
          </el-form-item>
        </div>
      </div>
    </template>

    <template #auth-links>
      <div class="forget-links">
        <router-link to="/login">返回登录</router-link>
        <router-link to="/register">立即注册</router-link>
      </div>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Lock } from '@element-plus/icons-vue'
import { forgetPassword } from '@/api/user'
import Auth from './Auth.vue'

const router = useRouter()
const formRef = ref(null)
const forgetFormRef = ref(null)
const loading = ref(false)

const forgetForm = reactive({
  email: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value !== forgetForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  const emailRegex = /^[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)*@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/
  if (!emailRegex.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ]
}

const handleSubmit = (form) => {
  formRef.value = form.value
  forgetFormRef.value = form.value
  handleForget()
}

const handleForget = () => {
  forgetFormRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      forgetPassword({
        email: forgetForm.email,
        newPassword: forgetForm.newPassword
      }, {
        successMsg: "密码重置成功，请重新登录",
        onSuccess: () => {
          router.push('/login')
          loading.value = false
        },
        onError: (error) => {
          console.error('密码重置失败:', error)
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

.forget-form-content {
  .form-description {
    text-align: center;
    color: #6c5ce7;
    margin-bottom: 25px;
    font-size: 14px;
    line-height: 1.5;
    padding: 12px;
    border-radius: 12px;
    background: rgba(162, 155, 254, 0.1);
    position: relative;
    overflow: hidden;
    font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
    
    &::before {
      content: "";
      position: absolute;
      width: 100%;
      height: 2px;
      bottom: 0;
      left: 0;
      background: linear-gradient(90deg, transparent, rgba(162, 155, 254, 0.5), transparent);
      animation: shimmer 2s infinite;
    }
  }
}

@keyframes shimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

.password-group {
  margin-top: 15px;
  position: relative;
  
  &::after {
    content: "";
    position: absolute;
    width: 0;
    height: 100%;
    border-left: 2px dashed rgba(162, 155, 254, 0.2);
    left: -15px;
    top: 0;
  }
  
  :deep(.el-form-item) {
    position: relative;
    
    &:first-child::before {
      content: "①";
      position: absolute;
      left: -30px;
      top: 15px;
      color: #a29bfe;
      font-size: 12px;
    }
    
    &:last-child::before {
      content: "②";
      position: absolute;
      left: -30px;
      top: 15px;
      color: #a29bfe;
      font-size: 12px;
    }
  }
}

.forget-links {
  display: flex;
  justify-content: space-between;
  width: 100%;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
  
  a {
    font-size: 14px;
    position: relative;
    transition: all 0.3s ease;
    
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
    
    &:hover {
      transform: translateY(-2px);
      
      &::after {
        width: 100%;
      }
    }
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