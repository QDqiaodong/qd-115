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
        <el-option label="保养中" value="MAINTENANCE" />
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
          :lastMaintenanceTime="maintenanceSummaries[device.id]?.lastMaintenanceTime || null"
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
            <el-option label="正常" value="NORMAL" :disabled="statusDisabled('NORMAL')" />
            <el-option label="故障" value="FAULTY" :disabled="statusDisabled('FAULTY')" />
            <el-option label="保养中" value="MAINTENANCE" :disabled="statusDisabled('MAINTENANCE')" />
            <el-option label="退役" value="RETIRED" :disabled="statusDisabled('RETIRED')" />
          </el-select>
          <div v-if="formMode === 'edit' && form.originalStatus === 'RETIRED'" class="status-hint">
            退役设备不可恢复为正常或保养状态
          </div>
        </el-form-item>
        <el-form-item label="购入日期" prop="purchaseDate">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择购入日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="存放位置" prop="location">
          <el-select v-model="form.location" placeholder="请选择存放位置" filterable allow-create default-first-option style="width: 100%">
            <el-option v-for="loc in STANDARD_LOCATIONS" :key="loc" :label="loc" :value="loc" />
          </el-select>
        </el-form-item>
        <el-form-item label="硬件规格" prop="hardwareSpecs">
          <el-input v-model="form.hardwareSpecs" type="textarea" :rows="3" placeholder="请输入硬件规格参数" />
        </el-form-item>
        <template v-if="form.deviceType === 'PROJECTOR'">
          <el-divider content-position="left">灯泡信息</el-divider>
          <el-form-item label="灯泡安装日期" prop="lampInstallDate">
            <el-date-picker v-model="form.lampInstallDate" type="date" value-format="YYYY-MM-DD" placeholder="选择灯泡安装日期" style="width: 100%" />
          </el-form-item>
          <el-form-item label="更换阈值(小时)" prop="lampReplaceHours">
            <el-input-number v-model="form.lampReplaceHours" :min="1" :max="100000" placeholder="请输入更换阈值" style="width: 100%" />
          </el-form-item>
        </template>
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
            <template v-if="currentDevice.deviceType === 'PROJECTOR'">
              <el-descriptions-item label="灯泡安装日期">{{ currentDevice.lampInstallDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="灯泡更换阈值">{{ currentDevice.lampReplaceHours ? currentDevice.lampReplaceHours + ' 小时' : '-' }}</el-descriptions-item>
            </template>
          </el-descriptions>
        <el-tabs v-model="detailTab" style="margin-top: 20px">
          <el-tab-pane label="运维总览" name="overview">
            <el-row :gutter="12" class="overview-stats">
              <el-col :span="8">
                <div class="overview-card">
                  <div class="overview-card-label">累计使用时长</div>
                  <div class="overview-card-value overview-blue">{{ overviewStats.totalDuration }}</div>
                  <div class="overview-card-sub">共 {{ overviewStats.usageCount }} 次使用</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-card">
                  <div class="overview-card-label">最近维修</div>
                  <div class="overview-card-value overview-red">{{ overviewStats.lastRepairTime }}</div>
                  <div class="overview-card-sub">累计 {{ overviewStats.repairCount }} 次检修</div>
                </div>
              </el-col>
              <el-col :span="8">
                <div class="overview-card">
                  <div class="overview-card-label">最近保养</div>
                  <div class="overview-card-value overview-green">{{ overviewStats.lastMaintenanceTime }}</div>
                  <div class="overview-card-sub">累计 {{ overviewStats.maintenanceCount }} 次养护</div>
                </div>
              </el-col>
            </el-row>

            <div class="next-maintenance-section" v-if="nextMaintenanceWindows">
              <div class="section-title">下次保养窗口</div>
              <div v-if="earliestUrgentWindow" class="urgent-banner" :class="{ overdue: earliestUrgentWindow.overdue }">
                <el-icon :size="16"><Warning /></el-icon>
                <span v-if="earliestUrgentWindow.overdue">
                  {{ earliestUrgentWindow.maintenanceTypeLabel }}已逾期 {{ Math.abs(earliestUrgentWindow.daysUntil) }} 天
                </span>
                <span v-else>
                  {{ earliestUrgentWindow.maintenanceTypeLabel }}将于 {{ earliestUrgentWindow.daysUntil }} 天后到期
                </span>
              </div>
              <el-row :gutter="12" style="margin-top: 12px">
                <el-col :span="6" v-for="w in Object.values(nextMaintenanceWindows)" :key="w.maintenanceType">
                  <div class="window-card" :class="{ overdue: w.overdue, urgent: w.urgent }">
                    <div class="window-type">{{ w.maintenanceTypeLabel }}</div>
                    <div class="window-interval">每 {{ w.intervalDays }} 天</div>
                    <div class="window-next" v-if="w.nextTime">
                      {{ w.nextTime }}
                    </div>
                    <div class="window-next no-data" v-else>暂无记录</div>
                    <div class="window-status" v-if="w.nextTime">
                      <el-tag v-if="w.overdue" type="danger" size="small" effect="dark">逾期{{ Math.abs(w.daysUntil) }}天</el-tag>
                      <el-tag v-else-if="w.urgent" type="warning" size="small" effect="dark">即将到期</el-tag>
                      <el-tag v-else type="success" size="small" effect="dark">剩余{{ w.daysUntil }}天</el-tag>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <div class="lamp-life-section" v-if="lampLifeInfo?.available">
              <div class="section-title">
                <el-icon><Sunny /></el-icon>
                灯泡寿命
              </div>
              <div class="lamp-life-card" :class="{ expired: lampLifeInfo.status === 'EXPIRED', warning: lampLifeInfo.status === 'WARNING' }">
                <div class="lamp-life-header">
                  <div class="lamp-life-status" :style="{ color: lampColor(lampLifeInfo.status) }">
                    <el-icon :size="18"><Sunny /></el-icon>
                    <span>{{ lampStatusLabel(lampLifeInfo.status) }}</span>
                  </div>
                  <div class="lamp-life-percent" :style="{ color: lampColor(lampLifeInfo.status) }">
                    {{ lampLifeInfo.usedPercent?.toFixed(1) }}%
                  </div>
                </div>
                <div class="lamp-life-bar-track">
                  <div class="lamp-life-bar-fill" :style="{ width: lampLifeInfo.usedPercent + '%', background: lampColor(lampLifeInfo.status) }"></div>
                </div>
                <el-row :gutter="12" class="lamp-life-stats">
                  <el-col :span="8">
                    <div class="lamp-stat-label">已使用</div>
                    <div class="lamp-stat-value">{{ lampLifeInfo.usedHours }} 小时</div>
                  </el-col>
                  <el-col :span="8">
                    <div class="lamp-stat-label">更换阈值</div>
                    <div class="lamp-stat-value">{{ lampLifeInfo.lampReplaceHours }} 小时</div>
                  </el-col>
                  <el-col :span="8">
                    <div class="lamp-stat-label">剩余寿命</div>
                    <div class="lamp-stat-value" :style="{ color: lampColor(lampLifeInfo.status) }">{{ lampLifeInfo.remainingHours }} 小时</div>
                  </el-col>
                </el-row>
                <div class="lamp-install-date">
                  灯泡安装日期：{{ lampLifeInfo.lampInstallDate || '未设置' }}
                </div>
              </div>
            </div>

            <div class="timeline-section">
              <div class="section-title">运维时间线</div>
              <el-timeline v-if="getOverviewTimeline().length > 0">
                <el-timeline-item
                  v-for="(event, idx) in getOverviewTimeline()"
                  :key="idx"
                  :timestamp="event.time"
                  :color="timelineTypeColor(event.type)"
                  placement="top"
                >
                  <el-card shadow="never" class="timeline-card">
                    <div class="timeline-header">
                      <el-tag :type="event.type === 'usage' ? 'primary' : event.type === 'repair' ? 'danger' : event.type === 'maintenance' ? 'success' : 'warning'" size="small">
                        {{ timelineTypeLabel(event.type) }}
                      </el-tag>
                      <span class="timeline-title">{{ event.title }}</span>
                    </div>
                    <div class="timeline-content">{{ event.content }}</div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无运维记录" :image-size="60" />
            </div>
          </el-tab-pane>
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
            <div v-if="detailRepairList.length > 0" class="cost-trend-section">
              <div class="section-title">
                <el-icon><TrendCharts /></el-icon>
                历次维修费用趋势
              </div>
              <div ref="costChartRef" class="cost-chart"></div>
              <div class="cost-summary">
                <el-row :gutter="12">
                  <el-col :span="8">
                    <div class="summary-card">
                      <div class="summary-label">累计维修费用</div>
                      <div class="summary-value">¥{{ totalRepairCost.toFixed(2) }}</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="summary-card">
                      <div class="summary-label">平均每次费用</div>
                      <div class="summary-value">¥{{ avgRepairCost.toFixed(2) }}</div>
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div class="summary-card">
                      <div class="summary-label">最高单次费用</div>
                      <div class="summary-value">¥{{ maxRepairCost.toFixed(2) }}</div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>
            <el-table :data="sortedRepairList" size="small" v-loading="detailLoading" style="margin-top: 16px">
              <el-table-column prop="repairTime" label="时间" width="160" />
              <el-table-column prop="symptom" label="异常现象" show-overflow-tooltip />
              <el-table-column prop="cause" label="故障原因" show-overflow-tooltip min-width="120" />
              <el-table-column prop="fixMethod" label="修复方式" show-overflow-tooltip min-width="120" />
              <el-table-column prop="repairPerson" label="维修人员" width="100" />
              <el-table-column label="费用" width="110" align="right">
                <template #default="{ row }">
                  <span class="repair-cost">¥{{ Number(row.cost || 0).toFixed(2) }}</span>
                </template>
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
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Edit, TrendCharts, Warning, Sunny } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import DeviceCard from '../components/DeviceCard.vue'
import BatchEdit from '../components/BatchEdit.vue'
import {
  getDevices, createDevice, updateDevice, deleteDevice, batchUpdateDevices,
  getUsageByDevice, getRepairByDevice, getMaintenanceByDevice, getUsageStats,
  getDeviceMaintenanceSummaries, getLampLife
} from '../api'
import { calculateNextWindows, getEarliestUrgentWindow } from '../utils/maintenanceInterval'
import { STANDARD_LOCATIONS, normalizeLocation } from '../utils/location'

const devices = ref([])
const searchText = ref('')
const filterType = ref('')
const filterStatus = ref('')
const maintenanceSummaries = ref({})

const typeMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const statusMap = { NORMAL: '正常', FAULTY: '故障', MAINTENANCE: '保养中', RETIRED: '退役' }
const mtMap = { CLEANING: '机身清洁', CABLE: '线路整理', FIRMWARE: '固件升级', OTHER: '其他' }

const formVisible = ref(false)
const formMode = ref('create')
const formRef = ref(null)
const form = ref({
  name: '', model: '', deviceType: '', status: 'NORMAL',
  purchaseDate: '', location: '', hardwareSpecs: '',
  lampInstallDate: '', lampReplaceHours: null
})

const formRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  model: [{ required: true, message: '请输入设备型号', trigger: 'blur' }],
  deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择设备状态', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购入日期', trigger: 'change' }]
}

