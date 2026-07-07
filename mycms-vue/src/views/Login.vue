<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="title">用户登录</h2>
        <div class="input-group">
          <label for="username">用户名</label>
          <input
              id="username"
              type="text"
              v-model="form.username"
              placeholder="请输入用户名"
          />
        </div>

        <div class="input-group">
          <label for="password">密码</label>
          <input
              id="password"
              type="password"
              v-model="form.password"
              placeholder="请输入密码"
          />
        </div>
        <button class="login-btn" @click=handleLogin>登录</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import {getUser} from "@/http/login.ts";

const router = useRouter()
// 表单数据
const form = reactive({
  username: 'sun',
  password: '784250'
})
const handleLogin = async () => {
  console.log('点击了登录按钮')

  try {
    const result = await getUser(form.username, form.password);
    console.log('后端返回结果：', result);
    if (result.code === 200) {
      console.log(result);
      if (result.data?.menuTree) {
        sessionStorage.setItem('menuTree', JSON.stringify(result.data.menuTree));
      }

      await router.push('/menu');
    } else {
      console.warn('登录失败：', result);
    }
  } catch (error) {
    console.error('登录请求失败', error);
  }
}
</script>

<style scoped>
/* 全屏居中，深色渐变背景 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #9966ea 0%, #a24b96 100%);
}

/* 登录卡片 */
.login-card {
  background: #e6cbcb;
  padding: 2rem 2.5rem;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  transition: transform 0.2s;
}

.login-card:hover {
  transform: translateY(-2px);
}

.title {
  text-align: center;
  margin-bottom: 1.5rem;
  color: #6363d5;
  font-weight: 600;
}

/* 表单项 */
.input-group {
  margin-bottom: 1.25rem;
}

.input-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: #555;
}

.input-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
}

.input-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
.login-btn {
  display: block;
  width: 50%;
  margin-top: 0.5rem;
  padding: 0.75rem;
  border: none;
  border-radius: 8px;
  background-color: #6363d5;
  color: white;
  font-size: 1rem;
  cursor: pointer;
}
</style>
