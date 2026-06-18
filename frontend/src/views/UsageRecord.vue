<template>
  <div class="usage-record">
    <div class="top-bar">
      <el-select v-model="selectedDeviceId" placeholder="选择设备（留空查看全部）" clearable filterable style="width: 280px" @change="fetchData">
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

    <el-card shadow="never" class="scene-card">
      <template #header>
        <div class="scene-header">
          <span class="scene-title">使用场景分布</span>
          <div class="scene-filters">
            <el-select v-model="sceneFilterType" placeholder="设备类型" clearable style="width: 140px" @change="fetchSceneDistribution">
              <el-option label="音响" value="SPEAKER" />
              <el-option label="投影仪" value="PROJECTOR" />
              <el-option label="播放器" value="PLAYER" />
              <el-option label="功放" value="AMPLIFIER" />
            </el-select>
            <el-select v-model="sceneFilterLocation" placeholder="存放位置" clearable style="width: 160px" @change="fetchSceneDistribution">
              <el-option v-for="loc in locationList" :key="loc" :label="loc" :value="loc" />
            </el-select>
          </div>
        </div>
      </template>
      <div class="scene-body">
        <div ref="chartRef" class="scene-chart" v-loading="sceneLoading"></div>
        <div class="scene-legend" v-if="sceneItems.length > 0">
          <div class="scene-total">
            <span class="scene-total-label">总时长</span>
            <span class="scene-total-value">{{ formatDuration(sceneTotalMinutes) }}</span>
          </div>
          <div class="scene-items">
            <div v-for="item in sceneItems" :key="item.scenario" class="scene-item">
              <span class="scene-dot" :style="{ background: scenarioColor(item.scenario) }"></span>
              <span class="scene-name">{{ item.scenario }}</span>
              <span class="scene-duration">{{ formatDuration(item.durationMinutes) }}</span>
              <span class="scene-percent">{{ item.percent }}%</span>
            </div>
          </div>
        </div>
        <el-empty v-if="sceneItems.length === 0 && !sceneLoading" description="暂无场景数据" :image-size="60" />
      </div>
    </el-card>

    <UsageHeatmap ref="heatmapRef" :device-id="selectedDeviceId" style="margin-top: 20px;" />

    <el-card shadow="never" style="margin-top: 20px;">
      <template #header>
        <span style="font-weight: 600; color: #303133;">使用记录列表</span>
      </template>
      <el-table :data="usageList" v-loading="loading" stripe>
        <el-table-column prop="usageDate" label="日期" width="120" />
        <el-table-column label="设备名称" width="140">
          <template #default="{ row }">{{ row.device?.name }}</template>
        </el-table-column>
        <el-table-column label="使用时长" width="120">
          <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
        </el-table-column>
        <el-table-column prop="scenario" label="使用场景" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="scenarioTagType(row.scenario)">{{ row.scenario || '-' }}</el-tag>
          </template>
        </el-table-column>
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
          <el-select v-model="form.deviceId" placeholder="选择设备" style="width: 100%">
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
            <el-option label="调试" value="调试" />
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
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getDevices, getAllUsage, createUsage, updateUsage, deleteUsage, getAllUsageStats, getUsageSceneDistribution, getUsageLocations } from '../api'
import UsageHeatmap from '../components/UsageHeatmap.vue'
import * as echarts from 'echarts'
import { normalizeLocation, sortLocations } from '../utils/location'

const deviceList = ref([])
const selectedDeviceId = ref('')
const usageList = ref([])
const loading = ref(false)
const heatmapRef = ref(null)

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

const scenarioTagType = (scenario) => {
  const map = { '观影': 'primary', '音乐': 'success', '游戏': 'warning', '调试': 'danger', '其他': 'info' }
  return map[scenario] || 'info'
}

const scenarioColor = (scenario) => {
  const map = { '观影': '#409EFF', '音乐': '#67C23A', '游戏': '#E6A23C', '调试': '#F56C6C', '其他': '#909399', '未分类': '#C0C4CC' }
  return map[scenario] || '#909399'
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

const sceneFilterType = ref('')
const sceneFilterLocation = ref('')
const locationList = ref([])
const sceneItems = ref([])
const sceneTotalMinutes = ref(0)
const sceneLoading = ref(false)
const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (chartRef.value && !chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
}

const renderChart = () => {
  if (!chartInstance) return
  if (sceneItems.value.length === 0) {
    chartInstance.clear()
    return
  }
  const data = sceneItems.value.map(item => ({
    name: item.scenario,
    value: item.durationMinutes,
    itemStyle: { color: scenarioColor(item.scenario) }
  }))
  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const h = Math.floor(params.value / 60)
        const m = params.value % 60
        const dur = h > 0 ? `${h}时${m}分` : `${m}分`
        return `${params.name}<br/>时长：${dur}<br/>占比：${params.percent}%`
      }
    },
    legend: { show: false },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: {
        show: true,
        formatter: '{b}\n{d}%',
        fontSize: 12,
        color: '#606266'
      },
      labelLine: { length: 12, length2: 8 },
      emphasis: {
        label: { fontSize: 14, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.2)' }
      },
      data
    }]
  }, true)
}

const fetchSceneDistribution = async () => {
  sceneLoading.value = true
  try {
    const res = await getUsageSceneDistribution(sceneFilterType.value || undefined, sceneFilterLocation.value || undefined)
    sceneItems.value = Array.isArray(res?.items) ? res.items : []
    sceneTotalMinutes.value = res?.totalMinutes || 0
    await nextTick()
    renderChart()
  } catch (e) {
    sceneItems.value = []
    sceneTotalMinutes.value = 0
  } finally {
    sceneLoading.value = false
  }
}

const fetchLocations = async () => {
  try {
    const res = await getUsageLocations()
    const locations = Array.isArray(res) ? res : []
    const normalized = [...new Set(locations.map(loc => normalizeLocation(loc)).filter(Boolean))]
    locationList.value = sortLocations(normalized)
  } catch (e) {
    locationList.value = []
  }
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
  loading.value = true
  try {
    const deviceId = selectedDeviceId.value || undefined
    const [usageRes, statsRes] = await Promise.all([
      getAllUsage(deviceId),
      getAllUsageStats(deviceId).catch(() => null)
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
    heatmapRef.value?.refresh()
    fetchSceneDistribution()
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
    heatmapRef.value?.refresh()
    fetchSceneDistribution()
  } catch (e) { /* cancelled */ }
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(async () => {
  await fetchDevices()
  await fetchLocations()
  await nextTick()
  initChart()
  fetchData()
  fetchSceneDistribution()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
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

.scene-card {
  margin-bottom: 0;
}
.scene-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.scene-title {
  font-weight: 600;
  color: #303133;
  font-size: 15px;
}
.scene-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}
.scene-body {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  min-height: 260px;
}
.scene-chart {
  flex: 0 0 320px;
  height: 260px;
}
.scene-legend {
  flex: 1;
  min-width: 0;
  padding-top: 8px;
}
.scene-total {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}
.scene-total-label {
  font-size: 13px;
  color: #909399;
}
.scene-total-value {
  font-size: 20px;
  font-weight: 700;
  color: #409EFF;
}
.scene-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.scene-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}
.scene-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.scene-name {
  flex: 1;
  min-width: 0;
}
.scene-duration {
  color: #909399;
  font-size: 12px;
}
.scene-percent {
  font-weight: 600;
  color: #303133;
  min-width: 48px;
  text-align: right;
}

@media (max-width: 768px) {
  .scene-body {
    flex-direction: column;
  }
  .scene-chart {
    flex: none;
    width: 100%;
  }
}
</style>
