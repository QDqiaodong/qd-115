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
  getUsageByDevice, getRepairByDevice, getMaintenanceByDevice
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
const detailTab = ref('usage')
const detailUsageList = ref([])
const detailRepairList = ref([])
const detailMaintenanceList = ref([])
const detailLoading = ref(false)

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
  detailTab.value = 'usage'
  detailLoading.value = true
  try {
    const [usage, repair, maintenance] = await Promise.all([
      getUsageByDevice(device.id).catch(() => []),
      getRepairByDevice(device.id).catch(() => []),
      getMaintenanceByDevice(device.id).catch(() => [])
    ])
    detailUsageList.value = Array.isArray(usage) ? usage : []
    detailRepairList.value = Array.isArray(repair) ? repair : []
    detailMaintenanceList.value = Array.isArray(maintenance) ? maintenance : []
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
</style>
