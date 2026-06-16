<template>
  <div class="status-wall">
    <div class="wall-header">
      <h2 class="wall-title">设备状态墙</h2>
      <el-button :icon="Refresh" circle @click="fetchData" :loading="loading" />
    </div>

    <div class="summary-bar">
      <div class="summary-item" v-for="s in summaryStats" :key="s.key">
        <span class="summary-count" :style="{ color: s.color }">{{ s.count }}</span>
        <span class="summary-label">{{ s.label }}</span>
      </div>
    </div>

    <div class="status-legend">
      <span class="legend-item"><span class="legend-dot" style="background: #67C23A"></span>正常</span>
      <span class="legend-item"><span class="legend-dot" style="background: #F56C6C"></span>故障</span>
      <span class="legend-item"><span class="legend-dot" style="background: #E6A23C"></span>保养中</span>
      <span class="legend-item"><span class="legend-dot" style="background: #909399"></span>退役</span>
    </div>

    <div v-loading="loading" class="wall-content">
      <div v-for="group in wallData" :key="group.deviceType" class="type-section">
        <div class="section-header">
          <div class="section-title-area">
            <el-icon :size="22" color="#409EFF"><component :is="typeIconMap[group.deviceType]" /></el-icon>
            <span class="section-title">{{ typeLabelMap[group.deviceType] }}</span>
            <span class="section-count">{{ group.totalCount }} 台</span>
          </div>
          <div class="section-status-bar">
            <el-tag v-for="(count, status) in group.statusCounts" :key="status" :type="statusTagType(status)" size="small" class="status-chip">
              {{ statusLabelMap[status] || status }} {{ count }}
            </el-tag>
          </div>
        </div>
        <div class="device-grid">
          <div
            v-for="device in group.devices"
            :key="device.id"
            class="device-tile"
            :class="'tile-' + device.status.toLowerCase()"
          >
            <div class="tile-header">
              <span class="tile-status-dot" :style="{ background: statusColorMap[device.status] }"></span>
              <span class="tile-name">{{ device.name }}</span>
            </div>
            <div class="tile-model">{{ device.model }}</div>
            <div class="tile-info">
              <span class="tile-location">
                <el-icon :size="12"><Location /></el-icon>
                {{ device.location || '未设置' }}
              </span>
              <el-tag :type="statusTagType(device.status)" size="small" effect="dark">
                {{ statusLabelMap[device.status] || device.status }}
              </el-tag>
            </div>
            <div class="tile-actions-row">
              <div v-if="device.lastMaintenanceTime" class="tile-last-action">
                <el-icon :size="11" color="#67C23A"><Tools /></el-icon>
                <span>{{ formatTime(device.lastMaintenanceTime) }}</span>
              </div>
              <div v-if="device.lastRepairTime" class="tile-last-action">
                <el-icon :size="11" color="#F56C6C"><Warning /></el-icon>
                <span>{{ formatTime(device.lastRepairTime) }}</span>
              </div>
              <div v-if="!device.lastMaintenanceTime && !device.lastRepairTime" class="tile-last-action no-record">
                暂无维护记录
              </div>
            </div>
            <div v-if="getNextWindowSummary(device)" class="tile-next-window" :class="{ overdue: getNextWindowSummary(device).overdue }">
              <el-icon :size="11" :color="getNextWindowSummary(device).overdue ? '#F56C6C' : '#E6A23C'"><Warning /></el-icon>
              <span>{{ getNextWindowSummary(device).overdue ? '逾期' + Math.abs(getNextWindowSummary(device).daysUntil) + '天' : '下次保养 ' + getNextWindowSummary(device).daysUntil + '天' }}</span>
            </div>
          </div>
          <el-empty v-if="group.devices.length === 0" description="暂无设备" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Location, Tools, Warning, Headset, Monitor, VideoPlay, Microphone } from '@element-plus/icons-vue'
import { getStatusWall } from '../api'

const loading = ref(false)
const wallData = ref([])

const typeLabelMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const typeIconMap = { SPEAKER: Headset, PROJECTOR: Monitor, PLAYER: VideoPlay, AMPLIFIER: Microphone }
const statusLabelMap = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '保养中', RETIRED: '退役' }
const statusColorMap = { NORMAL: '#67C23A', FAULTY: '#F56C6C', MAINTENANCE: '#E6A23C', RETIRED: '#909399' }

const summaryStats = computed(() => {
  const counts = { NORMAL: 0, FAULTY: 0, MAINTENANCE: 0, RETIRED: 0 }
  wallData.value.forEach(g => {
    Object.entries(g.statusCounts || {}).forEach(([k, v]) => {
      counts[k] = (counts[k] || 0) + v
    })
  })
  return [
    { key: 'NORMAL', label: '正常', count: counts.NORMAL, color: '#67C23A' },
    { key: 'FAULTY', label: '故障', count: counts.FAULTY, color: '#F56C6C' },
    { key: 'MAINTENANCE', label: '保养中', count: counts.MAINTENANCE, color: '#E6A23C' },
    { key: 'RETIRED', label: '退役', count: counts.RETIRED, color: '#909399' }
  ]
})

const statusTagType = (status) => {
  const map = { NORMAL: 'success', FAULTY: 'danger', MAINTENANCE: 'warning', RETIRED: 'info' }
  return map[status] || 'info'
}

const formatTime = (str) => {
  if (!str) return '-'
  return str.length > 16 ? str.slice(0, 16).replace('T', ' ') : str.replace('T', ' ')
}

const getNextWindowSummary = (device) => {
  if (!device.nextMaintenanceWindows) return null
  const windows = device.nextMaintenanceWindows
  let earliest = null
  for (const type in windows) {
    const w = windows[type]
    if (w.nextTime) {
      const nextDate = new Date(w.nextTime)
      if (!isNaN(nextDate.getTime())) {
        const now = new Date()
        const diffDays = Math.ceil((nextDate.getTime() - now.getTime()) / (24 * 60 * 60 * 1000))
        if (earliest === null || diffDays < earliest.daysUntil) {
          earliest = { maintenanceType: type, daysUntil: diffDays, nextTime: w.nextTime, overdue: diffDays < 0 }
        }
      }
    }
  }
  return earliest
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStatusWall()
    wallData.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取状态墙数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.status-wall {
  padding: 0;
}
.wall-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
}
.wall-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.summary-bar {
  display: flex;
  gap: 24px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.summary-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.summary-count {
  font-size: 28px;
  font-weight: 700;
}
.summary-label {
  font-size: 14px;
  color: #606266;
}
.status-legend {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding: 0 4px;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.type-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.section-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.section-count {
  font-size: 13px;
  color: #909399;
  margin-left: 4px;
}
.section-status-bar {
  display: flex;
  gap: 6px;
}
.status-chip {
  border-radius: 12px;
}
.device-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.device-tile {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
  transition: all 0.2s ease;
  position: relative;
  overflow: hidden;
}
.device-tile::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
}
.device-tile:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.tile-normal::before { background: #67C23A; }
.tile-faulty::before { background: #F56C6C; }
.tile-maintenance::before { background: #E6A23C; }
.tile-retired::before { background: #909399; }
.tile-retired {
  opacity: 0.6;
  background: #fafafa;
}
.tile-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.tile-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.tile-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tile-model {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tile-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  gap: 8px;
}
.tile-location {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #606266;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tile-actions-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  border-top: 1px solid #f2f3f5;
  padding-top: 8px;
}
.tile-last-action {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #909399;
}
.tile-last-action.no-record {
  color: #c0c4cc;
  font-style: italic;
}
.tile-next-window {
  display: flex;
  align-items: center;
  gap: 3px;
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid #f2f3f5;
  font-size: 11px;
  font-weight: 600;
  color: #e6a23c;
}
.tile-next-window.overdue {
  color: #f56c6c;
}
</style>