const detailVisible = ref(false)
const currentDevice = ref(null)
const detailTab = ref('overview')
const detailUsageList = ref([])
const detailRepairList = ref([])
const detailMaintenanceList = ref([])
const detailLoading = ref(false)
const lampLifeInfo = ref(null)
const overviewStats = ref({
  totalDuration: '0时0分',
  lastRepairTime: '-',
  lastMaintenanceTime: '-',
  repairCount: 0,
  maintenanceCount: 0,
  usageCount: 0
})

const costChartRef = ref(null)
let costChartInstance = null

const sortedRepairList = computed(() => {
  return [...detailRepairList.value].sort((a, b) => new Date(a.repairTime) - new Date(b.repairTime))
})

const totalRepairCost = computed(() => {
  return detailRepairList.value.reduce((sum, r) => sum + Number(r.cost || 0), 0)
})

const avgRepairCost = computed(() => {
  if (detailRepairList.value.length === 0) return 0
  return totalRepairCost.value / detailRepairList.value.length
})

const maxRepairCost = computed(() => {
  if (detailRepairList.value.length === 0) return 0
  return Math.max(...detailRepairList.value.map(r => Number(r.cost || 0)))
})

const batchEditVisible = ref(false)

const nextMaintenanceWindows = computed(() => {
  if (!currentDevice.value) return null
  const sorted = [...detailMaintenanceList.value].sort((a, b) => new Date(b.maintenanceTime) - new Date(a.maintenanceTime))
  const lastTime = sorted.length > 0 ? sorted[0].maintenanceTime : null
  return calculateNextWindows(currentDevice.value.deviceType, lastTime)
})

