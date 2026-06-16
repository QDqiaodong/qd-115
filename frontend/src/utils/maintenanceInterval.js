const INTERVAL_DAYS = {
  SPEAKER_CLEANING: 30,
  SPEAKER_CABLE: 90,
  SPEAKER_FIRMWARE: 180,
  SPEAKER_OTHER: 60,
  PROJECTOR_CLEANING: 15,
  PROJECTOR_CABLE: 60,
  PROJECTOR_FIRMWARE: 90,
  PROJECTOR_OTHER: 45,
  PLAYER_CLEANING: 30,
  PLAYER_CABLE: 90,
  PLAYER_FIRMWARE: 120,
  PLAYER_OTHER: 60,
  AMPLIFIER_CLEANING: 30,
  AMPLIFIER_CABLE: 60,
  AMPLIFIER_FIRMWARE: 180,
  AMPLIFIER_OTHER: 60
}

const MAINTENANCE_TYPE_LABELS = {
  CLEANING: '机身清洁',
  CABLE: '线路整理',
  FIRMWARE: '固件升级',
  OTHER: '其他'
}

const DEVICE_TYPE_LABELS = {
  SPEAKER: '音响',
  PROJECTOR: '投影仪',
  PLAYER: '播放器',
  AMPLIFIER: '功放'
}

function key(deviceType, maintenanceType) {
  return deviceType + '_' + maintenanceType
}

export function getIntervalDays(deviceType, maintenanceType) {
  return INTERVAL_DAYS[key(deviceType, maintenanceType)] || 90
}

export function calculateNextWindow(deviceType, lastMaintenanceTime, maintenanceType) {
  if (!lastMaintenanceTime) return null
  const lastTime = new Date(lastMaintenanceTime)
  if (isNaN(lastTime.getTime())) return null
  const days = getIntervalDays(deviceType, maintenanceType)
  const nextTime = new Date(lastTime.getTime() + days * 24 * 60 * 60 * 1000)
  return nextTime
}

export function calculateNextWindows(deviceType, lastMaintenanceTime) {
  const types = ['CLEANING', 'CABLE', 'FIRMWARE', 'OTHER']
  const now = new Date()
  const windows = {}
  for (const type of types) {
    const interval = getIntervalDays(deviceType, type)
    const nextTime = calculateNextWindow(deviceType, lastMaintenanceTime, type)
    const entry = {
      maintenanceType: type,
      maintenanceTypeLabel: MAINTENANCE_TYPE_LABELS[type],
      intervalDays: interval,
      nextTime: nextTime ? formatDateTime(nextTime) : null,
      nextTimeRaw: nextTime
    }
    if (nextTime) {
      const diffMs = nextTime.getTime() - now.getTime()
      const diffDays = Math.ceil(diffMs / (24 * 60 * 60 * 1000))
      entry.overdue = diffDays < 0
      entry.daysUntil = diffDays
      entry.urgent = diffDays >= 0 && diffDays <= 7
    }
    windows[type] = entry
  }
  return windows
}

export function getEarliestUrgentWindow(windows) {
  if (!windows) return null
  let earliest = null
  for (const type in windows) {
    const w = windows[type]
    if (w.nextTime && (w.overdue || w.urgent)) {
      if (!earliest || (w.daysUntil !== undefined && w.daysUntil < earliest.daysUntil)) {
        earliest = w
      }
    }
  }
  return earliest
}

export function getMaintenanceTypeLabel(type) {
  return MAINTENANCE_TYPE_LABELS[type] || type
}

export function getDeviceTypeLabel(type) {
  return DEVICE_TYPE_LABELS[type] || type
}

export function getAllIntervals() {
  return { ...INTERVAL_DAYS }
}

export function getIntervalsByDeviceType(deviceType) {
  const types = ['CLEANING', 'CABLE', 'FIRMWARE', 'OTHER']
  const result = {}
  for (const type of types) {
    result[type] = getIntervalDays(deviceType, type)
  }
  return result
}

function formatDateTime(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
