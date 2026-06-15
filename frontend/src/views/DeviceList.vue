<template>
  <div class="device-list">
    <div class="top-bar">
      <el-input
        v-model="searchText"
        placeholder="搜索设备名称/型号"
        clearable
        style="width: 240px"
        :prefix-icon="Search"
      />
      <el-select v-model="filterType" placeholder="设备类型" clearable style="width: 140px">
        <el-option label="音响" value="SPEAKER" />
        <el-option label="投影仪" value="PROJECTOR" />
        <el-option label="播放器" value="PLAYER" />
        <el-option label="功放" value="AMPLIFIER" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="设备状态" clearable style="width: 140px">
        <el-option label="正常" value="NORMAL" />
        <el-option label="故障" value="FAULTY" />
        <el-option label="维修中" value="MAINTENANCE" />
        <el-option label="退役" value="RETIRED" />
      </el-select>
      <div style="flex: 1" />
      <el-button type="primary" :icon="Plus" @click="openForm()">新增设备</el-button>
      <el-button :icon="Edit" @click="batchEditVisible = true">批量编辑</el-button>
    </div>

    <el-row :gutter="16">
      <el-col :span="6" v-for="device in filteredDevices" :key="device.id">
        <DeviceCard
          :device="device"
          @edit="openForm"
          @delete="handleDelete"
          @detail="openDetail"
        />
      </el-col>
    </el-row>

    <el-empty v-if="filteredDevices.length === 0" description="暂无设备数据" />

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新增设备' : '编辑设备'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备型号" prop="model">
          <el-input v-model="form.model" placeholder="请输入设备型号" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceType">
          <el-select v-model="form.deviceType" placeholder="请选择设备类型" style="width: 100%">
            <el-option label="音响" value="SPEAKER" />
            <el-option label="投影仪" value="PROJECTOR" />
            <el-option label="播放器" value="PLAYER" />
            <el-option label="功放" value="AMPLIFIER" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="正常" value="NORMAL" />
            <el-option label="故障" value="FAULTY" />
            <el-option label="维修中" value="MAINTENANCE" />
            <el-option label="退役" value="RETIRED" />
          </el-select>
        </el-form-item>
        <el-form-item label="购入日期" prop="purchaseDate">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择购入日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="存放位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入存放位置" />
        </el-form-item>
        <el-form-item label="硬件规格" prop="hardwareSpecs">
          <el-input v-model="form.hardwareSpecs" type="textarea" :rows="3" placeholder="请输入硬件规格参数" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

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
        <el-tabs v-model="detailTab" style="margin-top: 20px">
          <el-tab-pane label="运维总览" name="overview">
            <el-row :gutter="12" class="overview-stats">
              <el-col :span="8">
                <div class="overview-card">
                  <div class="overview-card-label">累计使用时长</div>
                  <div class="overview-card-value overview-blue">{{ overviewStats.totalDuration }}</div>
                  <div class="overview-card-sub">共 {{ overviewStats.usageCount }} 次使用</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-card">
                  <div class="overview-card-label">最近维修</div>
                  <div class="overview-card-value overview-red">{{ overviewStats.lastRepairTime }}</div>
                  <div class="overview-card-sub">累计 {{ overviewStats.repairCount }} 次检修</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-card">
                  <div class="overview-card-label">最近保养</div>
                  <div class="overview-card-value overview-green">{{ overviewStats.lastMaintenanceTime }}</div>
                  <div class="overview-card-sub">累计 {{ overviewStats.maintenanceCount }} 次养护</div>
                </div>
              </el-col>
            </el-row>

            <div class="timeline-section">
              <div class="section-title">运维时间线</div>
              <el-timeline v-if="getOverviewTimeline().length > 0">
                <el-timeline-item
                  v-for="(event, idx) in getOverviewTimeline()"
                  :key="idx"
                  :timestamp="event.time"
                  :color="timelineTypeColor(event.type)"
                  placement="top"
                >
                  <el-card shadow="never" class="timeline-card">
                    <div class="timeline-header">
                      <el-tag :type="event.type === 'usage' ? 'primary' : event.type === 'repair' ? 'danger' : event.type === 'maintenance' ? 'success' : 'warning'" size="small">
                        {{ timelineTypeLabel(event.type) }}
                      </el-tag>
                      <span class="timeline-title">{{ event.title }}</span>
                    </div>
                    <div class="timeline-content">{{ event.content }}</div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无运维记录" :image-size="60" />
            </div>
          </el-tab-pane>
          <el-tab-pane label="使用记录" name="usage">
            <el-table :data="detailUsageList" size="small" v-loading="detailLoading">
              <el-table-column prop="usageDate" label="日期" width="110" />
              <el-table-column label="时长" width="100">
                <template #default="{ row }">
                  {{ Math.floor(row.durationMinutes / 60) }}时{{ row.durationMinutes % 60 }}分
                </template>
              </el-table-column>
              <el-table-column prop="scenario" label="场景" width="90" />
              <el-table-column prop="remark" label="备注" />
            </el-table>
            <el-empty v-if="detailUsageList.length === 0 && !detailLoading" description="暂无使用记录" :image-size="60" />
          </el-tab-pane>
          <el-tab-pane label="检修记录" name="repair">
            <el-table :data="detailRepairList" size="small" v-loading="detailLoading">
              <el-table-column prop="repairTime" label="时间" width="160" />
              <el-table-column prop="symptom" label="异常现象" show-overflow-tooltip />
              <el-table-column label="费用" width="90">
                <template #default="{ row }">¥{{ row.cost || 0 }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="detailRepairList.length === 0 && !detailLoading" description="暂无检修记录" :image-size="60" />
          </el-tab-pane>
          <el-tab-pane label="养护记录" name="maintenance">
            <el-table :data="detailMaintenanceList" size="small" v-loading="detailLoading">
              <el-table-column prop="maintenanceTime" label="时间" width="160" />
              <el-table-column label="类型" width="90">
                <template #default="{ row }">{{ mtMap[row.maintenanceType] || row.maintenanceType }}</template>
              </el-table-column>
              <el-table-column prop="content" label="内容" show-overflow-tooltip />
            </el-table>
            <el-empty v-if="detailMaintenanceList.length === 0 && !detailLoading" description="暂无养护记录" :image-size="60" />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <BatchEdit
      :visible="batchEditVisible"
      :devices="devices"
      @update="handleBatchUpdate"
      @close="batchEditVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit } from '@element-plus/icons-vue'
