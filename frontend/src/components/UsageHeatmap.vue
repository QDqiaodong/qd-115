<template>
  <div class="usage-heatmap">
    <div class="heatmap-header">
      <div class="title-row">
        <h3>使用热力日历</h3>
        <div class="nav-buttons">
          <el-button size="small" :icon="ArrowLeft" circle @click="prevMonth" />
          <span class="current-month">{{ currentMonthLabel }}</span>
          <el-button size="small" :icon="ArrowRight" circle @click="nextMonth" />
        </div>
      </div>
      <div class="legend">
        <span class="legend-label">使用时长：</span>
        <span class="legend-item">
          <span class="legend-color lvl-0"></span>
          <span class="legend-text">无</span>
        </span>
        <span class="legend-item">
          <span class="legend-color lvl-1"></span>
          <span class="legend-text">1-30分</span>
        </span>
        <span class="legend-item">
          <span class="legend-color lvl-2"></span>
          <span class="legend-text">31-60分</span>
        </span>
        <span class="legend-item">
          <span class="legend-color lvl-3"></span>
          <span class="legend-text">1-3时</span>
        </span>
        <span class="legend-item">
          <span class="legend-color lvl-4"></span>
          <span class="legend-text">3时以上</span>
        </span>
      </div>
    </div>

    <div class="heatmap-body" v-loading="loading">
      <div class="weekday-labels">
        <span v-for="(w, i) in weekdayLabels" :key="i" class="weekday-label">{{ w }}</span>
      </div>
      <div class="calendar-grid">
        <div
          v-for="(cell, idx) in calendarCells"
          :key="idx"
          class="calendar-cell"
          :class="[
            cell.inMonth ? 'in-month' : 'out-month',
            `lvl-${cell.level}`,
            cell.isToday ? 'today' : ''
          ]"
          :title="buildTooltip(cell)"
          @click="onCellClick(cell)"
        >
          <span class="cell-day">{{ cell.day }}</span>
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" :title="detailTitle" width="600px" destroy-on-close>
      <div v-if="dailyDetailList.length > 0">
        <el-table :data="dailyDetailList" stripe>
          <el-table-column prop="scenario" label="使用场景" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="scenarioTagType(row.scenario)">
                {{ row.scenario || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deviceName" label="设备名称" min-width="140" />
          <el-table-column label="持续时长" width="120">
            <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        </el-table>
        <div class="detail-summary">
          当日合计使用时长：
          <strong style="color: #409EFF; font-size: 16px;">
            {{ formatDuration(totalDailyMinutes) }}
          </strong>
        </div>
      </div>
      <el-empty v-else description="当天暂无使用记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { getUsageHeatmap, getUsageDailyDetails } from '../api'
import { ElMessage } from 'element-plus'

const props = defineProps({
  deviceId: { type: [Number, String], default: null }
})

const weekdayLabels = ['日', '一', '二', '三', '四', '五', '六']
const loading = ref(false)
const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth())
const heatmapData = ref({})
const detailVisible = ref(false)
const detailTitle = ref('')
const dailyDetailList = ref([])
const selectedDate = ref('')

const currentMonthLabel = computed(() => `${currentYear.value}年${currentMonth.value + 1}月`)

const firstDayOfMonth = computed(() => new Date(currentYear.value, currentMonth.value, 1))
const lastDayOfMonth = computed(() => new Date(currentYear.value, currentMonth.value + 1, 0))
const daysInMonth = computed(() => lastDayOfMonth.value.getDate())
const firstWeekday = computed(() => firstDayOfMonth.value.getDay())

const startDateStr = computed(() => formatDate(firstDayOfMonth.value))
const endDateStr = computed(() => formatDate(lastDayOfMonth.value))

const todayStr = formatDate(new Date())

const calendarCells = computed(() => {
  const cells = []
  for (let i = 0; i < firstWeekday.value; i++) {
    const prevMonthDate = new Date(currentYear.value, currentMonth.value, -firstWeekday.value + i + 1)
    cells.push({
      date: formatDate(prevMonthDate),
      day: prevMonthDate.getDate(),
      inMonth: false,
      level: 0,
      minutes: 0,
      isToday: false
    })
  }
  for (let d = 1; d <= daysInMonth.value; d++) {
    const date = formatDate(new Date(currentYear.value, currentMonth.value, d))
    const minutes = Number(heatmapData.value[date] || 0)
    cells.push({
      date,
      day: d,
      inMonth: true,
      level: getLevel(minutes),
      minutes,
      isToday: date === todayStr
    })
  }
  const totalCells = firstWeekday.value + daysInMonth.value
  const remainCells = (7 - (totalCells % 7)) % 7
  for (let i = 1; i <= remainCells; i++) {
    const nextMonthDate = new Date(currentYear.value, currentMonth.value + 1, i)
    cells.push({
      date: formatDate(nextMonthDate),
      day: i,
      inMonth: false,
      level: 0,
      minutes: 0,
      isToday: false
    })
  }
  return cells
})

const totalDailyMinutes = computed(() => {
  return dailyDetailList.value.reduce((sum, item) => sum + (item.durationMinutes || 0), 0)
})

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDuration(minutes) {
  if (!minutes && minutes !== 0) return '-'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h > 0 ? `${h}时${m}分` : `${m}分`
}

function getLevel(minutes) {
  if (!minutes || minutes <= 0) return 0
  if (minutes <= 30) return 1
  if (minutes <= 60) return 2
  if (minutes <= 180) return 3
  return 4
}

function buildTooltip(cell) {
  if (!cell.minutes) return `${cell.date}：无使用记录`
  return `${cell.date}：使用 ${formatDuration(cell.minutes)}`
}

function scenarioTagType(scenario) {
  const map = {
    '观影': 'primary',
    '音乐': 'success',
    '游戏': 'warning',
    '调试': 'danger',
    '其他': 'info'
  }
  return map[scenario] || 'info'
}

async function fetchHeatmap() {
  loading.value = true
  try {
    const deviceIdVal = props.deviceId ? Number(props.deviceId) : null
    const res = await getUsageHeatmap(deviceIdVal, startDateStr.value, endDateStr.value)
    const map = {}
    if (Array.isArray(res)) {
      res.forEach(item => {
        map[item.date] = Number(item.minutes) || 0
      })
    }
    heatmapData.value = map
  } catch (e) {
    ElMessage.error('获取热力日历数据失败')
  } finally {
    loading.value = false
  }
}

async function onCellClick(cell) {
  if (!cell.inMonth) {
    const d = new Date(cell.date)
    currentYear.value = d.getFullYear()
    currentMonth.value = d.getMonth()
    return
  }
  selectedDate.value = cell.date
  detailTitle.value = `${cell.date} 使用详情`
  detailVisible.value = true
  dailyDetailList.value = []
  try {
    const deviceIdVal = props.deviceId ? Number(props.deviceId) : null
    const res = await getUsageDailyDetails(deviceIdVal, cell.date)
    dailyDetailList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取当日详情失败')
  }
}

function prevMonth() {
  if (currentMonth.value === 0) {
    currentMonth.value = 11
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

function nextMonth() {
  if (currentMonth.value === 11) {
    currentMonth.value = 0
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

watch([() => props.deviceId, currentYear, currentMonth], () => {
  fetchHeatmap()
}, { immediate: false })

onMounted(() => {
  fetchHeatmap()
})

defineExpose({ refresh: fetchHeatmap })
</script>

<style scoped>
.usage-heatmap {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}

.heatmap-header {
  margin-bottom: 16px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title-row h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.nav-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
}

.current-month {
  font-size: 15px;
  font-weight: 600;
  color: #409EFF;
  min-width: 100px;
  text-align: center;
}

.legend {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #909399;
}

.legend-label {
  color: #606266;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  display: inline-block;
}

.legend-color.lvl-0 { background: #ebedf0; }
.legend-color.lvl-1 { background: #c6e48b; }
.legend-color.lvl-2 { background: #7bc96f; }
.legend-color.lvl-3 { background: #239a3b; }
.legend-color.lvl-4 { background: #196127; }

.heatmap-body {
  position: relative;
}

.weekday-labels {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 6px;
}

.weekday-label {
  text-align: center;
  font-size: 12px;
  color: #909399;
  font-weight: 500;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-cell {
  aspect-ratio: 1 / 1;
  min-height: 44px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  position: relative;
  border: 1px solid transparent;
  user-select: none;
}

.calendar-cell:hover {
  transform: scale(1.06);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 2;
}

.calendar-cell.out-month {
  opacity: 0.35;
  cursor: pointer;
}

.calendar-cell.out-month:hover {
  opacity: 0.6;
}

.calendar-cell.lvl-0 { background: #ebedf0; }
.calendar-cell.lvl-1 { background: #c6e48b; }
.calendar-cell.lvl-2 { background: #7bc96f; }
.calendar-cell.lvl-3 { background: #239a3b; color: #fff; }
.calendar-cell.lvl-4 { background: #196127; color: #fff; }

.calendar-cell.today {
  border-color: #409EFF;
  border-width: 2px;
}

.calendar-cell .cell-day {
  font-size: 13px;
  font-weight: 500;
}

.detail-summary {
  margin-top: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
  text-align: right;
}
</style>