const earliestUrgentWindow = computed(() => {
  return getEarliestUrgentWindow(nextMaintenanceWindows.value)
})

const TRANSITION_RULES = {
  NORMAL: ['FAULTY', 'MAINTENANCE', 'RETIRED'],
  FAULTY: ['NORMAL', 'MAINTENANCE', 'RETIRED'],
  MAINTENANCE: ['NORMAL', 'FAULTY', 'RETIRED'],
  RETIRED: []
}

const allowedStatuses = computed(() => {
  if (formMode.value === 'create') {
    return ['NORMAL', 'FAULTY', 'MAINTENANCE', 'RETIRED']
  }
  const current = form.value.originalStatus
  if (!current) return ['NORMAL', 'FAULTY', 'MAINTENANCE', 'RETIRED']
  const allowed = TRANSITION_RULES[current] || []
  return [current, ...allowed]
})

const statusDisabled = (status) => {
  if (formMode.value === 'create') return false
  return !allowedStatuses.value.includes(status)
}

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

const lampColor = (status) => {
  const map = { NORMAL: '#67C23A', CAUTION: '#E6A23C', WARNING: '#F56C6C', EXPIRED: '#C45656' }
  return map[status] || '#909399'
}

const lampStatusLabel = (status) => {
  const map = { NORMAL: '正常', CAUTION: '注意', WARNING: '警告', EXPIRED: '已到期', UNSET: '未设置' }
  return map[status] || status
}

