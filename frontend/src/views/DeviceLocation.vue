<template>
  <div class="device-location">
    <div class="top-bar">
      <div class="stats-bar">
        <div class="stat-item normal">
          <span class="stat-dot"></span>
          <span class="stat-label">正常</span>
          <span class="stat-count">{{ statusCount.NORMAL }}</span>
        </div>
        <div class="stat-item faulty">
          <span class="stat-dot"></span>
          <span class="stat-label">故障</span>
          <span class="stat-count">{{ statusCount.FAULTY }}</span>
        </div>
        <div class="stat-item maintenance">
          <span class="stat-dot"></span>
          <span class="stat-label">维修中</span>
          <span class="stat-count">{{ statusCount.MAINTENANCE }}</span>
        </div>
        <div class="stat-item retired">
          <span class="stat-dot"></span>
          <span class="stat-label">退役</span>
          <span class="stat-count">{{ statusCount.RETIRED }}</span>
        </div>
      </div>
      <div style="flex: 1" />
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px">
        <el-option label="正常" value="NORMAL" />
        <el-option label="故障" value="FAULTY" />
        <el-option label="维修中" value="MAINTENANCE" />
        <el-option label="退役" value="RETIRED" />
      </el-select>
      <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 140px">
        <el-option label="音响" value="SPEAKER" />
        <el-option label="投影仪" value="PROJECTOR" />
        <el-option label="播放器" value="PLAYER" />
        <el-option label="功放" value="AMPLIFIER" />
      </el-select>
    </div>

    <div v-loading="loading">
      <div v-for="(group, location) in locationGroups" :key="location" class="location-section">
        <div class="location-header">
          <div class="location-title">
            <el-icon :size="22" color="#409EFF"><component :is="locationIcon(location)" /></el-icon>
            <span class="location-name">{{ location }}</span>
            <el-tag size="small" type="info" class="location-count">
              {{ group.length }} 台设备
            </el-tag>
          </div>
          <div class="location-summary">
            <el-tag v-if="getCountByStatus(group, 'NORMAL') > 0" type="success" size="small">
              正常 {{ getCountByStatus(group, 'NORMAL') }}
            </el-tag>
            <el-tag v-if="getCountByStatus(group, 'FAULTY') > 0" type="danger" size="small">
              故障 {{ getCountByStatus(group, 'FAULTY') }}
            </el-tag>
            <el-tag v-if="getCountByStatus(group, 'MAINTENANCE') > 0" type="warning" size="small">
              维修中 {{ getCountByStatus(group, 'MAINTENANCE') }}
            </el-tag>
            <el-tag v-if="getCountByStatus(group, 'RETIRED') > 0" type="info" size="small">
              退役 {{ getCountByStatus(group, 'RETIRED') }}
            </el-tag>
          </div>
        </div>
        <div class="location-cards">
          <div class="card-wrapper" v-for="device in group" :key="device.id">
            <MiniDeviceCard :device="device" @detail="openDetail" />
          </div>
        </div>
      </div>

      <el-empty v-if="Object.keys(locationGroups).length === 0" description="暂无位置数据" />
    </div>

    <el-drawer v-model="detailVisible" :title="currentDevice?.name || '设备详情'" size="550px">
      <template v-if="currentDevice">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="设备名称">{{ currentDevice.name }}</el-descriptions-item>
          <el-descriptions-item label="设备型号">{{ currentDevice.model }}</el-descriptions-item>
          <el-descriptions-item label="设备类型">{{ typeMap[currentDevice.deviceType] || currentDevice.deviceType }}</el-descriptions-item>
          <el-descriptions-item label="设备状态">
            <el-tag :type="statusTagType(currentDevice.status)" size="small">{{ statusMap[currentDevice.status] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="购入日期">{{ currentDevice.purchaseDate }}</el-descriptions-item>
          <el-descriptions-item label="存放位置">{{ currentDevice.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="硬件规格">{{ currentDevice.hardwareSpecs || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { ElMessage } from 'element-plus'
import { House, OfficeBuilding, Reading, Film, Location } from '@element-plus/icons-vue'
import { Headset, Monitor, VideoPlay, Microphone } from '@element-plus/icons-vue'
import { getDevices } from '../api'

const MiniDeviceCard = {
  name: 'MiniDeviceCard',
  props: {
    device: { type: Object, required: true }
  },
  emits: ['detail'],
  setup(props, { emit }) {
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

    return () => h('div', {
      class: 'mini-device-card',
      onClick: () => emit('detail', props.device)
    }, [
      h('div', { class: 'mini-card-header' }, [
        h('div', { class: 'mini-type-icon' }, [
          h('el-icon', { size: 24 }, [h(typeIcon.value)])
        ]),
        h('el-tag', { type: statusType.value, size: 'small', class: 'mini-status-badge' }, statusLabel.value)
      ]),
      h('div', { class: 'mini-card-body' }, [
        h('h4', { class: 'mini-device-name' }, props.device.name),
        h('p', { class: 'mini-device-model' }, props.device.model),
        h('el-tag', { size: 'small', type: 'info', class: 'mini-type-tag' }, typeLabel.value)
      ])
    ])
  }
}

const devices = ref([])
const loading = ref(false)
const filterStatus = ref('')
const filterType = ref('')

const typeMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const statusMap = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '维修中', RETIRED: '退役' }

const detailVisible = ref(false)
const currentDevice = ref(null)

const defaultLocations = ['客厅', '影音室', '卧室', '书房', '厨房']

const filteredDevices = computed(() => {
  return devices.value.filter(d => {
    const matchStatus = !filterStatus.value || d.status === filterStatus.value
    const matchType = !filterType.value || d.deviceType === filterType.value
    return matchStatus && matchType
  })
})

const locationGroups = computed(() => {
  const groups = {}
  const unassigned = []

  filteredDevices.value.forEach(device => {
    const loc = device.location && device.location.trim() ? device.location.trim() : null
    if (loc) {
      if (!groups[loc]) groups[loc] = []
      groups[loc].push(device)
    } else {
      unassigned.push(device)
    }
  })

  if (unassigned.length > 0) {
    groups['未分配位置'] = unassigned
  }

  const sorted = {}
  const knownLocations = defaultLocations.filter(loc => groups[loc])
  const otherLocations = Object.keys(groups).filter(loc => !defaultLocations.includes(loc) && loc !== '未分配位置')

  knownLocations.forEach(loc => { sorted[loc] = groups[loc] })
  otherLocations.sort().forEach(loc => { sorted[loc] = groups[loc] })
  if (groups['未分配位置']) sorted['未分配位置'] = groups['未分配位置']

  return sorted
})

const statusCount = computed(() => {
  const count = { NORMAL: 0, FAULTY: 0, MAINTENANCE: 0, RETIRED: 0 }
  devices.value.forEach(d => {
    if (count[d.status] !== undefined) count[d.status]++
  })
  return count
})

const getCountByStatus = (group, status) => {
  return group.filter(d => d.status === status).length
}

const locationIcon = (location) => {
  const iconMap = {
    '客厅': House,
    '影音室': Film,
    '卧室': Reading,
    '书房': Reading,
    '厨房': OfficeBuilding
  }
  return iconMap[location] || Location
}

const statusTagType = (status) => {
  const map = { NORMAL: 'success', FAULTY: 'danger', MAINTENANCE: 'warning', RETIRED: 'info' }
  return map[status] || 'info'
}

const openDetail = (device) => {
  currentDevice.value = device
  detailVisible.value = true
}

const fetchDevices = async () => {
  loading.value = true
  try {
    const res = await getDevices()
    devices.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取设备列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchDevices)
</script>

<style scoped>
.device-location {
  padding: 0;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
}

.stats-bar {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.stat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}

.stat-item.normal .stat-dot { background: #67C23A; }
.stat-item.faulty .stat-dot { background: #F56C6C; }
.stat-item.maintenance .stat-dot { background: #E6A23C; }
.stat-item.retired .stat-dot { background: #909399; }

.stat-label {
  color: #606266;
}

.stat-count {
  font-weight: 600;
  color: #303133;
  min-width: 16px;
}

.location-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
}

.location-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.location-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.location-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.location-count {
  margin-left: 4px;
}

.location-summary {
  display: flex;
  gap: 6px;
}

.location-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.card-wrapper {
  cursor: pointer;
}

.mini-device-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
  transition: all 0.25s ease;
  background: #fff;
}

.mini-device-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.mini-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.mini-type-icon {
  color: #409EFF;
}

.mini-status-badge {
  font-size: 12px;
}

.mini-card-body {
  text-align: left;
  margin-bottom: 12px;
}

.mini-device-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-device-model {
  font-size: 13px;
  color: #909399;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-type-tag {
  margin-bottom: 10px;
}
</style>
