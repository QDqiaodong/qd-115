import { createRouter, createWebHistory } from 'vue-router'
import DeviceList from '../views/DeviceList.vue'
import UsageRecord from '../views/UsageRecord.vue'
import RepairRecord from '../views/RepairRecord.vue'
import MaintenanceRecord from '../views/MaintenanceRecord.vue'

const routes = [
  { path: '/', redirect: '/devices' },
  { path: '/devices', name: 'DeviceList', component: DeviceList },
  { path: '/usage', name: 'UsageRecord', component: UsageRecord },
  { path: '/repair', name: 'RepairRecord', component: RepairRecord },
  { path: '/maintenance', name: 'MaintenanceRecord', component: MaintenanceRecord }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