const formatDuration = (minutes) => {
  if (!minutes && minutes !== 0) return '0时0分'
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  return h > 0 ? `${h}时${m}分` : `${m}分`
}

const formatDateTimeShort = (value) => {
  if (!value) return '-'
  const str = String(value)
  if (str.includes('T')) {
    const d = new Date(str)
    if (!isNaN(d.getTime())) {
      const pad = (n) => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    }
  }
  return str.length > 16 ? str.slice(0, 16) : str
}

const calcOverviewStats = (usage, repair, maintenance, usageStats) => {
  const totalMin = usageStats?.totalMinutes || usage.reduce((sum, r) => sum + (r.durationMinutes || 0), 0)
  const sortedRepair = [...repair].sort((a, b) => new Date(b.repairTime) - new Date(a.repairTime))
  const sortedMaintenance = [...maintenance].sort((a, b) => new Date(b.maintenanceTime) - new Date(a.maintenanceTime))
  overviewStats.value = {
    totalDuration: formatDuration(totalMin),
    lastRepairTime: sortedRepair.length > 0 ? formatDateTimeShort(sortedRepair[0].repairTime) : '暂无记录',
    lastMaintenanceTime: sortedMaintenance.length > 0 ? formatDateTimeShort(sortedMaintenance[0].maintenanceTime) : '暂无记录',
    repairCount: repair.length,
    maintenanceCount: maintenance.length,
    usageCount: usage.length
  }
}

