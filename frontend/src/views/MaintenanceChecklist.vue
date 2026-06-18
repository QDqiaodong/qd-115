<template>
  <div class="maintenance-checklist">
    <div class="top-bar">
      <el-select v-model="selectedDeviceId" placeholder="选择设备" filterable clearable style="width: 240px" @change="onDeviceChange">
        <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <div style="flex: 1" />
      <el-button type="primary" :icon="DocumentAdd" @click="startNewChecklist" :disabled="!selectedDeviceId">
        开始新保养
      </el-button>
    </div>

    <el-tabs v-model="activeType" class="type-tabs">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="清洁" name="CLEANING" />
      <el-tab-pane label="线缆检查" name="CABLE" />
      <el-tab-pane label="固件处理" name="FIRMWARE" />
      <el-tab-pane label="其他" name="OTHER" />
    </el-tabs>

    <div v-if="selectedDeviceId && currentChecklist" class="checklist-container">
      <el-card shadow="never" class="checklist-card" v-for="category in filteredCategories" :key="category.type">
        <template #header>
          <div class="category-header">
            <div class="category-title">
              <el-icon :size="20" :color="categoryIconColor(category.type)">
                <component :is="categoryIcon(category.type)" />
              </el-icon>
              <span>{{ categoryLabel(category.type) }}</span>
            </div>
            <el-tag :type="categoryProgressType(category)" size="small">
              {{ categoryCompletedCount(category) }}/{{ category.steps.length }} 已完成
            </el-tag>
          </div>
        </template>

        <el-steps :active="categoryActiveStep(category)" direction="vertical" finish-status="success">
          <el-step v-for="(step, idx) in category.steps" :key="idx" :title="step.title">
            <template #description>
              <div class="step-description">
                <p>{{ step.description }}</p>
                <div v-if="step.checked" class="step-meta">
                  <span class="step-operator">{{ step.operator || '未记录操作人' }}</span>
                  <span class="step-time">{{ formatTime(step.checkedAt) }}</span>
                </div>
              </div>
            </template>
            <template #icon>
              <div class="step-icon-wrapper" @click="toggleStep(category.type, idx)">
                <el-checkbox v-model="step.checked" :indeterminate="false" @change="onStepChange(category.type, idx)" />
              </div>
            </template>
          </el-step>
        </el-steps>
      </el-card>

      <el-card shadow="never" class="summary-card">
        <div class="summary-header">
          <span class="summary-title">保养进度总览</span>
          <el-tag :type="overallProgressType" size="large" effect="dark">
            {{ overallProgress }}% 完成
          </el-tag>
        </div>
        <el-progress :percentage="overallProgress" :stroke-width="12" :color="progressColor" />
        <div class="summary-stats">
          <div class="stat-item">
            <div class="stat-value">{{ totalSteps }}</div>
            <div class="stat-label">总步骤</div>
          </div>
          <div class="stat-item">
            <div class="stat-value success">{{ completedSteps }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <div class="stat-item">
            <div class="stat-value warning">{{ pendingSteps }}</div>
            <div class="stat-label">待完成</div>
          </div>
        </div>
        <div class="summary-actions">
          <el-button @click="resetChecklist">重置清单</el-button>
          <el-button type="primary" :icon="Check" @click="completeChecklist" :disabled="!allStepsCompleted">
            完成保养
          </el-button>
        </div>
      </el-card>
    </div>

    <el-empty v-else-if="!selectedDeviceId" description="请选择设备查看保养流程清单" :image-size="80" />
    <el-empty v-else description="暂无保养清单数据" :image-size="80" />

    <el-dialog v-model="completeDialogVisible" title="确认完成保养" width="480px" destroy-on-close>
      <el-form ref="completeFormRef" :model="completeForm" :rules="completeFormRules" label-width="100px">
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="completeForm.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="completeForm.remark" type="textarea" :rows="3" placeholder="请输入保养备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentAdd, Check, MagicStick, Connection, Upload, List } from '@element-plus/icons-vue'
import { getDevices, getMaintenanceByDevice, createMaintenance } from '../api'

const deviceList = ref([])
const selectedDeviceId = ref('')
const activeType = ref('all')
const currentChecklist = ref(null)
const completeDialogVisible = ref(false)
const completeFormRef = ref(null)
const completeForm = ref({ operator: '', remark: '' })

const completeFormRules = {
  operator: [{ required: true, message: '请输入操作人姓名', trigger: 'blur' }]
}

