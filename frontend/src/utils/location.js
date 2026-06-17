import { House, Film, Reading, OfficeBuilding, Location } from '@element-plus/icons-vue'

export const STANDARD_LOCATIONS = [
  '客厅',
  '影音室',
  '卧室',
  '书房',
  '厨房',
  '餐厅',
  '卫生间',
  '阳台',
  '储物间',
  '玄关'
]

const LOCATION_ALIAS_MAP = {
  '客厅': ['客厅', '客厅电视墙', '客厅吊顶', '客厅设备柜', '客厅侧墙', '客厅墙角', '客厅沙发旁', '主客厅', '大客厅'],
  '影音室': ['影音室', '家庭影院', '放映室', '视听室', '影视厅'],
  '卧室': ['卧室', '主卧', '次卧', '主卧室', '次卧室', '卧室床头柜', '卧室书桌', '卧室电视柜', '小卧室', '大卧室'],
  '书房': ['书房', '书房间', '工作间', '办公间', '办公室'],
  '厨房': ['厨房', '厨房间', '中厨', '西厨'],
  '餐厅': ['餐厅', '饭厅', '餐区'],
  '卫生间': ['卫生间', '洗手间', '厕所', '浴室', '主卫', '客卫'],
  '阳台': ['阳台', '南阳台', '北阳台', '生活阳台'],
  '储物间': ['储物间', '储藏室', '杂物间', '仓库'],
  '玄关': ['玄关', '门厅', '入口', '入户花园']
}

export const normalizeLocation = (location) => {
  if (!location || typeof location !== 'string') {
    return ''
  }
  const trimmed = location.trim()
  if (!trimmed) return ''
  
  for (const [standard, aliases] of Object.entries(LOCATION_ALIAS_MAP)) {
    if (aliases.some(alias => trimmed.includes(alias) || alias.includes(trimmed))) {
      return standard
    }
  }
  
  return trimmed
}

export const getLocationIcon = (location) => {
  const iconMap = {
    '客厅': House,
    '影音室': Film,
    '卧室': Reading,
    '书房': Reading,
    '厨房': OfficeBuilding,
    '餐厅': OfficeBuilding,
    '卫生间': Location,
    '阳台': Location,
    '储物间': OfficeBuilding,
    '玄关': Location
  }
  const normalized = normalizeLocation(location)
  return iconMap[normalized] || Location
}

export const getLocationIconComponent = (location) => {
  return getLocationIcon(location)
}

export const sortLocations = (locations) => {
  const standardOrder = STANDARD_LOCATIONS.filter(loc => locations.includes(loc))
  const otherLocations = locations.filter(loc => !STANDARD_LOCATIONS.includes(loc)).sort()
  return [...standardOrder, ...otherLocations]
}

export const groupDevicesByLocation = (devices) => {
  const groups = {}
  const unassigned = []

  devices.forEach(device => {
    const loc = device.location && device.location.trim() ? device.location.trim() : null
    if (loc) {
      const normalized = normalizeLocation(loc)
      if (!groups[normalized]) groups[normalized] = []
      groups[normalized].push(device)
    } else {
      unassigned.push(device)
    }
  })

  if (unassigned.length > 0) {
    groups['未分配位置'] = unassigned
  }

  const sorted = {}
  const knownLocations = STANDARD_LOCATIONS.filter(loc => groups[loc])
  const otherLocations = Object.keys(groups).filter(loc => !STANDARD_LOCATIONS.includes(loc) && loc !== '未分配位置')

  knownLocations.forEach(loc => { sorted[loc] = groups[loc] })
  otherLocations.sort().forEach(loc => { sorted[loc] = groups[loc] })
  if (groups['未分配位置']) sorted['未分配位置'] = groups['未分配位置']

  return sorted
}

export const getNormalizedLocationList = (devices) => {
  const locations = [...new Set(
    devices
      .map(d => d.location)
      .filter(Boolean)
      .map(loc => normalizeLocation(loc))
      .filter(Boolean)
  )]
  return sortLocations(locations)
}
