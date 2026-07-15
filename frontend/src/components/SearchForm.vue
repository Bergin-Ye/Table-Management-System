<template>
  <div class="search-card">
    <el-form :model="form" :inline="true" class="search-form">
      <slot></slot>
      <el-form-item>
        <el-button type="primary" @click="$emit('search')">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
        <el-button @click="$emit('reset')">
          <el-icon><Refresh /></el-icon>
          <span>重置</span>
        </el-button>
        <el-button v-if="showToggle" link type="primary" @click="collapsed = !collapsed">
          {{ collapsed ? '展开' : '收起' }}
          <el-icon><component :is="collapsed ? 'ArrowDown' : 'ArrowUp'" /></el-icon>
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  form: {
    type: Object,
    required: true
  },
  showToggle: {
    type: Boolean,
    default: false
  }
})

defineEmits(['search', 'reset'])

const collapsed = ref(false)
</script>

<style scoped>
.search-card {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 20px 20px 4px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #E5E5EA;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
}
</style>
