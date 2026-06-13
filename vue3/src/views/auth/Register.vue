<template>
  <Auth 
    :formData="registerForm" 
    :rules="rules" 
    :loading="loading"
    submitText="注册"
    formTitle="创建账号"
    @submit="handleSubmit"
  >
    <template #form-items>
      <div class="form-group">
        <el-form-item prop="username">
          <el-input 
            v-model="registerForm.username"
            :prefix-icon="User"
            placeholder="请输入用户名">
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="registerForm.password"
            :prefix-icon="Lock"
            type="password"
            placeholder="请输入密码">
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword"
            :prefix-icon="Lock"
            type="password"
            placeholder="请确认密码">
          </el-input>
        </el-form-item>
      </div>
      
      <div class="form-group">
        <el-form-item prop="email">
          <el-input 
            v-model="registerForm.email"
            :prefix-icon="Message"
            placeholder="请输入邮箱">
          </el-input>
        </el-form-item>
        <el-form-item prop="phone">
          <el-input 
            v-model="registerForm.phone"
            :prefix-icon="Phone"
            placeholder="请输入手机号">
          </el-input>
        </el-form-item>
        <el-form-item prop="name">
          <el-input 
            v-model="registerForm.name"
            :prefix-icon="UserFilled"
            placeholder="请输入姓名">
          </el-input>
        </el-form-item>
      </div>
    </template>

    <template #auth-links>
      <div class="register-links">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </template>
  </Auth>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Phone, UserFilled } from '@element-plus/icons-vue'
import { registerUser } from '@/api/user'
import Auth from './Auth.vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const userStore = useUserStore()

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  name: '',
  roleCode: 'USER', // 默认注册为普通用户
})

const validatePass2 = (rule, value, callback) => {
  if (value !== registerForm.password) {
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

const validatePhone = (rule, value, callback) => {
  if (value && !/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度必须在3到50个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度必须在6到100个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validatePass2, trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: validateEmail, trigger: 'blur' }
  ],
  phone: [
    { validator: validatePhone, trigger: 'blur' }
  ],
  name: [
    { required: false }
  ]
}

const handleSubmit = (form) => {
  formRef.value = form.value
  registerFormRef.value = form.value
  handleRegister()
}

const handleRegister = () => {
  registerFormRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      const { confirmPassword, ...registerData } = registerForm
      registerUser(registerData, {
        successMsg: '注册成功，请登录',
        onSuccess: () => {
          router.push('/login')
          loading.value = false
        },
        onError: (error) => {
          console.error('注册失败:', error)
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

.form-group {
  margin-bottom: 10px;
  position: relative;
  
  &::after {
    content: "";
    position: absolute;
    width: 80%;
    height: 1px;
    bottom: -5px;
    left: 10%;
    background: linear-gradient(90deg, transparent, rgba(162, 155, 254, 0.3), transparent);
  }

  &:last-child::after {
    display: none;
  }
}

.register-links {
  width: 100%;
  text-align: center;
  font-size: 14px;
  font-family: 'PingFangSC-Medium', 'JetBrainsMono-Medium', sans-serif;
  
  a {
    font-weight: 500;
    margin-left: 5px;
    position: relative;
    
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
}

/* 图标样式 */
:deep(.el-input__prefix) {
  color: #a29bfe;
  
  svg {
    transition: transform 0.3s ease;
  }
}

/* 输入框悬停效果 */
:deep(.el-input__wrapper:hover) {
  .el-input__prefix svg {
    transform: scale(1.2);
  }
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

/* 表单项过渡动画 */
:deep(.el-form-item) {
  transition: transform 0.3s ease;
  
  &:hover {
    transform: translateX(5px);
  }
}
</style> 