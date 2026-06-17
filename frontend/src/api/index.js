import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => config,
  error => Promise.reject(error)
)

request.interceptors.response.use(
  response => response.data,
  error => {
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

export const getDevices = () => request.get('/devices')
export const getDevice = (id) => request.get(`/devices/${id}`)
export const getDeviceMaintenanceSummaries = () => request.get('/devices/maintenance-summaries')
export const createDevice = (data) => request.post('/devices', data)
export const updateDevice = (id, data) => request.put(`/devices/${id}`, data)
export const deleteDevice = (id) => request.delete(`/devices/${id}`)
export const getDevicesByType = (type) => request.get('/devices/type/' + type)
export const getDevicesByStatus = (status) => request.get('/devices/status/' + status)
export const batchUpdateDevices = (data) => request.put('/devices/batch', data)
export const getAllowedTransitions = (id) => request.get(`/devices/${id}/allowed-transitions`)
export const transitionDeviceStatus = (id, targetStatus) => request.put(`/devices/${id}/transition`, { targetStatus })
export const getStatusWall = () => request.get('/devices/status-wall')
export const getHealthScore = (id) => request.get(`/devices/health-score/${id}`)
export const getLampLife = (id) => request.get(`/devices/${id}/lamp-life`)

export const getUsageByDevice = (deviceId) => request.get(`/usage/device/${deviceId}`)
export const createUsage = (data) => request.post('/usage', data)
export const updateUsage = (id, data) => request.put(`/usage/${id}`, data)
export const deleteUsage = (id) => request.delete(`/usage/${id}`)
export const getUsageStats = (deviceId) => request.get(`/usage/device/${deviceId}/stats`)
export const getUsageHeatmap = (deviceId, startDate, endDate) => {
  const params = new URLSearchParams()
  if (deviceId) params.append('deviceId', deviceId)
  params.append('startDate', startDate)
  params.append('endDate', endDate)
  return request.get(`/usage/heatmap?${params.toString()}`)
}
export const getUsageDailyDetails = (deviceId, date) => {
  const params = new URLSearchParams()
  if (deviceId) params.append('deviceId', deviceId)
  params.append('date', date)
  return request.get(`/usage/daily?${params.toString()}`)
}
export const getUsageSceneDistribution = (deviceType, location) => {
  const params = new URLSearchParams()
  if (deviceType) params.append('deviceType', deviceType)
  if (location) params.append('location', location)
  return request.get(`/usage/scene-distribution?${params.toString()}`)
}
export const getUsageLocations = () => request.get('/usage/locations')

export const getRepairByDevice = (deviceId) => request.get(`/repair/device/${deviceId}`)
export const getRepairTimeline = (deviceId) => request.get(`/repair/timeline/${deviceId}`)
export const getRepairCostStatistics = (params) => request.get('/repair/cost-statistics', { params })
export const createRepair = (data) => request.post('/repair', data)
export const updateRepair = (id, data) => request.put(`/repair/${id}`, data)
export const deleteRepair = (id) => request.delete(`/repair/${id}`)

export const getMaintenanceByDevice = (deviceId) => request.get(`/maintenance/device/${deviceId}`)
export const createMaintenance = (data) => request.post('/maintenance', data)
export const updateMaintenance = (id, data) => request.put(`/maintenance/${id}`, data)
export const deleteMaintenance = (id) => request.delete(`/maintenance/${id}`)
export const getMaintenanceByType = (type) => request.get('/maintenance/type/' + type)

export const getMaintenanceIntervals = () => request.get('/maintenance-interval/intervals')
export const getMaintenanceIntervalsByType = (deviceType) => request.get(`/maintenance-interval/intervals/${deviceType}`)
export const getNextMaintenanceWindow = (deviceType, lastMaintenanceTime, maintenanceType) => {
  const params = new URLSearchParams()
  params.append('deviceType', deviceType)
  params.append('lastMaintenanceTime', lastMaintenanceTime)
  params.append('maintenanceType', maintenanceType)
  return request.get(`/maintenance-interval/next-window?${params.toString()}`)
}
export const getNextMaintenanceWindows = (deviceType, lastMaintenanceTime) => {
  const params = new URLSearchParams()
  params.append('deviceType', deviceType)
  if (lastMaintenanceTime) params.append('lastMaintenanceTime', lastMaintenanceTime)
  return request.get(`/maintenance-interval/next-windows?${params.toString()}`)
}

export const getFirmwareByDevice = (deviceId) => request.get(`/firmware/device/${deviceId}`)
export const getLatestFirmwareByDevice = (deviceId) => request.get(`/firmware/device/${deviceId}/latest`)
export const getFirmware = (id) => request.get(`/firmware/${id}`)
export const createFirmware = (data) => request.post('/firmware', data)
export const updateFirmware = (id, data) => request.put(`/firmware/${id}`, data)
export const deleteFirmware = (id) => request.delete(`/firmware/${id}`)

export default request
