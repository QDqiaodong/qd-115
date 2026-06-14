<template>
  <div class="maintenance-record">
    <div class="top-bar">
      <el-select v-model="selectedDeviceId" placeholder="选择设备" clearable filterable style="width: 240px" @change="fetchData">
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
          <el-select v-model="form.deviceId" placeholder="选择设备" style="width: 100%" :disabled="formMode === 'edit'">
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
import { Plus } from '@element-plus/icons-vue'
import { getDevices, getMaintenanceByDevice, createMaintenance, updateMaintenance, deleteMaintenance } from '../api'

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

const fetchData = async () => {
  if (!selectedDeviceId.value) {
    maintenanceList.value = []
    return
  }
  loading.value = true
  try {
    const res = await getMaintenanceByDevice(selectedDeviceId.value)
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
      maintenanceTime: row.maintenanceTime,
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
</style>
