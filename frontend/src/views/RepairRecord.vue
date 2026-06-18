<template>
  <div class="repair-record">
    <div class="top-bar">
      <el-select v-model="selectedDeviceId" placeholder="选择设备" clearable filterable style="width: 240px" @change="fetchData">
        <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-tag v-if="currentDeviceStatus" :type="statusTagType(currentDeviceStatus)" size="large" effect="dark">当前状态：{{ statusLabel(currentDeviceStatus) }}</el-tag>
      <div style="flex: 1" />
      <el-button :type="viewMode === 'table' ? 'primary' : ''" @click="viewMode = 'table'">列表视图</el-button>
      <el-button :type="viewMode === 'timeline' ? 'primary' : ''" @click="viewMode = 'timeline'">诊断时间轴</el-button>
      <el-button type="primary" :icon="Plus" @click="openForm" :disabled="!selectedDeviceId">新增检修记录</el-button>
    </div>

    <template v-if="viewMode === 'table'">
      <el-card shadow="never">
        <el-table :data="repairList" v-loading="loading" stripe border>
          <el-table-column prop="repairTime" label="检修时间" width="180" fixed="left" />
          <el-table-column label="设备名称" width="140" fixed="left">
            <template #default="{ row }">{{ row.device?.name }}</template>
          </el-table-column>
          <el-table-column prop="symptom" label="异常现象" show-overflow-tooltip min-width="180" />
          <el-table-column label="维修详情" align="center">
            <el-table-column prop="cause" label="故障原因" show-overflow-tooltip min-width="150" />
            <el-table-column prop="fixMethod" label="修复方式" show-overflow-tooltip min-width="150" />
            <el-table-column label="修复结果" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.fixResult" :type="fixResultTagType(row.fixResult)" size="small">
                  {{ fixResultLabel(row.fixResult) }}
                </el-tag>
                <span v-else class="text-muted">未填写</span>
              </template>
            </el-table-column>
            <el-table-column prop="repairPerson" label="维修人员" width="100" />
            <el-table-column label="费用" width="120" align="right">
              <template #default="{ row }">
                <span class="cost-amount">¥{{ row.cost != null ? Number(row.cost).toFixed(2) : '0.00' }}</span>
              </template>
            </el-table-column>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
    <template v-else>
      <el-card shadow="never">
        <RepairTimeline :device-id="selectedDeviceId" />
      </el-card>
    </template>

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新增检修记录' : '编辑检修记录'" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="设备" prop="deviceId">
          <el-select v-model="form.deviceId" placeholder="选择设备" style="width: 100%" :disabled="formMode === 'edit'">
            <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="检修时间" prop="repairTime">
          <el-date-picker v-model="form.repairTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择检修时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="异常现象" prop="symptom">
          <el-input v-model="form.symptom" type="textarea" :rows="2" placeholder="请描述异常现象" />
        </el-form-item>
        <el-form-item label="故障原因" prop="cause">
          <el-input v-model="form.cause" type="textarea" :rows="2" placeholder="请描述故障原因" />
        </el-form-item>
        <el-form-item label="修复方式" prop="fixMethod">
          <el-input v-model="form.fixMethod" type="textarea" :rows="2" placeholder="请描述修复方式" />
        </el-form-item>
        <el-form-item label="修复结果" prop="fixResult">
          <el-select v-model="form.fixResult" placeholder="请选择修复结果" style="width: 100%" clearable>
            <el-option label="已修复" value="FIXED" />
            <el-option label="部分修复" value="PARTIAL" />
            <el-option label="无法修复" value="UNFIXED" />
          </el-select>
          <div class="form-hint">选择后将自动更新设备状态</div>
        </el-form-item>
        <el-form-item label="检修人" prop="repairPerson">
          <el-input v-model="form.repairPerson" placeholder="请输入检修人姓名" />
        </el-form-item>
        <el-form-item label="费用(元)" prop="cost">
          <el-input-number v-model="form.cost" :min="0" :precision="2" controls-position="right" style="width: 100%" />
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
import { getDevices, getRepairByDevice, createRepair, updateRepair, deleteRepair } from '../api'
import RepairTimeline from '../components/RepairTimeline.vue'

const deviceList = ref([])
const selectedDeviceId = ref('')
const repairList = ref([])
const loading = ref(false)
const viewMode = ref('table')

const currentDeviceStatus = computed(() => {
  const d = deviceList.value.find(item => item.id === selectedDeviceId.value)
  return d?.status || ''
})

const statusMap = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '保养中', RETIRED: '退役' }

const statusLabel = (s) => statusMap[s] || s

const statusTagType = (status) => {
  const map = { NORMAL: 'success', FAULTY: 'danger', MAINTENANCE: 'warning', RETIRED: 'info' }
  return map[status] || 'info'
}

const fixResultLabelMap = { FIXED: '已修复', PARTIAL: '部分修复', UNFIXED: '无法修复' }
const fixResultLabel = (s) => fixResultLabelMap[s] || s
const fixResultTagType = (s) => {
  const map = { FIXED: 'success', PARTIAL: 'warning', UNFIXED: 'danger' }
  return map[s] || 'info'
}

const formVisible = ref(false)
const formMode = ref('create')
const formRef = ref(null)
const form = ref({
  deviceId: '', repairTime: '', symptom: '', cause: '', fixMethod: '', fixResult: '', repairPerson: '', cost: 0, remark: ''
})

const formRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  repairTime: [{ required: true, message: '请选择检修时间', trigger: 'change' }],
  symptom: [{ required: true, message: '请描述异常现象', trigger: 'blur' }]
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
    repairList.value = []
    return
  }
  loading.value = true
  try {
    const res = await getRepairByDevice(selectedDeviceId.value)
    repairList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取检修记录失败')
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
      repairTime: formatDateTime(row.repairTime),
      symptom: row.symptom,
      cause: row.cause,
      fixMethod: row.fixMethod,
      fixResult: row.fixResult || '',
      repairPerson: row.repairPerson,
      cost: row.cost || 0,
      remark: row.remark
    }
  } else {
    formMode.value = 'create'
    form.value = {
      deviceId: selectedDeviceId.value,
      repairTime: '',
      symptom: '',
      cause: '',
      fixMethod: '',
      fixResult: '',
      repairPerson: '',
      cost: 0,
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
      repairTime: form.value.repairTime,
      symptom: form.value.symptom,
      cause: form.value.cause,
      fixMethod: form.value.fixMethod,
      fixResult: form.value.fixResult || null,
      repairPerson: form.value.repairPerson,
      cost: form.value.cost,
      remark: form.value.remark
    }
    if (formMode.value === 'create') {
      await createRepair(payload)
      ElMessage.success('记录创建成功')
    } else {
      await updateRepair(form.value.id, payload)
      ElMessage.success('记录更新成功')
    }
    formVisible.value = false
    fetchData()
    fetchDevices()
  } catch (e) {
    if (e !== false) ElMessage.error('操作失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该检修记录？', '删除确认', { type: 'warning' })
    await deleteRepair(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancelled */ }
}

onMounted(async () => {
  await fetchDevices()
})
</script>

<style scoped>
.repair-record { padding: 0; }
.top-bar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px;
}
.cost-amount {
  font-weight: 600;
  color: #f56c6c;
}
.text-muted {
  color: #c0c4cc;
  font-size: 12px;
}
.form-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
:deep(.el-table .el-table__group-title) {
  font-weight: 600;
  background: #f5f7fa !important;
}
</style>