const getOverviewTimeline = () => {
  const events = []
  detailUsageList.value.forEach(r => {
    events.push({
      type: 'usage',
      time: r.usageDate,
      timestamp: new Date(r.usageDate).getTime(),
      title: `使用记录 - ${Math.floor(r.durationMinutes / 60)}时${r.durationMinutes % 60}分`,
      content: `场景：${r.scenario || '-'}${r.remark ? ' | 备注：' + r.remark : ''}`
    })
  })
  detailRepairList.value.forEach(r => {
    events.push({
      type: 'repair',
      time: formatDateTimeShort(r.repairTime),
      timestamp: new Date(r.repairTime).getTime(),
      title: `检修记录${r.cost ? ' - ¥' + Number(r.cost).toFixed(2) : ''}`,
      content: `异常：${r.symptom || '-'}${r.fixMethod ? ' | 修复：' + r.fixMethod : ''}`
    })
  })
  detailMaintenanceList.value.forEach(r => {
    events.push({
      type: 'maintenance',
      time: formatDateTimeShort(r.maintenanceTime),
      timestamp: new Date(r.maintenanceTime).getTime(),
      title: `养护记录 - ${mtMap[r.maintenanceType] || r.maintenanceType}`,
      content: `内容：${r.content || '-'}${r.operator ? ' | 操作人：' + r.operator : ''}`
    })
  })
  if (currentDevice.value?.createdAt) {
    events.push({
      type: 'status',
      time: formatDateTimeShort(currentDevice.value.createdAt),
      timestamp: new Date(currentDevice.value.createdAt).getTime(),
      title: '设备入库',
      content: `初始状态：${statusMap[currentDevice.value.status] || currentDevice.value.status}`
    })
  }
  return events.sort((a, b) => b.timestamp - a.timestamp)
}

const timelineTypeColor = (type) => {
  const map = { usage: '#409EFF', repair: '#F56C6C', maintenance: '#67C23A', status: '#E6A23C' }
  return map[type] || '#909399'
}

const timelineTypeLabel = (type) => {
  const map = { usage: '使用', repair: '检修', maintenance: '养护', status: '状态' }
  return map[type] || type
}

const fetchDevices = async () => {
  try {
    const [deviceRes, summaryRes] = await Promise.all([
      getDevices(),
      getDeviceMaintenanceSummaries().catch(() => ({}))
    ])
    devices.value = Array.isArray(deviceRes) ? deviceRes : []
    maintenanceSummaries.value = summaryRes || {}
  } catch (e) {
    ElMessage.error('获取设备列表失败')
  }
}