const CHECKLIST_TEMPLATES = {
  CLEANING: {
    type: 'CLEANING',
    steps: [
      { title: '设备外观清洁', description: '使用干净软布擦拭设备外壳，去除灰尘和污渍', checked: false, checkedAt: null, operator: '' },
      { title: '通风口除尘', description: '使用毛刷或吸尘器清理设备通风口，确保通风良好', checked: false, checkedAt: null, operator: '' },
      { title: '接口清洁', description: '使用专用清洁剂和棉签清洁各类接口，确保连接稳定', checked: false, checkedAt: null, operator: '' },
      { title: '屏幕/镜头清洁', description: '使用专用清洁布和清洁剂清洁显示屏或镜头', checked: false, checkedAt: null, operator: '' },
      { title: '内部除尘', description: '必要时打开设备外壳，使用吹气球清理内部灰尘', checked: false, checkedAt: null, operator: '' }
    ]
  },
  CABLE: {
    type: 'CABLE',
    steps: [
      { title: '线缆外观检查', description: '检查所有线缆外皮是否有破损、老化现象', checked: false, checkedAt: null, operator: '' },
      { title: '接口连接检查', description: '检查所有接口连接是否牢固，有无松动', checked: false, checkedAt: null, operator: '' },
      { title: '线缆整理', description: '整理线缆布局，使用扎带固定，确保走线整齐', checked: false, checkedAt: null, operator: '' },
      { title: '接口氧化处理', description: '检查金属接口是否氧化，必要时进行清洁处理', checked: false, checkedAt: null, operator: '' },
      { title: '线缆标签确认', description: '确认线缆标签清晰可辨，必要时重新标注', checked: false, checkedAt: null, operator: '' }
    ]
  },
  FIRMWARE: {
    type: 'FIRMWARE',
    steps: [
      { title: '当前版本确认', description: '检查设备当前固件版本号并记录', checked: false, checkedAt: null, operator: '' },
      { title: '新版本检查', description: '检查官方是否发布新固件版本', checked: false, checkedAt: null, operator: '' },
      { title: '备份配置', description: '备份设备当前配置和数据，防止升级失败', checked: false, checkedAt: null, operator: '' },
      { title: '固件升级', description: '按照官方指引执行固件升级操作', checked: false, checkedAt: null, operator: '' },
      { title: '升级后验证', description: '确认固件升级成功，测试设备功能是否正常', checked: false, checkedAt: null, operator: '' }
    ]
  },
  OTHER: {
    type: 'OTHER',
    steps: [
      { title: '设备状态检测', description: '运行设备自检程序，确认各项功能正常', checked: false, checkedAt: null, operator: '' },
      { title: '散热系统检查', description: '检查风扇运转是否正常，散热片是否积尘', checked: false, checkedAt: null, operator: '' },
      { title: '螺丝紧固检查', description: '检查设备外壳螺丝是否松动，必要时紧固', checked: false, checkedAt: null, operator: '' },
      { title: '功能测试', description: '对设备主要功能进行测试验证', checked: false, checkedAt: null, operator: '' },
      { title: '记录归档', description: '填写保养记录，归档相关文档', checked: false, checkedAt: null, operator: '' }
    ]
  }
}

const mtLabelMap = { CLEANING: '清洁', CABLE: '线缆检查', FIRMWARE: '固件处理', OTHER: '其他' }
const mtIconMap = { CLEANING: MagicStick, CABLE: Connection, FIRMWARE: Upload, OTHER: List }
const mtColorMap = { CLEANING: '#67C23A', CABLE: '#409EFF', FIRMWARE: '#E6A23C', OTHER: '#909399' }

const categoryLabel = (type) => mtLabelMap[type] || type
const categoryIcon = (type) => mtIconMap[type] || List
const categoryIconColor = (type) => mtColorMap[type] || '#909399'

const categories = computed(() => {
  if (!currentChecklist.value) return []
  return Object.values(currentChecklist.value.categories)
})

const filteredCategories = computed(() => {
  if (activeType.value === 'all') return categories.value
  return categories.value.filter(c => c.type === activeType.value)
})

const categoryCompletedCount = (category) => {
  return category.steps.filter(s => s.checked).length
}

const categoryActiveStep = (category) => {
  const firstUnchecked = category.steps.findIndex(s => !s.checked)
  return firstUnchecked === -1 ? category.steps.length : firstUnchecked
}

const categoryProgressType = (category) => {
  const completed = categoryCompletedCount(category)
  const total = category.steps.length
  if (completed === total) return 'success'
  if (completed > 0) return 'warning'
  return 'info'
}

const totalSteps = computed(() => {
  if (!currentChecklist.value) return 0
  return Object.values(currentChecklist.value.categories).reduce((sum, c) => sum + c.steps.length, 0)
})

