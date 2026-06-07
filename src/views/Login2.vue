<script setup lang="ts">
</script>

<template>
  <div class="login-container">
    <div class="login-card">
  <el-form>
    <div class="title">权限管理系统</div>
  <el-form-item label="用户名" prop="username">
    <el-input  placeholder="请输入用户名" />
  </el-form-item>
  <el-form-item label="密码" prop="password">
    <el-input placeholder="请输入密码"/>
  </el-form-item>
  <el-form-item>
    <el-button type="primary" >
      登录
    </el-button>
  </el-form-item>
  </el-form>
  </div>
  </div>
</template>

<style scoped>
.login-container{
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #ffffff;
}
.title{
text-align: center;
  font-size: 20px;
  margin-bottom: 20px;
  font-weight: 600;
}
.login-card{
  background: #6363d5;
  padding: 2rem 2.5rem;
  border-radius: 16px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
  transition: transform 0.2s;
}
</style>