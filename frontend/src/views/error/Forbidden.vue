<template>
  <div class="forbidden-page">
    <div class="forbidden-content">
      <el-icon
        :size="80"
        color="#f56c6c"
      >
        <Lock />
      </el-icon>
      <h1>403</h1>
      <h2>抱歉，您没有权限访问此页面</h2>
      <p>请联系管理员获取相应权限，或返回其他页面</p>
      <div class="actions">
        <el-button
          type="primary"
          @click="goBack"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回上页
        </el-button>
        <el-button @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.go(-1)
}

const goHome = () => {
  router.push('/')
}
</script>

<style lang="scss" scoped>
.forbidden-page {
  height: calc(100vh - 120px);
  display: flex;
  justify-content: center;
  align-items: center;
}

.forbidden-content {
  text-align: center;
  
  h1 {
    font-size: 72px;
    color: #f56c6c;
    margin: 20px 0 10px;
    font-weight: 600;
  }
  
  h2 {
    font-size: 20px;
    color: #303133;
    margin-bottom: 10px;
  }
  
  p {
    color: #909399;
    margin-bottom: 30px;
  }
  
  .actions {
    display: flex;
    gap: 12px;
    justify-content: center;
  }
}
</style>
