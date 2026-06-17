<template>
  <div class="repair-timeline" v-loading="loading">
    <div v-if="timeline.length === 0 && !loading" class="empty-tip">
      <el-empty description="暂无检修记录" :image-size="80" />
    </div>
    <el-timeline v-else>
      <el-timeline-item
        v-for="item in timeline"
        :key="item.id"
        :timestamp="formatTime(item.repairTime)"
        placement="top"
        :type="item.repeatedFault ? 'danger' : 'primary'"
        :hollow="!item.repeatedFault"
        size="large"
      >
        <el-card shadow="hover" class="timeline-card" :class="{ 'repeated-fault': item.repeatedFault }">
          <div class="timeline-header">
            <span class="symptom-text">{{ item.symptom }}</span>
            <el-tag v-if="item.repeatedFault" type="danger" size="small" effect="dark" class="repeat-tag">
              反复故障
            </el-tag>
          </div>
          <div class="timeline-body">
            <div class="detail-row" v-if="item.cause">
              <span class="detail-label">故障原因：</span>
              <span class="detail-value">{{ item.cause }}</span>
            </div>
            <div class="detail-row" v-if="item.fixMethod">
              <span class="detail-label">修复方法：</span>
              <span class="detail-value">{{ item.fixMethod }}</span>
            </div>
            <div class="detail-row" v-if="item.repairPerson">
              <span class="detail-label">维修人员：</span>
              <span class="detail-value">{{ item.repairPerson }}</span>
            </div>
            <div class="detail-row" v-if="item.cost != null">
              <span class="detail-label">维修费用：</span>
              <span class="detail-value cost">¥{{ Number(item.cost).toFixed(2) }}</span>
            </div>
            <div class="detail-row" v-if="item.remark">
              <span class="detail-label">备注：</span>
              <span class="detail-value">{{ item.remark }}</span>
            </div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getRepairTimeline } from '../api'

const props = defineProps({
  deviceId: { type: [Number, String], default: null }
})

const timeline = ref([])
const loading = ref(false)

const formatTime = (val) => {
  if (!val) return ''
  if (typeof val === 'string') {
    if (val.includes('T')) {
      const d = new Date(val)
      if (!isNaN(d.getTime())) {
        const pad = (n) => String(n).padStart(2, '0')
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
      }
    }
    return val.replace(/:\d{2}$/, '')
  }
  return String(val)
}

const fetchTimeline = async () => {
  if (!props.deviceId) {
    timeline.value = []
    return
  }
  loading.value = true
  try {
    const res = await getRepairTimeline(props.deviceId)
    timeline.value = Array.isArray(res) ? res : []
  } catch (e) {
    ElMessage.error('获取诊断时间轴失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.deviceId, fetchTimeline, { immediate: true })
</script>

<style scoped>
.repair-timeline {
  padding: 12px 0;
}
.empty-tip {
  padding: 40px 0;
}
.timeline-card {
  border-radius: 8px;
}
.timeline-card.repeated-fault {
  border-left: 3px solid #f56c6c;
}
.timeline-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.symptom-text {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.repeat-tag {
  font-size: 11px;
}
.timeline-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-row {
  font-size: 13px;
  line-height: 1.6;
  display: flex;
}
.detail-label {
  color: #909399;
  white-space: nowrap;
  min-width: 80px;
}
.detail-value {
  color: #606266;
  word-break: break-all;
}
.detail-value.cost {
  color: #f56c6c;
  font-weight: 600;
}
</style>