import DeviceCard from '../components/DeviceCard.vue'
import BatchEdit from '../components/BatchEdit.vue'
import {
  getDevices, createDevice, updateDevice, deleteDevice, batchUpdateDevices,
  getUsageByDevice, getRepairByDevice, getMaintenanceByDevice, getUsageStats
} from '../api'

const devices = ref([])
const searchText = ref('')
const filterType = ref('')
const filterStatus = ref('')

const typeMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const statusMap = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '维修中', RETIRED: '退役' }
const mtMap = { CLEANING: '机身清洁', CABLE: '线路整理', FIRMWARE: '固件升级', OTHER: '其他' }

const formVisible = ref(false)
const formMode = ref('create')
const formRef = ref(null)
const form = ref({
  name: '', model: '', deviceType: '', status: 'NORMAL',
  purchaseDate: '', location: '', hardwareSpecs: ''
})

const formRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  model: [{ required: true, message: '请输入设备型号', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择设备状态', trigger: 'change' }]
}

const detailVisible = ref(false)
const currentDevice = ref(null)
const detailTab = ref('overview')
const detailUsageList = ref([])
const detailRepairList = ref([])
const detailMaintenanceList = ref([])
const detailLoading = ref(false)
const overviewStats = ref({
  totalDuration: '0时0分',
  lastRepairTime: '-',
  lastMaintenanceTime: '-',
  repairCount: 0,
  maintenanceCount: 0,
  usageCount: 0
})

const batchEditVisible = ref(false)

const filteredDevices = computed(() => {
  return devices.value.filter(d => {
    const matchSearch = !searchText.value ||
      d.name?.includes(searchText.value) ||
      d.model?.includes(searchText.value)
    const matchType = !filterType.value || d.deviceType === filterType.value
    const matchStatus = !filterStatus.value || d.status === filterStatus.value
    return matchSearch && matchType && matchStatus
  })
})

const statusTagType = (status) => {
  const map = { NORMAL: 'success', FAULTY: 'danger', MAINTENANCE: 'warning', RETIRED: 'info' }
  return map[status] || 'info'
}

const formatDuration = (minutes) => {
  if (!minutes && minutes !== 0) return '0时0分'
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  return h > 0 ? `${h}时${m}分` : `${m}分`
}

const formatDateTimeShort = (value) => {
  if (!value) return '-'
  const str = String(value)
  if (str.includes('T')) {
    const d = new Date(str)
    if (!isNaN(d.getTime())) {
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
  }
  return str.length > 16 ? str.slice(0, 16) : str
}

const calcOverviewStats = (usage, repair, maintenance, usageStats) => {
  const totalMin = usageStats?.totalMinutes || usage.reduce((sum, r) => sum + (r.durationMinutes || 0), 0)
  const sortedRepair = [...repair].sort((a, b) => new Date(b.repairTime) - new Date(a.repairTime))
  const sortedMaintenance = [...maintenance].sort((a, b) => new Date(b.maintenanceTime) - new Date(a.maintenanceTime))
  overviewStats.value = {
    totalDuration: formatDuration(totalMin),
    lastRepairTime: sortedRepair.length > 0 ? formatDateTimeShort(sortedRepair[0].repairTime) : '暂无记录',
    lastMaintenanceTime: sortedMaintenance.length > 0 ? formatDateTimeShort(sortedMaintenance[0].maintenanceTime) : '暂无记录',
    repairCount: repair.length,
    maintenanceCount: maintenance.length,
    usageCount: usage.length
  }
}

const getOverviewTimeline = () => {
  const events = []
  detailUsageList.value.forEach(r => {
    events.push({
      type: 'usage',
      time: r.usageDate,
      timestamp: new Date(r.usageDate).getTime(),
      title: `使用记录 - ${Math.floor(r.durationMinutes / 60)}时${r.durationMinutes % 60}分`,
      content: `场景：${r.scenario || '-'}${r.remark ? ' | 备注：' + r.remark : ''}`
    })
  })
  detailRepairList.value.forEach(r => {
    events.push({
      type: 'repair',
      time: formatDateTimeShort(r.repairTime),
      timestamp: new Date(r.repairTime).getTime(),
      title: `检修记录${r.cost ? ' - ¥' + Number(r.cost).toFixed(2) : ''}`,
      content: `异常：${r.symptom || '-'}${r.fixMethod ? ' | 修复：' + r.fixMethod : ''}`
    })
  })
  detailMaintenanceList.value.forEach(r => {
    events.push({
      type: 'maintenance',
      time: formatDateTimeShort(r.maintenanceTime),
      timestamp: new Date(r.maintenanceTime).getTime(),
      title: `养护记录 - ${mtMap[r.maintenanceType] || r.maintenanceType}`,
      content: `内容：${r.content || '-'}${r.operator ? ' | 操作人：' + r.operator : ''}`
    })
  })
  if (currentDevice.value?.createdAt) {
    events.push({
      type: 'status',
      time: formatDateTimeShort(currentDevice.value.createdAt),
      timestamp: new Date(currentDevice.value.createdAt).getTime(),
      title: '设备入库',
      content: `初始状态：${statusMap[currentDevice.value.status] || currentDevice.value.status}`
    })
  }
  return events.sort((a, b) => b.timestamp - a.timestamp)
}

const timelineTypeColor = (type) => {
  const map = { usage: '#409EFF', repair: '#F56C6C', maintenance: '#67C23A', status: '#E6A23C' }
  return map[type] || '#909399'
}

const timelineTypeLabel = (type) => {
  const map = { usage: '使用', repair: '检修', maintenance: '养护', status: '状态' }
  return map[type] || type
}

const fetchDevices = async () => {
  try {
    const res = await getDevices()
    devices.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取设备列表失败')
  }
}

const openForm = (device = null) => {
  if (device && device.id) {
    formMode.value = 'edit'
    form.value = { ...device }
  } else {
    formMode.value = 'create'
    form.value = { name: '', model: '', deviceType: '', status: 'NORMAL', purchaseDate: '', location: '', hardwareSpecs: '' }
  }
  formVisible.value = true
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    if (formMode.value === 'create') {
      await createDevice(form.value)
      ElMessage.success('设备创建成功')
    } else {
      await updateDevice(form.value.id, form.value)
      ElMessage.success('设备更新成功')
    }
    formVisible.value = false
    fetchDevices()
  } catch (e) {
    if (e !== false) ElMessage.error('操作失败')
  }
}

const handleDelete = async (device) => {
  try {
    await ElMessageBox.confirm(`确认删除设备「${device.name}」？`, '删除确认', { type: 'warning' })
    await deleteDevice(device.id)
    ElMessage.success('删除成功')
    fetchDevices()
  } catch (e) { /* cancelled */ }
}

const openDetail = async (device) => {
  currentDevice.value = device
  detailVisible.value = true
  detailTab.value = 'overview'
  detailLoading.value = true
  try {
    const [usage, repair, maintenance, usageStats] = await Promise.all([
      getUsageByDevice(device.id).catch(() => []),
      getRepairByDevice(device.id).catch(() => []),
      getMaintenanceByDevice(device.id).catch(() => []),
      getUsageStats(device.id).catch(() => null)
    ])
    detailUsageList.value = Array.isArray(usage) ? usage : []
    detailRepairList.value = Array.isArray(repair) ? repair : []
    detailMaintenanceList.value = Array.isArray(maintenance) ? maintenance : []
    calcOverviewStats(detailUsageList.value, detailRepairList.value, detailMaintenanceList.value, usageStats)
  } catch (e) {
    ElMessage.error('获取详情数据失败')
  } finally {
    detailLoading.value = false
  }
}

const handleBatchUpdate = async (data) => {
  try {
    await batchUpdateDevices(data)
    ElMessage.success('批量更新成功')
    batchEditVisible.value = false
    fetchDevices()
  } catch (e) {
    ElMessage.error('批量更新失败')
  }
}

onMounted(fetchDevices)
</script>

<style scoped>
.device-list { padding: 0; }
.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}
.el-col { margin-bottom: 16px; }

.overview-stats { margin-bottom: 20px; }
.overview-card {
  background: linear-gradient(135deg, #f8faff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  transition: all 0.3s;
}
.overview-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
.overview-card-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.overview-card-value {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
  line-height: 1.3;
  word-break: break-all;
}
.overview-blue { color: #409EFF; }
.overview-red { color: #F56C6C; }
.overview-green { color: #67C23A; }
.overview-card-sub {
  font-size: 12px;
  color: #c0c4cc;
}

.timeline-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 3px solid #409EFF;
}
.timeline-card {
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 4px;
}
.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.timeline-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.timeline-content {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}
:deep(.el-timeline-item__timestamp) {
  font-size: 12px;
  color: #909399;
}
</style>
