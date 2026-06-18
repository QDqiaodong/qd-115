<template>
  <div class="amplifier-calibration">
    <div class="top-bar">
      <el-select v-model="selectedAmplifierId" placeholder="选择功放设备" clearable filterable style="width: 240px" @change="fetchData">
        <el-option v-for="d in amplifierList" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <div style="flex: 1" />
      <el-button type="primary" :icon="Plus" @click="openForm" :disabled="!selectedAmplifierId">新增校准记录</el-button>
    </div>

    <el-card shadow="never" class="summary-card" v-if="selectedAmplifierId && latestCalibration">
      <div class="card-title-row">
        <span class="card-title">最新校准参数</span>
        <el-tag size="small" type="primary">{{ latestCalibration.calibrationDate }}</el-tag>
        <el-tag size="small" effect="plain" v-if="latestCalibration.calibrationMethod">
          {{ latestCalibration.calibrationMethod }}
        </el-tag>
      </div>
      <el-row :gutter="16">
        <el-col :span="6" v-for="channel in channelList" :key="channel">
          <div class="channel-card" :class="{ active: latestParams[channel] }">
            <div class="channel-name">{{ channel }}</div>
            <div class="channel-params" v-if="latestParams[channel]">
              <div class="param-item">
                <span class="param-label">音量基准</span>
                <span class="param-value">{{ latestParams[channel].volumeReference }} dB</span>
              </div>
              <div class="param-item">
                <span class="param-label">距离</span>
                <span class="param-value">{{ latestParams[channel].distance }} m</span>
              </div>
              <div class="param-item">
                <span class="param-label">分频点</span>
                <span class="param-value">{{ latestParams[channel].crossoverPoint }} Hz</span>
              </div>
            </div>
            <div class="no-data" v-else>暂无数据</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeChannel" class="channel-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部声道" name="" />
        <el-tab-pane v-for="channel in channelList" :key="channel" :label="channel" :name="channel" />
      </el-tabs>

      <el-table :data="filteredList" v-loading="loading" stripe>
        <el-table-column prop="calibrationDate" label="校准日期" width="120" />
        <el-table-column label="功放设备" width="160">
          <template #default="{ row }">{{ row.amplifier?.name }}</template>
        </el-table-column>
        <el-table-column prop="channelName" label="声道" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="primary" effect="light">{{ row.channelName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="volumeReference" label="音量基准(dB)" width="120" />
        <el-table-column prop="distance" label="距离(米)" width="100" />
        <el-table-column prop="crossoverPoint" label="分频点(Hz)" width="110" />
        <el-table-column prop="calibrationMethod" label="校准方式" width="120" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新增校准记录' : '编辑校准记录'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="功放设备" prop="amplifierId">
          <el-select v-model="form.amplifierId" placeholder="选择功放设备" style="width: 100%" filterable>
            <el-option v-for="d in amplifierList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="声道" prop="channelName">
          <el-select v-model="form.channelName" placeholder="选择声道" style="width: 100%" filterable allow-create>
            <el-option label="前置左声道" value="前置左声道" />
            <el-option label="前置右声道" value="前置右声道" />
            <el-option label="中置声道" value="中置声道" />
            <el-option label="环绕左声道" value="环绕左声道" />
            <el-option label="环绕右声道" value="环绕右声道" />
            <el-option label="后置左声道" value="后置左声道" />
            <el-option label="后置右声道" value="后置右声道" />
            <el-option label="低音炮.1" value="低音炮.1" />
            <el-option label="天空左声道" value="天空左声道" />
            <el-option label="天空右声道" value="天空右声道" />
          </el-select>
        </el-form-item>
        <el-form-item label="音量基准" prop="volumeReference">
          <el-input-number v-model="form.volumeReference" :min="-30" :max="0" :step="0.5" :precision="1" style="width: 100%" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">dB</span>
        </el-form-item>
        <el-form-item label="距离" prop="distance">
          <el-input-number v-model="form.distance" :min="0.1" :step="0.1" :precision="1" style="width: 100%" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">米</span>
        </el-form-item>
        <el-form-item label="分频点" prop="crossoverPoint">
          <el-input-number v-model="form.crossoverPoint" :min="40" :max="200" :step="10" style="width: 100%" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">Hz</span>
        </el-form-item>
        <el-form-item label="校准日期" prop="calibrationDate">
          <el-date-picker v-model="form.calibrationDate" type="date" value-format="YYYY-MM-DD" placeholder="选择校准日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="校准方式" prop="calibrationMethod">
          <el-select v-model="form.calibrationMethod" placeholder="选择校准方式" style="width: 100%" filterable allow-create clearable>
            <el-option label="Dirac Live" value="Dirac Live" />
            <el-option label="Audyssey" value="Audyssey" />
            <el-option label="YPAO" value="YPAO" />
            <el-option label="手动微调" value="手动微调" />
            <el-option label="自动校准" value="自动校准" />
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getDevicesByType, getAllAmplifierCalibrations, createAmplifierCalibration, updateAmplifierCalibration, deleteAmplifierCalibration } from '../api'

const amplifierList = ref([])
const selectedAmplifierId = ref('')
const calibrationList = ref([])
const loading = ref(false)
const activeChannel = ref('')

const channelList = computed(() => {
  const channels = new Set()
  calibrationList.value.forEach(item => channels.add(item.channelName))
  return Array.from(channels)
})

const latestParams = computed(() => {
  const params = {}
  const channelMap = {}
  calibrationList.value.forEach(item => {
    if (!channelMap[item.channelName] || new Date(item.calibrationDate) > new Date(channelMap[item.channelName].calibrationDate)) {
      channelMap[item.channelName] = item
    }
  })
  Object.keys(channelMap).forEach(channel => {
    params[channel] = channelMap[channel]
  })
  return params
})

const latestCalibration = computed(() => {
  if (calibrationList.value.length === 0) return null
  const sorted = [...calibrationList.value].sort((a, b) => new Date(b.calibrationDate) - new Date(a.calibrationDate))
  return sorted[0]
})

const filteredList = computed(() => {
  if (!activeChannel.value) return calibrationList.value
  return calibrationList.value.filter(item => item.channelName === activeChannel.value)
})

const formVisible = ref(false)
const formMode = ref('create')
const formRef = ref(null)
const form = ref({
  amplifierId: '',
  channelName: '',
  volumeReference: 75,
  distance: 3.0,
  crossoverPoint: 80,
  calibrationDate: '',
  calibrationMethod: '',
  remark: ''
})

const formRules = {
  amplifierId: [{ required: true, message: '请选择功放设备', trigger: 'change' }],
  channelName: [{ required: true, message: '请选择声道', trigger: 'change' }],
  volumeReference: [{ required: true, message: '请输入音量基准', trigger: 'blur' }],
  distance: [{ required: true, message: '请输入距离', trigger: 'blur' }],
  crossoverPoint: [{ required: true, message: '请输入分频点', trigger: 'blur' }],
  calibrationDate: [{ required: true, message: '请选择校准日期', trigger: 'change' }]
}

const fetchAmplifiers = async () => {
  try {
    const res = await getDevicesByType('AMPLIFIER')
    amplifierList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取功放设备列表失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const amplifierId = selectedAmplifierId.value || undefined
    const res = await getAllAmplifierCalibrations(amplifierId)
    calibrationList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取校准记录失败')
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
      amplifierId: row.amplifier?.id || selectedAmplifierId.value,
      channelName: row.channelName,
      volumeReference: row.volumeReference,
      distance: row.distance,
      crossoverPoint: row.crossoverPoint,
      calibrationDate: row.calibrationDate,
      calibrationMethod: row.calibrationMethod || '',
      remark: row.remark || ''
    }
  } else {
    formMode.value = 'create'
    form.value = {
      amplifierId: selectedAmplifierId.value,
      channelName: '',
      volumeReference: 75,
      distance: 3.0,
      crossoverPoint: 80,
      calibrationDate: new Date().toISOString().split('T')[0],
      calibrationMethod: '',
      remark: ''
    }
  }
  formVisible.value = true
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    const payload = { ...form.value }
    if (formMode.value === 'create') {
      await createAmplifierCalibration(payload)
      ElMessage.success('校准记录创建成功')
    } else {
      await updateAmplifierCalibration(form.value.id, payload)
      ElMessage.success('校准记录更新成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e) {
    if (e !== false) {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该校准记录？', '删除确认', { type: 'warning' })
    await deleteAmplifierCalibration(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancelled */ }
}

onMounted(async () => {
  await fetchAmplifiers()
  fetchData()
})
</script>

<style scoped>
.amplifier-calibration { padding: 0; }
.top-bar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px;
}
.summary-card {
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
.channel-card {
  background: linear-gradient(135deg, #f8faff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
  transition: all 0.3s;
}
.channel-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.channel-card.active {
  border-color: #409EFF;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
}
.channel-name {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ebeef5;
}
.channel-params {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.param-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.param-label {
  font-size: 12px;
  color: #909399;
}
.param-value {
  font-size: 13px;
  font-weight: 600;
  color: #409EFF;
}
.no-data {
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  padding: 10px 0;
}
.channel-tabs {
  margin-bottom: 16px;
}
</style>
