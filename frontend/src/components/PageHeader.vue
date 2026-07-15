<template>
  <div class="page-header">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>{{ title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <h2 class="page-title">{{ title }}</h2>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  }
})
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  margin: 8px 0 0 0;
  font-size: 22px;
  font-weight: 600;
  color: #1D1D1F;
}
</style>
