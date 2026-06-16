<template>
  <el-card class="device-card" shadow="hover" @mouseenter="hovered = true" @mouseleave="hovered = false">
    <div class="card-header">
      <div class="type-icon">
        <el-icon :size="32">
          <component :is="typeIcon" />
        </el-icon>
      </div>
      <el-tag :type="statusType" size="small" class="status-badge">{{ statusLabel }}</el-tag>
    </div>
    <div class="card-body">
      <h3 class="device-name">{{ device.name }}</h3>
      <p class="device-model">{{ device.model }}</p>
      <el-tag size="small" type="info" class="type-tag">{{ typeLabel }}</el-tag>
      <div v-if="maintenanceIndicator" class="maintenance-badge" :class="{ overdue: maintenanceIndicator.overdue }">
        <el-icon :size="12"><Warning /></el-icon>
        <span v-if="maintenanceIndicator.overdue">逾期{{ Math.abs(maintenanceIndicator.daysUntil) }}天</span>
        <span v-else>{{ maintenanceIndicator.maintenanceTypeLabel }} {{ maintenanceIndicator.daysUntil }}天</span>
      </div>
      <div class="device-info">
        <p><span class="label">购入日期：</span>{{ device.purchaseDate }}</p>
        <p><span class="label">位置：</span>{{ device.location || '-' }}</p>
        <p v-if="device.hardwareSpecs"><span class="label">规格：</span>{{ device.hardwareSpecs }}</p>
      </div>
    </div>
    <div class="card-actions">
      <el-button type="primary" link size="small" @click="$emit('edit', device)">编辑</el-button>
      <el-button type="danger" link size="small" @click="$emit('delete', device)">删除</el-button>
      <el-button type="success" link size="small" @click="$emit('detail', device)">详情</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Headset, Monitor, VideoPlay, Microphone, Warning } from '@element-plus/icons-vue'
import { calculateNextWindows, getEarliestUrgentWindow } from '../utils/maintenanceInterval'

const props = defineProps({
  device: { type: Object, required: true },
  lastMaintenanceTime: { type: String, default: null }
})

defineEmits(['edit', 'delete', 'detail'])

const hovered = ref(false)

const typeMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const statusMap = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '维修中', RETIRED: '退役' }

const typeIcon = computed(() => {
  const map = { SPEAKER: Headset, PROJECTOR: Monitor, PLAYER: VideoPlay, AMPLIFIER: Microphone }
  return map[props.device.deviceType] || Monitor
})

const typeLabel = computed(() => typeMap[props.device.deviceType] || props.device.deviceType)

const statusType = computed(() => {
  const map = { NORMAL: 'success', FAULTY: 'danger', MAINTENANCE: 'warning', RETIRED: 'info' }
  return map[props.device.status] || 'info'
})

const statusLabel = computed(() => statusMap[props.device.status] || '未知')

const maintenanceIndicator = computed(() => {
  if (!props.lastMaintenanceTime) return null
  const windows = calculateNextWindows(props.device.deviceType, props.lastMaintenanceTime)
  return getEarliestUrgentWindow(windows)
})
</script>

<style scoped>
.device-card {
  transition: all 0.3s ease;
  border-radius: 8px;
  overflow: hidden;
}
.device-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.type-icon {
  color: #409EFF;
}
.status-badge {
  font-size: 12px;
}
.card-body {
  margin-bottom: 12px;
}
.device-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
}
.device-model {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px 0;
}
.type-tag {
  margin-bottom: 10px;
}
.maintenance-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  font-weight: 600;
  color: #e6a23c;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 10px;
  padding: 2px 8px;
  margin-left: 6px;
  vertical-align: middle;
}
.maintenance-badge.overdue {
  color: #f56c6c;
  background: #fef0f0;
  border-color: #fde2e2;
}
.device-info p {
  font-size: 13px;
  color: #606266;
  margin: 4px 0;
  line-height: 1.6;
}
.device-info .label {
  color: #909399;
}
.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}
</style>
