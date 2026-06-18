<template>
  <div class="maintenance-record">
    <div class="top-bar">
      <el-select v-model="selectedDeviceId" placeholder="选择设备（留空查看全部）" clearable filterable style="width: 240px" @change="onDeviceChange">
        <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <div style="flex: 1" />
      <el-button type="primary" :icon="Plus" @click="openForm" :disabled="!selectedDeviceId">新增养护记录</el-button>
    </div>

    <el-tabs v-model="activeType" class="type-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="机身清洁" name="CLEANING" />
      <el-tab-pane label="线路整理" name="CABLE" />
      <el-tab-pane label="固件升级" name="FIRMWARE" />
      <el-tab-pane label="其他" name="OTHER" />
    </el-tabs>

    <el-card shadow="never" v-if="nextMaintenanceWindows" class="next-window-card">
      <div class="card-title-row">
        <span class="card-title">下次保养窗口</span>
        <el-tag v-if="earliestUrgentWindow" :type="earliestUrgentWindow.overdue ? 'danger' : 'warning'" size="small" effect="dark">
          <el-icon :size="12"><Warning /></el-icon>
          {{ earliestUrgentWindow.overdue ? '逾期' + Math.abs(earliestUrgentWindow.daysUntil) + '天' : earliestUrgentWindow.maintenanceTypeLabel + '即将到期' }}
        </el-tag>
      </div>
      <el-row :gutter="12" class="window-row">
        <el-col :span="6" v-for="w in Object.values(nextMaintenanceWindows)" :key="w.maintenanceType">
          <div class="mt-window-card" :class="{ overdue: w.overdue, urgent: w.urgent }">
            <div class="mt-window-type">{{ w.maintenanceTypeLabel }}</div>
            <div class="mt-window-interval">每 {{ w.intervalDays }} 天</div>
            <div class="mt-window-next" v-if="w.nextTime">{{ w.nextTime }}</div>
            <div class="mt-window-next no-data" v-else>暂无记录</div>
            <div class="mt-window-status" v-if="w.nextTime">
              <el-tag v-if="w.overdue" type="danger" size="small">逾期{{ Math.abs(w.daysUntil) }}天</el-tag>
              <el-tag v-else-if="w.urgent" type="warning" size="small">即将到期</el-tag>
              <el-tag v-else type="success" size="small">剩余{{ w.daysUntil }}天</el-tag>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <el-table :data="filteredList" v-loading="loading" stripe>
        <el-table-column prop="maintenanceTime" label="养护时间" width="180" />
        <el-table-column label="设备名称" width="140">
          <template #default="{ row }">{{ row.device?.name }}</template>
        </el-table-column>
        <el-table-column label="养护类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagColor(row.maintenanceType)" size="small" effect="light">{{ typeLabel(row.maintenanceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="养护内容" show-overflow-tooltip min-width="200" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新增养护记录' : '编辑养护记录'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="设备" prop="deviceId">
          <el-select v-model="form.deviceId" placeholder="选择设备" style="width: 100%">
            <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="养护时间" prop="maintenanceTime">
          <el-date-picker v-model="form.maintenanceTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择养护时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="养护类型" prop="maintenanceType">
          <el-select v-model="form.maintenanceType" placeholder="选择养护类型" style="width: 100%">
            <el-option label="机身清洁" value="CLEANING" />
            <el-option label="线路整理" value="CABLE" />
            <el-option label="固件升级" value="FIRMWARE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="养护内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请描述养护内容" />
        </el-form-item>
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="form.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Warning } from '@element-plus/icons-vue'
import { getDevices, getAllMaintenance, createMaintenance, updateMaintenance, deleteMaintenance } from '../api'
import { calculateNextWindows, getEarliestUrgentWindow } from '../utils/maintenanceInterval'

const deviceList = ref([])
const selectedDeviceId = ref('')
const maintenanceList = ref([])
const loading = ref(false)
const activeType = ref('')

const mtLabelMap = { CLEANING: '机身清洁', CABLE: '线路整理', FIRMWARE: '固件升级', OTHER: '其他' }
const mtColorMap = { CLEANING: 'primary', CABLE: 'success', FIRMWARE: 'warning', OTHER: 'info' }

const typeLabel = (t) => mtLabelMap[t] || t
const typeTagColor = (t) => mtColorMap[t] || 'info'

const filteredList = computed(() => {
  if (!activeType.value) return maintenanceList.value
  return maintenanceList.value.filter(item => item.maintenanceType === activeType.value)
})

const selectedDevice = computed(() => {
  if (!selectedDeviceId.value) return null
  return deviceList.value.find(d => d.id === selectedDeviceId.value) || null
})

const nextMaintenanceWindows = computed(() => {
  if (!selectedDevice.value || maintenanceList.value.length === 0) return null
  const sorted = [...maintenanceList.value].sort((a, b) => new Date(b.maintenanceTime) - new Date(a.maintenanceTime))
  const lastTime = sorted[0].maintenanceTime
  return calculateNextWindows(selectedDevice.value.deviceType, lastTime)
})

const earliestUrgentWindow = computed(() => {
  return getEarliestUrgentWindow(nextMaintenanceWindows.value)
})

const formVisible = ref(false)
const formMode = ref('create')
const formRef = ref(null)
const form = ref({
  deviceId: '', maintenanceTime: '', maintenanceType: '', content: '', operator: '', remark: ''
})

const formRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  maintenanceTime: [{ required: true, message: '请选择养护时间', trigger: 'change' }],
  maintenanceType: [{ required: true, message: '请选择养护类型', trigger: 'change' }],
  content: [{ required: true, message: '请描述养护内容', trigger: 'blur' }]
}

const fetchDevices = async () => {
  try {
    const res = await getDevices()
    deviceList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取设备列表失败')
  }
}

const onDeviceChange = () => {
  activeType.value = ''
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const deviceId = selectedDeviceId.value || undefined
    const res = await getAllMaintenance(deviceId)
    maintenanceList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取养护记录失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  // computed handles filtering
}

const openForm = (row = null) => {
  if (row && row.id) {
    formMode.value = 'edit'
    form.value = {
      id: row.id,
      deviceId: row.device?.id || selectedDeviceId.value,
      maintenanceTime: formatDateTime(row.maintenanceTime),
      maintenanceType: row.maintenanceType,
      content: row.content,
      operator: row.operator,
      remark: row.remark
    }
  } else {
    formMode.value = 'create'
    form.value = {
      deviceId: selectedDeviceId.value,
      maintenanceTime: '',
      maintenanceType: '',
      content: '',
      operator: '',
      remark: ''
    }
  }
  formVisible.value = true
}

const formatDateTime = (value) => {
  if (!value) return ''
  if (value instanceof Date) {
    const pad = (n) => String(n).padStart(2, '0')
    return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())} ${pad(value.getHours())}:${pad(value.getMinutes())}:${pad(value.getSeconds())}`
  }
  if (typeof value === 'string') {
    if (value.includes('T')) {
      const d = new Date(value)
      if (!isNaN(d.getTime())) {
        const pad = (n) => String(n).padStart(2, '0')
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
      }
    }
    return value
  }
  return ''
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    const payload = {
      deviceId: form.value.deviceId,
      maintenanceTime: form.value.maintenanceTime,
      maintenanceType: form.value.maintenanceType,
      content: form.value.content,
      operator: form.value.operator,
      remark: form.value.remark
    }
    if (formMode.value === 'create') {
      await createMaintenance(payload)
      ElMessage.success('记录创建成功')
    } else {
      await updateMaintenance(form.value.id, payload)
      ElMessage.success('记录更新成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e) {
    if (e !== false) ElMessage.error('操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该养护记录？', '删除确认', { type: 'warning' })
    await deleteMaintenance(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancelled */ }
}

onMounted(async () => {
  await fetchDevices()
  fetchData()
})
</script>

<style scoped>
.maintenance-record { padding: 0; }
.top-bar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px;
}
.type-tabs {
  margin-bottom: 16px;
  background: #fff;
  padding: 0 16px;
  border-radius: 8px;
}
.next-window-card {
  margin-bottom: 16px;
}
.card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.window-row { margin-top: 4px; }
.mt-window-card {
  background: linear-gradient(135deg, #f8faff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  transition: all 0.3s;
}
.mt-window-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.mt-window-card.overdue {
  border-color: #fde2e2;
  background: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
}
.mt-window-card.urgent {
  border-color: #faecd8;
  background: linear-gradient(135deg, #fdf6ec 0%, #ffffff 100%);
}
.mt-window-type {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.mt-window-interval {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.mt-window-next {
  font-size: 12px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 6px;
}
.mt-window-next.no-data {
  color: #c0c4cc;
  font-weight: 400;
}
.mt-window-status { margin-top: 2px; }
</style>
