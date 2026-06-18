<template>
  <el-container class="app-container">
    <el-aside width="220px" class="app-aside">
      <div class="logo-area">
        <el-icon :size="28" color="#409EFF"><Monitor /></el-icon>
        <span class="logo-text">影音运维台账</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="aside-menu"
      >
        <el-menu-item index="/devices">
          <el-icon><Monitor /></el-icon>
          <span>设备档案</span>
        </el-menu-item>
        <el-menu-item index="/status-wall">
          <el-icon><DataBoard /></el-icon>
          <span>状态墙</span>
        </el-menu-item>
        <el-menu-item index="/location">
          <el-icon><Location /></el-icon>
          <span>位置图</span>
        </el-menu-item>
        <el-menu-item index="/usage">
          <el-icon><Timer /></el-icon>
          <span>使用记录</span>
        </el-menu-item>
        <el-menu-item index="/repair">
          <el-icon><Warning /></el-icon>
          <span>检修登记</span>
        </el-menu-item>
        <el-sub-menu index="maintenance-group">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>保养管理</span>
          </template>
          <el-menu-item index="/maintenance">
            <span>养护记录</span>
          </el-menu-item>
          <el-menu-item index="/maintenance-checklist">
            <span>流程清单</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/cost-statistics">
          <el-icon><TrendCharts /></el-icon>
          <span>费用统计</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { Monitor, Timer, Warning, Tools, Location, DataBoard, TrendCharts } from '@element-plus/icons-vue'
</script>

<style scoped>
.app-container {
  height: 100vh;
}

.app-aside {
  background-color: #304156;
  overflow-y: auto;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  gap: 8px;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
}

.aside-menu {
  border-right: none;
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
