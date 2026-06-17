<template>
  <div class="cost-statistics">
    <div class="top-bar">
      <el-select v-model="filters.deviceType" placeholder="设备类型" clearable style="width: 140px" @change="fetchData">
        <el-option label="音响" value="SPEAKER" />
        <el-option label="投影仪" value="PROJECTOR" />
        <el-option label="播放器" value="PLAYER" />
        <el-option label="功放" value="AMPLIFIER" />
      </el-select>
      <el-select v-model="filters.location" placeholder="位置" clearable style="width: 160px" @change="fetchData">
        <el-option v-for="loc in locationList" :key="loc" :label="loc" :value="loc" />
      </el-select>
      <el-date-picker
        v-model="filters.yearMonth"
        type="month"
        placeholder="选择月份"
        value-format="YYYY-MM"
        clearable
        style="width: 160px"
        @change="fetchData"
      />
      <el-input
        v-model="filters.cause"
        placeholder="故障原因关键词"
        clearable
        style="width: 180px"
        @clear="fetchData"
        @keyup.enter="fetchData"
      />
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <div class="summary-cards" v-if="statistics">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-value">¥{{ formatMoney(statistics.totalCost) }}</div>
        <div class="summary-label">维修总费用</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-value">{{ statistics.totalCount }}</div>
        <div class="summary-label">维修总次数</div>
      </el-card>
      <el-card shadow="hover" class="summary-card">
        <div class="summary-value">¥{{ formatMoney(statistics.avgCost) }}</div>
        <div class="summary-label">平均费用</div>
      </el-card>
    </div>

    <div class="charts-row" v-if="statistics">
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">按设备类型统计</span></template>
        <div ref="typeChartRef" class="chart-container"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">按位置统计</span></template>
        <div ref="locationChartRef" class="chart-container"></div>
      </el-card>
    </div>

    <div class="charts-row" v-if="statistics">
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">按月份费用趋势</span></template>
        <div ref="monthChartRef" class="chart-container"></div>
      </el-card>
      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">按故障原因统计</span></template>
        <div ref="causeChartRef" class="chart-container"></div>
      </el-card>
    </div>

    <el-card shadow="never" style="margin-top: 16px" v-if="statistics && statistics.records && statistics.records.length > 0">
      <template #header><span class="chart-title">费用明细</span></template>
      <el-table :data="statistics.records" stripe border size="small">
        <el-table-column prop="repairTime" label="检修时间" width="170" />
        <el-table-column label="设备名称" width="140">
          <template #default="{ row }">{{ row.device?.name }}</template>
        </el-table-column>
        <el-table-column label="设备类型" width="100">
          <template #default="{ row }">{{ typeLabel(row.device?.deviceType) }}</template>
        </el-table-column>
        <el-table-column label="位置" width="120">
          <template #default="{ row }">{{ row.device?.location || '-' }}</template>
        </el-table-column>
        <el-table-column prop="symptom" label="故障现象" show-overflow-tooltip min-width="150" />
        <el-table-column prop="cause" label="故障原因" show-overflow-tooltip min-width="120" />
        <el-table-column label="费用" width="110" align="right">
          <template #default="{ row }">
            <span class="cost-amount">¥{{ row.cost != null ? Number(row.cost).toFixed(2) : '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="repairPerson" label="维修人员" width="100" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getRepairCostStatistics, getDevices } from '../api'
import * as echarts from 'echarts'
import { normalizeLocation, sortLocations, getNormalizedLocationList } from '../utils/location'

const filters = ref({
  deviceType: '',
  location: '',
  yearMonth: '',
  cause: ''
})
const locationList = ref([])
const statistics = ref(null)

const typeChartRef = ref(null)
const locationChartRef = ref(null)
const monthChartRef = ref(null)
const causeChartRef = ref(null)

let typeChart = null
let locationChart = null
let monthChart = null
let causeChart = null

const typeMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const typeLabel = (t) => typeMap[t] || t || '-'

const formatMoney = (val) => {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

const fetchLocations = async () => {
  try {
    const res = await getDevices()
    const devices = Array.isArray(res) ? res : []
    locationList.value = getNormalizedLocationList(devices)
  } catch (e) { /* ignore */ }
}

const fetchData = async () => {
  try {
    const params = {}
    if (filters.value.deviceType) params.deviceType = filters.value.deviceType
    if (filters.value.location) params.location = filters.value.location
    if (filters.value.yearMonth) params.yearMonth = filters.value.yearMonth
    if (filters.value.cause) params.cause = filters.value.cause
    const res = await getRepairCostStatistics(params)
    statistics.value = res
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error('获取费用统计失败')
  }
}

const resetFilters = () => {
  filters.value = { deviceType: '', location: '', yearMonth: '', cause: '' }
  fetchData()
}

const renderCharts = () => {
  if (!statistics.value) return
  renderTypeChart()
  renderLocationChart()
  renderMonthChart()
  renderCauseChart()
}

const renderTypeChart = () => {
  if (!typeChartRef.value) return
  if (!typeChart) typeChart = echarts.init(typeChartRef.value)
  const data = statistics.value.byDeviceType || {}
  const names = Object.keys(data).map(k => typeMap[k] || k)
  const costs = Object.values(data).map(v => Number(v.totalCost))
  const counts = Object.values(data).map(v => v.count)
  typeChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['总费用', '维修次数'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: names },
    yAxis: [
      { type: 'value', name: '费用(元)', axisLabel: { formatter: '¥{value}' } },
      { type: 'value', name: '次数' }
    ],
    series: [
      { name: '总费用', type: 'bar', data: costs, itemStyle: { color: '#409EFF' } },
      { name: '维修次数', type: 'line', yAxisIndex: 1, data: counts, itemStyle: { color: '#67C23A' } }
    ]
  })
}

const renderLocationChart = () => {
  if (!locationChartRef.value) return
  if (!locationChart) locationChart = echarts.init(locationChartRef.value)
  const data = statistics.value.byLocation || {}
  const names = Object.keys(data)
  const costs = Object.values(data).map(v => Number(v.totalCost))
  const counts = Object.values(data).map(v => v.count)
  locationChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['总费用', '维修次数'] },
    grid: { left: 60, right: 60, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: names },
    yAxis: [
      { type: 'value', name: '费用(元)', axisLabel: { formatter: '¥{value}' } },
      { type: 'value', name: '次数' }
    ],
    series: [
      { name: '总费用', type: 'bar', data: costs, itemStyle: { color: '#E6A23C' } },
      { name: '维修次数', type: 'line', yAxisIndex: 1, data: counts, itemStyle: { color: '#F56C6C' } }
    ]
  })
}

