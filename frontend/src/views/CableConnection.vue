<template>
  <div class="cable-connection">
    <div class="top-bar">
      <el-select v-model="selectedDeviceId" placeholder="选择设备（留空查看全部）" clearable filterable style="width: 240px" @change="fetchData">
        <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <div style="flex: 1" />
      <el-button type="primary" :icon="Plus" @click="openForm">新增连接</el-button>
    </div>

    <el-card shadow="never" class="diagram-card">
      <div class="card-title-row">
        <span class="card-title">连接关系图</span>
        <el-tag size="small" type="info">共 {{ connectionList.length }} 条连接</el-tag>
      </div>
      <div class="connection-diagram">
        <svg :width="diagramWidth" :height="diagramHeight" class="diagram-svg">
          <defs>
            <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#409EFF" />
            </marker>
            <linearGradient :id="'line-grad-' + i" v-for="(conn, i) in connectionList" :key="'grad-' + i" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" :style="{ stopColor: getInterfaceColor(conn.interfaceType).start }" />
              <stop offset="100%" :style="{ stopColor: getInterfaceColor(conn.interfaceType).end }" />
            </linearGradient>
          </defs>

          <g v-for="(device, index) in deviceNodes" :key="device.id" class="device-node">
            <rect
              :x="device.x"
              :y="device.y"
              :width="nodeWidth"
              :height="nodeHeight"
              rx="8"
              :fill="getDeviceTypeColor(device.deviceType)"
              class="device-rect"
            />
            <text :x="device.x + nodeWidth / 2" :y="device.y + nodeHeight / 2 - 8" text-anchor="middle" class="device-name">
              {{ device.name }}
            </text>
            <text :x="device.x + nodeWidth / 2" :y="device.y + nodeHeight / 2 + 12" text-anchor="middle" class="device-type">
              {{ getDeviceTypeLabel(device.deviceType) }}
            </text>
          </g>

          <g v-for="(conn, i) in connectionList" :key="'line-' + i">
            <path
              :d="getConnectionPath(conn)"
              :stroke="'url(#line-grad-' + i + ')'"
              stroke-width="3"
              fill="none"
              marker-end="url(#arrowhead)"
              class="connection-line"
            />
            <text
              :x="getLabelPosition(conn).x"
              :y="getLabelPosition(conn).y"
              text-anchor="middle"
              class="connection-label"
            >
              {{ conn.interfaceType }} · {{ conn.cableLength }}m
            </text>
          </g>
        </svg>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="connectionList" v-loading="loading" stripe>
        <el-table-column label="源设备" min-width="140">
          <template #default="{ row }">
            <div class="device-cell">
              <el-tag :type="getDeviceTagType(row.sourceDevice?.deviceType)" size="small" effect="light">
                {{ row.sourceDevice?.name }}
              </el-tag>
              <span class="port-info" v-if="row.sourcePort">{{ row.sourcePort }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="目标设备" min-width="140">
          <template #default="{ row }">
            <div class="device-cell">
              <el-tag :type="getDeviceTagType(row.targetDevice?.deviceType)" size="small" effect="light">
                {{ row.targetDevice?.name }}
              </el-tag>
              <span class="port-info" v-if="row.targetPort">{{ row.targetPort }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="interfaceType" label="接口类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getInterfaceTagType(row.interfaceType)" size="small" effect="dark">
              {{ row.interfaceType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cableLength" label="线缆长度" width="100">
          <template #default="{ row }">{{ row.cableLength }} 米</template>
        </el-table-column>
        <el-table-column prop="connectionLocation" label="连接位置" width="160" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="formMode === 'create' ? '新增连接' : '编辑连接'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="源设备" prop="sourceDeviceId">
          <el-select v-model="form.sourceDeviceId" placeholder="选择源设备" style="width: 100%" filterable>
            <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="源端口" prop="sourcePort">
          <el-input v-model="form.sourcePort" placeholder="如 HDMI OUT 1" />
        </el-form-item>
        <el-form-item label="目标设备" prop="targetDeviceId">
          <el-select v-model="form.targetDeviceId" placeholder="选择目标设备" style="width: 100%" filterable>
            <el-option v-for="d in deviceList" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标端口" prop="targetPort">
          <el-input v-model="form.targetPort" placeholder="如 HDMI IN 1" />
        </el-form-item>
        <el-form-item label="接口类型" prop="interfaceType">
          <el-select v-model="form.interfaceType" placeholder="选择接口类型" style="width: 100%" filterable allow-create>
            <el-option label="HDMI 2.1" value="HDMI 2.1" />
            <el-option label="HDMI 2.0" value="HDMI 2.0" />
            <el-option label="光纤" value="光纤" />
            <el-option label="同轴" value="同轴" />
            <el-option label="XLR平衡" value="XLR平衡" />
            <el-option label="RCA" value="RCA" />
            <el-option label="喇叭线" value="喇叭线" />
            <el-option label="USB" value="USB" />
            <el-option label="网线" value="网线" />
          </el-select>
        </el-form-item>
        <el-form-item label="线缆长度" prop="cableLength">
          <el-input-number v-model="form.cableLength" :min="0.1" :step="0.5" :precision="1" style="width: 100%" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px">米</span>
        </el-form-item>
        <el-form-item label="连接位置" prop="connectionLocation">
          <el-input v-model="form.connectionLocation" placeholder="如 客厅设备柜" />
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
import { getDevices, getAllCableConnections, createCableConnection, updateCableConnection, deleteCableConnection } from '../api'

const deviceList = ref([])
const selectedDeviceId = ref('')
const connectionList = ref([])
const loading = ref(false)

const deviceTypeLabelMap = { SPEAKER: '音响', PROJECTOR: '投影仪', PLAYER: '播放器', AMPLIFIER: '功放' }
const deviceTypeTagMap = { SPEAKER: 'success', PROJECTOR: 'warning', PLAYER: 'primary', AMPLIFIER: 'danger' }
const deviceTypeColorMap = {
  SPEAKER: '#67C23A',
  PROJECTOR: '#E6A23C',
  PLAYER: '#409EFF',
  AMPLIFIER: '#F56C6C'
}

const getDeviceTypeLabel = (type) => deviceTypeLabelMap[type] || type
const getDeviceTagType = (type) => deviceTypeTagMap[type] || 'info'
const getDeviceTypeColor = (type) => deviceTypeColorMap[type] || '#909399'

const interfaceColors = {
  'HDMI 2.1': { start: '#409EFF', end: '#66B1FF' },
  'HDMI 2.0': { start: '#67C23A', end: '#85CE61' },
  '光纤': { start: '#E6A23C', end: '#EEBE77' },
  '同轴': { start: '#909399', end: '#A6A9AD' },
  'XLR平衡': { start: '#F56C6C', end: '#F78989' },
  'RCA': { start: '#606266', end: '#909399' },
  '喇叭线': { start: '#8E44AD', end: '#BB79D9' },
  'USB': { start: '#16A085', end: '#2BC4A5' }
}

const getInterfaceColor = (type) => interfaceColors[type] || { start: '#909399', end: '#C0C4CC' }

const interfaceTagTypes = {
  'HDMI 2.1': 'primary',
  'HDMI 2.0': 'success',
  '光纤': 'warning',
  '同轴': 'info',
  'XLR平衡': 'danger',
  'RCA': 'info',
  '喇叭线': '',
  'USB': 'success'
}

const getInterfaceTagType = (type) => interfaceTagTypes[type] || 'info'

const nodeWidth = 140
const nodeHeight = 70
const diagramWidth = 900
const diagramHeight = 500
const padding = 60

const deviceNodes = computed(() => {
  const devices = {}
  connectionList.value.forEach(conn => {
    if (conn.sourceDevice) devices[conn.sourceDevice.id] = conn.sourceDevice
    if (conn.targetDevice) devices[conn.targetDevice.id] = conn.targetDevice
  })

  const deviceArray = Object.values(devices)
  const columns = 3
  const rows = Math.ceil(deviceArray.length / columns)
  const colWidth = (diagramWidth - padding * 2 - nodeWidth) / (columns - 1 || 1)
  const rowHeight = (diagramHeight - padding * 2 - nodeHeight) / (rows - 1 || 1)

  return deviceArray.map((device, index) => {
    const col = index % columns
    const row = Math.floor(index / columns)
    return {
      ...device,
      x: padding + col * colWidth,
      y: padding + row * rowHeight
    }
  })
})

const getDeviceNode = (deviceId) => {
  return deviceNodes.value.find(d => d.id === deviceId)
}

const getConnectionPath = (conn) => {
  const sourceNode = getDeviceNode(conn.sourceDevice?.id)
  const targetNode = getDeviceNode(conn.targetDevice?.id)
  if (!sourceNode || !targetNode) return ''

  const sx = sourceNode.x + nodeWidth
  const sy = sourceNode.y + nodeHeight / 2
  const tx = targetNode.x
  const ty = targetNode.y + nodeHeight / 2

  const dx = tx - sx
  const controlOffset = Math.abs(dx) * 0.5

  return `M ${sx} ${sy} C ${sx + controlOffset} ${sy}, ${tx - controlOffset} ${ty}, ${tx} ${ty}`
}

const getLabelPosition = (conn) => {
  const sourceNode = getDeviceNode(conn.sourceDevice?.id)
  const targetNode = getDeviceNode(conn.targetDevice?.id)
  if (!sourceNode || !targetNode) return { x: 0, y: 0 }

  const sx = sourceNode.x + nodeWidth
  const sy = sourceNode.y + nodeHeight / 2
  const tx = targetNode.x
  const ty = targetNode.y + nodeHeight / 2

  return {
    x: (sx + tx) / 2,
    y: (sy + ty) / 2 - 8
  }
}

const formVisible = ref(false)
const formMode = ref('create')
const formRef = ref(null)
const form = ref({
  sourceDeviceId: '',
  targetDeviceId: '',
  interfaceType: '',
  cableLength: 1.5,
  connectionLocation: '',
  sourcePort: '',
  targetPort: '',
  remark: ''
})

const formRules = {
  sourceDeviceId: [{ required: true, message: '请选择源设备', trigger: 'change' }],
  targetDeviceId: [{ required: true, message: '请选择目标设备', trigger: 'change' }],
  interfaceType: [{ required: true, message: '请选择接口类型', trigger: 'change' }],
  cableLength: [{ required: true, message: '请输入线缆长度', trigger: 'blur' }]
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
    const res = await getAllCableConnections(deviceId)
    connectionList.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取连接记录失败')
  } finally {
    loading.value = false
  }
}

const openForm = (row = null) => {
  if (row && row.id) {
    formMode.value = 'edit'
    form.value = {
      id: row.id,
      sourceDeviceId: row.sourceDevice?.id || '',
      targetDeviceId: row.targetDevice?.id || '',
      interfaceType: row.interfaceType,
      cableLength: row.cableLength,
      connectionLocation: row.connectionLocation || '',
      sourcePort: row.sourcePort || '',
      targetPort: row.targetPort || '',
      remark: row.remark || ''
    }
  } else {
    formMode.value = 'create'
    form.value = {
      sourceDeviceId: '',
      targetDeviceId: '',
      interfaceType: '',
      cableLength: 1.5,
      connectionLocation: '',
      sourcePort: '',
      targetPort: '',
      remark: ''
    }
  }
  formVisible.value = true
}

const submitForm = async () => {
  try {
    await formRef.value.validate()
    const payload = { ...form.value }
    if (formMode.value === 'create') {
      await createCableConnection(payload)
      ElMessage.success('连接创建成功')
    } else {
      await updateCableConnection(form.value.id, payload)
      ElMessage.success('连接更新成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e) {
    if (e !== false) {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该连接记录？', '删除确认', { type: 'warning' })
    await deleteCableConnection(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) { /* cancelled */ }
}

onMounted(async () => {
  await fetchDevices()
  fetchData()
})
</script>

<style scoped>
.cable-connection { padding: 0; }
.top-bar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 20px; background: #fff; padding: 16px; border-radius: 8px;
}
.diagram-card { margin-bottom: 16px; }
.card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.connection-diagram {
  display: flex;
  justify-content: center;
  background: #f8f9fb;
  border-radius: 8px;
  padding: 20px;
  overflow-x: auto;
}
.diagram-svg {
  min-width: 800px;
}
.device-rect {
  stroke: rgba(0, 0, 0, 0.1);
  stroke-width: 1;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  transition: all 0.3s;
}
.device-node:hover .device-rect {
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.15));
}
.device-name {
  font-size: 13px;
  font-weight: 600;
  fill: #fff;
}
.device-type {
  font-size: 11px;
  fill: rgba(255, 255, 255, 0.85);
}
.connection-line {
  transition: stroke-width 0.3s;
  cursor: pointer;
}
.connection-line:hover {
  stroke-width: 5;
}
.connection-label {
  font-size: 11px;
  fill: #606266;
  font-weight: 500;
  paint-order: stroke;
  stroke: #fff;
  stroke-width: 3px;
}
.device-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.port-info {
  font-size: 12px;
  color: #909399;
}
</style>
