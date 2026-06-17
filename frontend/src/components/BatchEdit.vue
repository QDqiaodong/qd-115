<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('close')" title="批量编辑设备" width="700px" @close="$emit('close')" destroy-on-close>
    <el-alert type="info" :closable="false" style="margin-bottom: 16px">
      勾选需要修改的设备，统一设置「摆放位置」和「设备状态」，未选择的字段将保持原值。
    </el-alert>
    <div class="batch-fields" style="margin-bottom: 16px; display: flex; gap: 12px;">
      <el-select v-model="batchLocation" placeholder="批量设置摆放位置（留空不变）" style="flex: 1" clearable filterable allow-create default-first-option>
        <el-option v-for="loc in locationOptions" :key="loc" :label="loc" :value="loc" />
      </el-select>
      <el-select v-model="batchStatus" placeholder="批量设置设备状态（留空不变）" style="width: 180px" clearable>
        <el-option label="正常" value="NORMAL" />
        <el-option label="故障" value="FAULTY" />
        <el-option label="保养中" value="MAINTENANCE" />
        <el-option label="退役" value="RETIRED" />
      </el-select>
      <el-button type="primary" :disabled="!canApply" @click="applyBatch">批量应用</el-button>
    </div>
    <el-alert v-if="retiredInSelection" type="warning" :closable="false" style="margin-bottom: 12px">
      选中的设备中包含退役设备，退役设备的状态不会被变更
    </el-alert>
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
      <el-button type="primary" :disabled="!hasAppliedChanges" @click="openPreview">确认提交</el-button>
    </template>

    <el-dialog v-model="previewVisible" title="批量变更预览" width="720px" destroy-on-close append-to-body>
      <div class="preview-summary">
        <el-row :gutter="12">
          <el-col :span="8">
            <div class="summary-card total">
              <div class="summary-label">选中设备</div>
              <div class="summary-value">{{ previewStats.total }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-card changed">
              <div class="summary-label">将变更</div>
              <div class="summary-value">{{ previewStats.changed }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="summary-card skipped">
              <div class="summary-label">跳过</div>
              <div class="summary-value">{{ previewStats.skipped }}</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="preview-detail">
        <div class="detail-title">变更明细</div>
        <el-table :data="previewList" size="small" max-height="380">
          <el-table-column prop="name" label="设备名称" width="120" show-overflow-tooltip />
          <el-table-column prop="model" label="型号" width="130" show-overflow-tooltip />
          <el-table-column label="摆放位置" min-width="160">
            <template #default="{ row }">
              <div class="compare-row">
                <span class="old-value" :class="{ 'no-change': !row.locationChanged }">
                  {{ row.originalLocation || '—' }}
                </span>
                <el-icon v-if="row.locationChanged" class="arrow-icon"><ArrowRight /></el-icon>
                <span v-if="row.locationChanged" class="new-value">
                  {{ row.newLocation || '—' }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="设备状态" min-width="160">
            <template #default="{ row }">
              <div class="compare-row">
                <el-tag v-if="!row.statusChanged" :type="statusTypeMap[row.originalStatus]" size="small">
                  {{ typeMapStatus[row.originalStatus] }}
                </el-tag>
                <template v-else>
                  <el-tag :type="statusTypeMap[row.originalStatus]" size="small" effect="plain">
                    {{ typeMapStatus[row.originalStatus] }}
                  </el-tag>
                  <el-icon class="arrow-icon"><ArrowRight /></el-icon>
                  <el-tag :type="statusTypeMap[row.newStatus]" size="small">
                    {{ typeMapStatus[row.newStatus] }}
                  </el-tag>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.skipped" type="info" size="small">跳过</el-tag>
              <el-tag v-else-if="row.changed" type="success" size="small">变更</el-tag>
              <el-tag v-else type="info" size="small">不变</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="previewStats.skipped > 0" class="skip-reason">
        <el-icon><InfoFilled /></el-icon>
        <span>共 {{ previewStats.skipped }} 台设备状态无法变更（退役设备不可恢复，或状态转换不允许）</span>
      </div>

      <template #footer>
        <el-button @click="previewVisible = false">返回修改</el-button>
        <el-button type="primary" :disabled="previewStats.changed === 0" @click="confirmSubmit">确认提交</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, InfoFilled } from '@element-plus/icons-vue'
import { STANDARD_LOCATIONS } from '../utils/location'
import { getStandardLocations } from '../api'

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
const originalDevices = ref([])
const previewVisible = ref(false)
const hasAppliedChanges = ref(false)
const locationOptions = ref([...STANDARD_LOCATIONS])

const fetchStandardLocations = async () => {
  try {
    const res = await getStandardLocations()
    if (Array.isArray(res) && res.length > 0) {
      locationOptions.value = res
    }
  } catch (e) {
    locationOptions.value = [...STANDARD_LOCATIONS]
  }
}

onMounted(fetchStandardLocations)

const typeMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const typeMapStatus = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '保养中', RETIRED: '退役' }
const statusTypeMap = { NORMAL: 'success', FAULTY: 'danger', MAINTENANCE: 'warning', RETIRED: 'info' }

const TRANSITION_RULES = {
  NORMAL: ['FAULTY', 'MAINTENANCE', 'RETIRED'],
  FAULTY: ['NORMAL', 'MAINTENANCE', 'RETIRED'],
  MAINTENANCE: ['NORMAL', 'FAULTY', 'RETIRED'],
  RETIRED: []
}

const hasSelection = computed(() => selection.value.length > 0)

const canApply = computed(() => hasSelection.value && (batchLocation.value || batchStatus.value))

const retiredInSelection = computed(() => {
  return selection.value.some(d => d.status === 'RETIRED')
})

const previewList = computed(() => {
  return selection.value.map(device => {
    const original = originalDevices.value.find(d => d.id === device.id) || device
    const locationChanged = batchLocation.value && original.location !== batchLocation.value
    const newLocation = locationChanged ? batchLocation.value : original.location

    let statusChanged = false
    let newStatus = original.status
    let skipped = false

    if (batchStatus.value) {
      if (original.status === 'RETIRED') {
        skipped = true
      } else {
        const allowed = TRANSITION_RULES[original.status] || []
        if (allowed.includes(batchStatus.value) && original.status !== batchStatus.value) {
          statusChanged = true
          newStatus = batchStatus.value
        } else if (original.status !== batchStatus.value) {
          skipped = true
        }
      }
    }

    const changed = locationChanged || statusChanged

    return {
      id: device.id,
      name: device.name,
      model: device.model,
      originalLocation: original.location,
      newLocation,
      locationChanged,
      originalStatus: original.status,
      newStatus,
      statusChanged,
      changed,
      skipped
    }
  })
})

const previewStats = computed(() => {
  const total = previewList.value.length
  let changed = 0
  let skipped = 0
  previewList.value.forEach(item => {
    if (item.skipped) skipped++
    else if (item.changed) changed++
  })
  return { total, changed, skipped }
})

const onSelectionChange = (rows) => {
  selection.value = rows
}

const applyBatch = () => {
  if (!canApply.value) {
    ElMessage.warning('请先勾选设备并设置要修改的字段')
    return
  }
  let count = 0
  let skippedRetired = 0
  selection.value.forEach(d => {
    const target = localDevices.value.find(item => item.id === d.id)
    if (target) {
      if (batchLocation.value) {
        target.location = batchLocation.value
        count++
      }
      if (batchStatus.value) {
        if (target.status === 'RETIRED') {
          skippedRetired++
        } else {
          const allowed = TRANSITION_RULES[target.status] || []
          if (allowed.includes(batchStatus.value)) {
            target.status = batchStatus.value
            count++
          } else {
            skippedRetired++
          }
        }
      }
    }
  })
  hasAppliedChanges.value = true
  let msg = `已批量应用 ${count} 项修改，点击确认提交保存`
  if (skippedRetired > 0) {
    msg += `（${skippedRetired} 台设备状态不可变更，已跳过）`
  }
  ElMessage.success(msg)
}

const openPreview = () => {
  if (!hasAppliedChanges.value) {
    ElMessage.warning('请先点击「批量应用」预览修改效果')
    return
  }
  previewVisible.value = true
}

const confirmSubmit = () => {
  const updatedIds = selection.value.map(d => d.id)
  const updatedList = localDevices.value.filter(d => updatedIds.includes(d.id))
  previewVisible.value = false
  emit('update', updatedList)
}

watch(() => props.visible, (v) => {
  if (v) {
    localDevices.value = JSON.parse(JSON.stringify(props.devices))
    originalDevices.value = JSON.parse(JSON.stringify(props.devices))
    selection.value = []
    batchLocation.value = ''
    batchStatus.value = ''
    hasAppliedChanges.value = false
    previewVisible.value = false
    nextTick(() => tableRef.value?.clearSelection())
  }
})
</script>

<style scoped>
.preview-summary {
  margin-bottom: 16px;
}

.summary-card {
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  border: 1px solid #ebeef5;
}

.summary-card.total {
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  border-color: #d9ecff;
}

.summary-card.changed {
  background: linear-gradient(135deg, #f0f9eb 0%, #ffffff 100%);
  border-color: #e1f3d8;
}

.summary-card.skipped {
  background: linear-gradient(135deg, #fdf6ec 0%, #ffffff 100%);
  border-color: #faecd8;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.summary-card.total .summary-value {
  color: #409EFF;
}

.summary-card.changed .summary-value {
  color: #67C23A;
}

.summary-card.skipped .summary-value {
  color: #E6A23C;
}

.detail-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #409EFF;
}

.compare-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.old-value {
  color: #909399;
  text-decoration: line-through;
  font-size: 13px;
}

.old-value.no-change {
  color: #606266;
  text-decoration: none;
}

.new-value {
  color: #67C23A;
  font-weight: 600;
  font-size: 13px;
}

.arrow-icon {
  color: #c0c4cc;
  font-size: 14px;
}

.skip-reason {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 10px 14px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 6px;
  color: #e6a23c;
  font-size: 13px;
}

.preview-detail {
  margin-bottom: 8px;
}
</style>