const completedSteps = computed(() => {
  if (!currentChecklist.value) return 0
  return Object.values(currentChecklist.value.categories).reduce((sum, c) => sum + categoryCompletedCount(c), 0)
})

const pendingSteps = computed(() => totalSteps.value - completedSteps.value)

const overallProgress = computed(() => {
  if (totalSteps.value === 0) return 0
  return Math.round((completedSteps.value / totalSteps.value) * 100)
})

const overallProgressType = computed(() => {
  if (overallProgress.value === 100) return 'success'
  if (overallProgress.value > 50) return 'warning'
  return 'info'
})

const progressColor = computed(() => {
  if (overallProgress.value === 100) return '#67C23A'
  if (overallProgress.value > 50) return '#E6A23C'
  return '#909399'
})

const allStepsCompleted = computed(() => completedSteps.value === totalSteps.value && totalSteps.value > 0)

const fetchDevices = async () => {
  try {
    const res = await getDevices()
    deviceList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取设备列表失败')
  }
}

const onDeviceChange = () => {
  if (selectedDeviceId.value) {
    loadChecklist()
  } else {
    currentChecklist.value = null
  }
}

const loadChecklist = () => {
  startNewChecklist()
}

const startNewChecklist = () => {
  const categories = {}
  Object.keys(CHECKLIST_TEMPLATES).forEach(type => {
    categories[type] = {
      type,
      steps: CHECKLIST_TEMPLATES[type].steps.map(s => ({ ...s, checked: false, checkedAt: null, operator: '' }))
    }
  })
  currentChecklist.value = {
    deviceId: selectedDeviceId.value,
    startTime: new Date().toISOString(),
    categories
  }
}

const toggleStep = (categoryType, stepIndex) => {
  const step = currentChecklist.value.categories[categoryType].steps[stepIndex]
  step.checked = !step.checked
  onStepChange(categoryType, stepIndex)
}

const onStepChange = (categoryType, stepIndex) => {
  const step = currentChecklist.value.categories[categoryType].steps[stepIndex]
  if (step.checked) {
    step.checkedAt = new Date().toISOString()
  } else {
    step.checkedAt = null
  }
}

const resetChecklist = () => {
  ElMessageBox.confirm('确认重置所有步骤？已完成的进度将被清除。', '重置确认', { type: 'warning' })
    .then(() => {
      startNewChecklist()
      ElMessage.success('清单已重置')
    })
    .catch(() => {})
}

const completeChecklist = () => {
  completeForm.value = { operator: '', remark: '' }
  completeDialogVisible.value = true
}

const submitComplete = async () => {
  try {
    await completeFormRef.value.validate()
    
    const completedTypes = []
    const contents = []
    
    Object.values(currentChecklist.value.categories).forEach(category => {
      if (category.steps.some(s => s.checked)) {
        completedTypes.push(category.type)
        const completedSteps = category.steps.filter(s => s.checked).map(s => s.title).join('、')
        contents.push(`${categoryLabel(category.type)}：${completedSteps}`)
      }
    })

    for (const type of completedTypes) {
      await createMaintenance({
        deviceId: selectedDeviceId.value,
        maintenanceTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
        maintenanceType: type,
        content: contents.find(c => c.startsWith(categoryLabel(type))),
        operator: completeForm.value.operator,
        remark: completeForm.value.remark
      })
    }

    ElMessage.success('保养记录已保存')
    completeDialogVisible.value = false
    startNewChecklist()
  } catch (e) {
    if (e !== false) ElMessage.error('保存失败')
  }
}

const formatTime = (value) => {
  if (!value) return ''
  const d = new Date(value)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(fetchDevices)
</script>

<style scoped>
.maintenance-checklist { padding: 0; }

.top-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.type-tabs {
  margin-bottom: 16px;
  background: #fff;
  padding: 0 16px;
  border-radius: 8px;
}

.checklist-container {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.checklist-card {
  flex: 1;
  margin-bottom: 16px;
}

.category-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.step-description {
  padding: 8px 0;
}

.step-description p {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.step-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.step-icon-wrapper {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-card {
  width: 320px;
  position: sticky;
  top: 20px;
  flex-shrink: 0;
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.summary-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.summary-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 16px;
  padding: 16px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  margin-bottom: 4px;
}

.stat-value.success { color: #67C23A; }
.stat-value.warning { color: #E6A23C; }

.stat-label {
  font-size: 12px;
  color: #909399;
}

.summary-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.summary-actions .el-button {
  flex: 1;
}

:deep(.el-step__title) {
  font-weight: 500;
}

:deep(.el-step.is-simple .el-step__description) {
  padding-left: 0;
}
</style>