const renderMonthChart = () => {
  if (!monthChartRef.value) return
  if (!monthChart) monthChart = echarts.init(monthChartRef.value)
  const data = statistics.value.byMonth || {}
  const sortedKeys = Object.keys(data).sort()
  const costs = sortedKeys.map(k => Number(data[k].totalCost))
  const avgCosts = sortedKeys.map(k => Number(data[k].avgCost))
  monthChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['总费用', '平均费用'] },
    grid: { left: 60, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: sortedKeys },
    yAxis: { type: 'value', name: '费用(元)', axisLabel: { formatter: '¥{value}' } },
    series: [
      { name: '总费用', type: 'bar', data: costs, itemStyle: { color: '#409EFF' } },
      { name: '平均费用', type: 'line', data: avgCosts, itemStyle: { color: '#E6A23C' } }
    ]
  })
}

const renderCauseChart = () => {
  if (!causeChartRef.value) return
  if (!causeChart) causeChart = echarts.init(causeChartRef.value)
  const data = statistics.value.byCause || {}
  const pieData = Object.entries(data).map(([name, v]) => ({
    name: name.length > 12 ? name.substring(0, 12) + '...' : name,
    value: Number(v.totalCost)
  }))
  causeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, fontSize: 12 },
      data: pieData
    }]
  })
}

const handleResize = () => {
  typeChart?.resize()
  locationChart?.resize()
  monthChart?.resize()
  causeChart?.resize()
}

onMounted(async () => {
  await fetchLocations()
  await fetchData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  typeChart?.dispose()
  locationChart?.dispose()
  monthChart?.dispose()
  causeChart?.dispose()
})
</script>

<style scoped>
.cost-statistics { padding: 0; }
.top-bar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px;
}
.summary-cards {
  display: flex; gap: 16px; margin-bottom: 20px;
}
.summary-card {
  flex: 1; text-align: center; border-radius: 8px;
}
.summary-value {
  font-size: 28px; font-weight: 700; color: #409EFF;
}
.summary-card:nth-child(2) .summary-value { color: #67C23A; }
.summary-card:nth-child(3) .summary-value { color: #E6A23C; }
.summary-label {
  font-size: 13px; color: #909399; margin-top: 4px;
}
.charts-row {
  display: flex; gap: 16px; margin-bottom: 16px;
}
.chart-card {
  flex: 1; border-radius: 8px;
}
.chart-title {
  font-weight: 600; font-size: 14px; color: #303133;
}
.chart-container {
  height: 300px; width: 100%;
}
.cost-amount {
  font-weight: 600; color: #f56c6c;
}
</style>
