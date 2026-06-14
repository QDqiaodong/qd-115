<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('close')" title="批量编辑设备" width="700px" @close="$emit('close')" destroy-on-close>
    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      勾选需要修改的设备，统一设置「摆放位置」和「设备状态」，未选择的字段将保持原值。
    </el-alert>
    <div class="batch-fields" style="margin-bottom: 16px; display: flex; gap: 12px;">
      <el-input v-model="batchLocation" placeholder="批量设置摆放位置（留空不变）" style="flex: 1" clearable />
      <el-select v-model="batchStatus" placeholder="批量设置设备状态（留空不变）" style="width: 180px" clearable>
        <el-option label="正常" value="NORMAL" />
        <el-option label="故障" value="FAULTY" />
        <el-option label="维修中" value="MAINTENANCE" />
        <el-option label="退役" value="RETIRED" />
      </el-select>
      <el-button type="primary" :disabled="!hasSelection" @click="applyBatch">批量应用</el-button>
    </div>
    <el-table ref="tableRef" :data="localDevices" @selection-change="onSelectionChange" height="400">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="name" label="设备名称" width="140" />
      <el-table-column prop="model" label="型号" width="160" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ typeMap[row.deviceType] || row.deviceType }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTypeMap[row.status]" size="small">{{ typeMapStatus[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="摆放位置" min-width="140" />
    </el-table>
    <template #footer>
      <el-button @click="$emit('close')">取消</el-button>
      <el-button type="primary" :disabled="!hasSelection" @click="confirm">确认提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: { type: Boolean, default: false },
  devices: { type: Array, default: () => [] }
})

const emit = defineEmits(['update', 'close'])

const tableRef = ref(null)
const selection = ref([])
const batchLocation = ref('')
const batchStatus = ref('')
const localDevices = ref([])

const typeMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const typeMapStatus = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '维修中', RETIRED: '退役' }
const statusTypeMap = { NORMAL: 'success', FAULTY: 'danger', MAINTENANCE: 'warning', RETIRED: 'info' }

const hasSelection = computed(() => selection.value.length > 0)

const onSelectionChange = (rows) => {
  selection.value = rows
}

const applyBatch = () => {
  if (!hasSelection.value) {
    ElMessage.warning('请先勾选设备')
    return
  }
  let count = 0
  selection.value.forEach(d => {
    const target = localDevices.value.find(item => item.id === d.id)
    if (target) {
      if (batchLocation.value) {
        target.location = batchLocation.value
        count++
      }
      if (batchStatus.value) {
        target.status = batchStatus.value
        count++
      }
    }
  })
  ElMessage.success(`已批量应用 ${count} 项修改，点击确认提交保存`)
}

const confirm = () => {
  if (!hasSelection.value) {
    ElMessage.warning('请先勾选设备')
    return
  }
  const updatedIds = selection.value.map(d => d.id)
  const updatedList = localDevices.value.filter(d => updatedIds.includes(d.id))
  emit('update', updatedList)
}

watch(() => props.visible, (v) => {
  if (v) {
    localDevices.value = JSON.parse(JSON.stringify(props.devices))
    selection.value = []
    batchLocation.value = ''
    batchStatus.value = ''
    nextTick(() => tableRef.value?.clearSelection())
  }
})
</script>