const openForm = (device = null) => {
  if (device && device.id) {
    formMode.value = 'edit'
    form.value = { ...device, originalStatus: device.status }
  } else {
    formMode.value = 'create'
    form.value = {
      name: '', model: '', deviceType: '', status: 'NORMAL',
      purchaseDate: '', location: '', hardwareSpecs: '',
      lampInstallDate: '', lampReplaceHours: null, originalStatus: ''
    }
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
    if (e !== false) {
      const interceptorMsg = e?.response?.data?.message
      if (!interceptorMsg) ElMessage.error('操作失败')
    }
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

const initCostChart = () => {
  if (!costChartRef.value || sortedRepairList.value.length === 0) return

  nextTick(() => {
    if (!costChartRef.value) return

    if (costChartInstance) {
      costChartInstance.dispose()
    }

    costChartInstance = echarts.init(costChartRef.value)

    const dates = sortedRepairList.value.map(r => {
      const d = new Date(r.repairTime)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    })
    const costs = sortedRepairList.value.map(r => Number(r.cost || 0))
    const cumulative = []
    let sum = 0
    costs.forEach(c => {
      sum += c
      cumulative.push(parseFloat(sum.toFixed(2)))
    })

    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params) => {
          let result = `<div style="font-weight: 600; margin-bottom: 8px;">${params[0].axisValue}</div>`
          params.forEach(p => {
            const marker = `<span style="display:inline-block;margin-right:4px;border-radius:50%;width:10px;height:10px;background-color:${p.color};"></span>`
            result += `<div style="margin: 4px 0;">${marker}${p.seriesName}: <span style="font-weight: 600;">¥${Number(p.value).toFixed(2)}</span></div>`
          })
          const idx = params[0].dataIndex
          const repair = sortedRepairList.value[idx]
          if (repair) {
            result += `<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee; font-size: 12px; color: #666;">`
            if (repair.symptom) result += `<div>异常: ${repair.symptom}</div>`
            if (repair.cause) result += `<div>原因: ${repair.cause}</div>`
            if (repair.repairPerson) result += `<div>维修人员: ${repair.repairPerson}</div>`
            result += `</div>`
          }
          return result
        }
      },
      legend: {
        data: ['单次费用', '累计费用'],
        bottom: 0
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '15%',
        top: '10%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: {
          rotate: 45,
          fontSize: 11
        },
        axisLine: { lineStyle: { color: '#dcdfe6' } }
      },
      yAxis: [
        {
          type: 'value',
          name: '单次费用(元)',
          position: 'left',
          axisLabel: { formatter: '¥{value}' },
          splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } }
        },
        {
          type: 'value',
          name: '累计费用(元)',
          position: 'right',
          axisLabel: { formatter: '¥{value}' },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '单次费用',
          type: 'bar',
          data: costs,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#f56c6c' },
              { offset: 1, color: '#fcb6b6' }
            ]),
            borderRadius: [4, 4, 0, 0]
          },
          barWidth: '40%',
          label: {
            show: true,
            position: 'top',
            formatter: '¥{c}',
            fontSize: 11,
            color: '#f56c6c'
          }
        },
        {
          name: '累计费用',
          type: 'line',
          yAxisIndex: 1,
          data: cumulative,
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: { color: '#409EFF' },
          lineStyle: { width: 3, color: '#409EFF' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
            ])
          }
        }
      ]
    }

    costChartInstance.setOption(option)

    const handleResize = () => {
      costChartInstance?.resize()
    }
    window.addEventListener('resize', handleResize)
    costChartInstance.__resizeHandler = handleResize
  })
}

const destroyCostChart = () => {
  if (costChartInstance) {
    if (costChartInstance.__resizeHandler) {
      window.removeEventListener('resize', costChartInstance.__resizeHandler)
    }
    costChartInstance.dispose()
    costChartInstance = null
  }
}

watch(detailTab, (newTab) => {
  if (newTab === 'repair' && detailVisible.value) {
    nextTick(() => initCostChart())
  }
})

watch(detailVisible, (visible) => {
  if (!visible) {
    destroyCostChart()
  }
})

onBeforeUnmount(() => {
  destroyCostChart()
})

