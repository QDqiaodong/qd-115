<template>
  <div class="usage-record">
    <div class="top-bar">
      <el-select v-model="selectedDeviceId" placeholder="选择设备" clearable filterable style="width: 240px" @change="fetchData">
        <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-button type="primary" :icon="Plus" @click="openForm" :disabled="!selectedDeviceId">新增记录</el-button>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.totalDuration }}</div>
          <div class="stat-label">累计使用时长</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.monthDuration }}</div>
          <div class="stat-label">本月使用时长</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stats.avgDuration }}</div>
          <div class="stat-label">平均单次时长</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <el-table :data="usageList" v-loading="loading" stripe>
        <el-table-column prop="usageDate" label="日期" width="120" />
        <el-table-column label="设备名称" width="140">
          <template #default="{ row }">{{ row.device?.name }}</template>
        </el-table-column>
        <el-table-column label="使用时长" width="120">
          <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
        </el-table-column>
        <el-table-column prop="scenario" label="使用场景" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新增使用记录' : '编辑使用记录'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="设备" prop="deviceId">
          <el-select v-model="form.deviceId" placeholder="选择设备" style="width: 100%" :disabled="formMode === 'edit'">
            <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="usageDate">
          <el-date-picker v-model="form.usageDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="使用时长" required>
          <div style="display: flex; gap: 8px; width: 100%">
            <el-input-number v-model="form.hours" :min="0" :max="24" controls-position="right" placeholder="小时" style="flex: 1" />
            <el-input-number v-model="form.minutes" :min="0" :max="59" controls-position="right" placeholder="分钟" style="flex: 1" />
          </div>
        </el-form-item>
        <el-form-item label="使用场景" prop="scenario">
          <el-select v-model="form.scenario" placeholder="选择场景" style="width: 100%">
            <el-option label="观影" value="观影" />
            <el-option label="音乐" value="音乐" />
            <el-option label="游戏" value="游戏" />
            <el-option label="其他" value="其他" />
          </el-select>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getDevices, getUsageByDevice, createUsage, updateUsage, deleteUsage, getUsageStats } from '../api'

const deviceList = ref([])
const selectedDeviceId = ref('')
const usageList = ref([])
const loading = ref(false)

const stats = reactive({ totalDuration: '0时0分', monthDuration: '0时0分', avgDuration: '0分' })

const formVisible = ref(false)
const formMode = ref('create')
const formRef = ref(null)
const form = ref({
  deviceId: '', usageDate: '', hours: 0, minutes: 0, scenario: '', remark: ''
})

const formRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  usageDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  scenario: [{ required: true, message: '请选择场景', trigger: 'change' }]
}

const formatDuration = (minutes) => {
  if (!minutes && minutes !== 0) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}时${m}分` : `${m}分`
}

const formatStatsDuration = (minutes) => {
  if (!minutes && minutes !== 0) return '0时0分'
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  return h > 0 ? `${h}时${m}分` : `${m}分`
}

const fetchDevices = async () => {
  try {
    const res = await getDevices()
    deviceList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取设备列表失败')
  }
}

const fetchData = async () => {
  if (!selectedDeviceId.value) {
    usageList.value = []
    stats.totalDuration = '0时0分'
    stats.monthDuration = '0时0分'
    stats.avgDuration = '0分'
    return
  }
  loading.value = true
  try {
    const [usageRes, statsRes] = await Promise.all([
      getUsageByDevice(selectedDeviceId.value),
      getUsageStats(selectedDeviceId.value).catch(() => null)
    ])
    usageList.value = Array.isArray(usageRes) ? usageRes : []
    if (statsRes) {
      const s = statsRes
      stats.totalDuration = formatStatsDuration(s.totalMinutes)
      stats.monthDuration = formatStatsDuration(s.monthlyMinutes)
      stats.avgDuration = formatStatsDuration(s.avgMinutes)
    }
  } catch (e) {
    ElMessage.error('获取使用记录失败')
  } finally {
    loading.value = false
  }
}

const openForm = (row = null) => {
  if (row && row.id) {
    formMode.value = 'edit'
    form.value = {
      id: row.id,
      deviceId: row.device?.id || selectedDeviceId.value,
      usageDate: row.usageDate,
      hours: Math.floor(row.durationMinutes / 60),
      minutes: row.durationMinutes % 60,
      scenario: row.scenario,
      remark: row.remark
    }
  } else {
    formMode.value = 'create'
    form.value = {
      deviceId: selectedDeviceId.value,
      usageDate: '',
      hours: 0,
      minutes: 0,
      scenario: '',
      remark: ''
    }
  }
  formVisible.value = true
}

const submitForm = async () => {
  const payload = {
    deviceId: form.value.deviceId,
    usageDate: form.value.usageDate,
    durationMinutes: (Number(form.value.hours) || 0) * 60 + (Number(form.value.minutes) || 0),
    scenario: form.value.scenario,
    remark: form.value.remark
  }
  if (payload.durationMinutes <= 0) {
    ElMessage.warning('请输入使用时长')
    return
  }
  try {
    await formRef.value.validate()
    if (formMode.value === 'create') {
      await createUsage(payload)
      ElMessage.success('记录创建成功')
    } else {
      await updateUsage(form.value.id, payload)
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
    await ElMessageBox.confirm('确认删除该使用记录？', '删除确认', { type: 'warning' })
    await deleteUsage(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancelled */ }
}

onMounted(async () => {
  await fetchDevices()
})
</script>

<style scoped>
.usage-record { padding: 0; }
.top-bar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px;
}
.stats-row { margin-bottom: 20px; }
.stat-card { text-align: center; }
.stat-value {
  font-size: 28px; font-weight: 700; color: #409EFF;
  margin-bottom: 6px;
}
.stat-label {
  font-size: 13px; color: #909399;
}
</style>
