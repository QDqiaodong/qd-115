import { createRouter, createWebHistory } from 'vue-router'
import DeviceList from '../views/DeviceList.vue'
import DeviceLocation from '../views/DeviceLocation.vue'
import UsageRecord from '../views/UsageRecord.vue'
import RepairRecord from '../views/RepairRecord.vue'
import MaintenanceRecord from '../views/MaintenanceRecord.vue'
import MaintenanceChecklist from '../views/MaintenanceChecklist.vue'
import StatusWall from '../views/StatusWall.vue'
import CostStatistics from '../views/CostStatistics.vue'
import CableConnection from '../views/CableConnection.vue'
import AmplifierCalibration from '../views/AmplifierCalibration.vue'

const routes = [
  { path: '/', redirect: '/devices' },
  { path: '/devices', name: 'DeviceList', component: DeviceList },
  { path: '/location', name: 'DeviceLocation', component: DeviceLocation },
  { path: '/usage', name: 'UsageRecord', component: UsageRecord },
  { path: '/repair', name: 'RepairRecord', component: RepairRecord },
  { path: '/maintenance', name: 'MaintenanceRecord', component: MaintenanceRecord },
  { path: '/maintenance-checklist', name: 'MaintenanceChecklist', component: MaintenanceChecklist },
  { path: '/status-wall', name: 'StatusWall', component: StatusWall },
  { path: '/cost-statistics', name: 'CostStatistics', component: CostStatistics },
  { path: '/cable-connections', name: 'CableConnection', component: CableConnection },
  { path: '/amplifier-calibration', name: 'AmplifierCalibration', component: AmplifierCalibration }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