const openDetail = async (device) => {
  destroyCostChart()
  currentDevice.value = device
  detailVisible.value = true
  detailTab.value = 'overview'
  detailLoading.value = true
  lampLifeInfo.value = null
  try {
    const [usage, repair, maintenance, usageStats, lampLife] = await Promise.all([
      getUsageByDevice(device.id).catch(() => []),
      getRepairByDevice(device.id).catch(() => []),
      getMaintenanceByDevice(device.id).catch(() => []),
      getUsageStats(device.id).catch(() => null),
      device.deviceType === 'PROJECTOR' ? getLampLife(device.id).catch(() => null) : Promise.resolve(null)
    ])
    detailUsageList.value = Array.isArray(usage) ? usage : []
    detailRepairList.value = Array.isArray(repair) ? repair : []
    detailMaintenanceList.value = Array.isArray(maintenance) ? maintenance : []
    lampLifeInfo.value = lampLife
    calcOverviewStats(detailUsageList.value, detailRepairList.value, detailMaintenanceList.value, usageStats)
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

.overview-stats { margin-bottom: 20px; }
.overview-card {
  background: linear-gradient(135deg, #f8faff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  transition: all 0.3s;
}
.overview-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
.overview-card-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.overview-card-value {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
  line-height: 1.3;
  word-break: break-all;
}
.overview-blue { color: #409EFF; }
.overview-red { color: #F56C6C; }
.overview-green { color: #67C23A; }
.overview-card-sub {
  font-size: 12px;
  color: #c0c4cc;
}

.next-maintenance-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}
.urgent-banner {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 6px;
  background: linear-gradient(135deg, #fdf6ec 0%, #fff7e6 100%);
  border: 1px solid #faecd8;
  color: #e6a23c;
  font-size: 13px;
  font-weight: 600;
}
.urgent-banner.overdue {
  background: linear-gradient(135deg, #fef0f0 0%, #fff1f0 100%);
  border-color: #fde2e2;
  color: #f56c6c;
}
.window-card {
  background: linear-gradient(135deg, #f8faff 0%, #ffffff 100%);
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
  transition: all 0.3s;
  position: relative;
}
.window-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}
.window-card.overdue {
  border-color: #fde2e2;
  background: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
}
.window-card.urgent {
  border-color: #faecd8;
  background: linear-gradient(135deg, #fdf6ec 0%, #ffffff 100%);
}
.window-type {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.window-interval {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.window-next {
  font-size: 13px;
  font-weight: 600;
  color: #409EFF;
  margin-bottom: 8px;
  line-height: 1.4;
}
.window-next.no-data {
  color: #c0c4cc;
  font-weight: 400;
}
.window-status {
  margin-top: 4px;
}

.lamp-life-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}
.lamp-life-section .section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: '#E6A23C';
}
.lamp-life-card {
  background: linear-gradient(135deg, #f0f9eb 0%, #ffffff 100%);
  border: 1px solid #e1f3d8;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
}
.lamp-life-card.warning {
  background: linear-gradient(135deg, #fdf6ec 0%, #ffffff 100%);
  border-color: #faecd8;
}
.lamp-life-card.expired {
  background: linear-gradient(135deg, #fef0f0 0%, #ffffff 100%);
  border-color: #fde2e2;
}
.lamp-life-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.lamp-life-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
}
.lamp-life-percent {
  font-size: 20px;
  font-weight: 700;
}
.lamp-life-bar-track {
  height: 10px;
  background: #ebeef5;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 16px;
}
.lamp-life-bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease, background 0.5s ease;
}
.lamp-life-stats {
  margin-bottom: 12px;
}
.lamp-stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  text-align: center;
}
.lamp-stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  text-align: center;
}
.lamp-install-date {
  font-size: 12px;
  color: #909399;
  text-align: center;
  padding-top: 8px;
  border-top: 1px solid #ebeef5;
}

.timeline-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 3px solid #409EFF;
}
.timeline-card {
  padding: 12px 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 4px;
}
.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.timeline-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}
.timeline-content {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}
:deep(.el-timeline-item__timestamp) {
  font-size: 12px;
  color: #909399;
}
.status-hint {
  font-size: 12px;
  color: #F56C6C;
  margin-top: 4px;
  line-height: 1.4;
}

.cost-trend-section {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
}

.cost-trend-section .section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #409EFF;
}

.cost-chart {
  width: 100%;
  height: 320px;
}

.cost-summary {
  margin-top: 16px;
}

.summary-card {
  background: linear-gradient(135deg, #fff5f5 0%, #ffffff 100%);
  border: 1px solid #fde2e2;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(245, 108, 108, 0.15);
  transform: translateY(-2px);
}

.summary-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
  line-height: 1.3;
}

.repair-cost {
  font-weight: 600;
  color: #f56c6c;
}
</style>
