-- 家用影音设备运维台账 - 数据库初始化脚本
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '设备名称',
    model VARCHAR(100) NOT NULL COMMENT '型号',
    device_type VARCHAR(20) NOT NULL COMMENT '设备类型',
    purchase_date DATE NOT NULL COMMENT '购入时间',
    location VARCHAR(100) COMMENT '摆放位置',
    hardware_specs TEXT COMMENT '硬件参数',
    status VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '设备状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_device_type (device_type),
    INDEX idx_status (status),
    INDEX idx_purchase_date (purchase_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备基础档案';

CREATE TABLE IF NOT EXISTS usage_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '设备ID',
    usage_date DATE NOT NULL COMMENT '使用日期',
    duration_minutes INT NOT NULL COMMENT '使用时长(分钟)',
    scenario VARCHAR(50) COMMENT '使用场景',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_usage_date (usage_date),
    INDEX idx_device_date (device_id, usage_date),
    CONSTRAINT fk_usage_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='使用时长记录';

CREATE TABLE IF NOT EXISTS repair_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '设备ID',
    repair_time DATETIME NOT NULL COMMENT '检修时间',
    symptom TEXT NOT NULL COMMENT '异常现象',
    cause TEXT COMMENT '故障原因',
    fix_method TEXT COMMENT '修复方式',
    repair_person VARCHAR(50) COMMENT '检修人',
    cost DECIMAL(10,2) COMMENT '费用',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_repair_time (repair_time),
    INDEX idx_device_repair (device_id, repair_time),
    CONSTRAINT fk_repair_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='故障检修记录';

CREATE TABLE IF NOT EXISTS maintenance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '设备ID',
    maintenance_time DATETIME NOT NULL COMMENT '养护时间',
    maintenance_type VARCHAR(20) NOT NULL COMMENT '养护类型',
    content TEXT NOT NULL COMMENT '养护内容',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_maintenance_type (maintenance_type),
    INDEX idx_maintenance_time (maintenance_time),
    INDEX idx_device_maintenance (device_id, maintenance_type),
    CONSTRAINT fk_maintenance_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常养护记录';

CREATE TABLE IF NOT EXISTS firmware_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id BIGINT NOT NULL COMMENT '设备ID',
    firmware_version VARCHAR(50) NOT NULL COMMENT '固件版本号',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    description TEXT NOT NULL COMMENT '更新说明/处理说明',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_device_id (device_id),
    INDEX idx_firmware_version (firmware_version),
    INDEX idx_update_time (update_time),
    INDEX idx_device_update (device_id, update_time),
    CONSTRAINT fk_firmware_device FOREIGN KEY (device_id) REFERENCES device(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='固件版本记录';

-- 预置设备数据
INSERT INTO device (name, model, device_type, purchase_date, location, hardware_specs, status) VALUES
('家庭影院主音响', 'Bose 901 Series VI', 'SPEAKER', '2023-03-15', '客厅电视墙', '频响范围 20Hz-20kHz, 功率 400W, 阻抗 8Ω', 'NORMAL'),
('4K激光投影仪', 'XGIMI Aura 4K', 'PROJECTOR', '2023-06-20', '客厅吊顶', '4K UHD分辨率, 2400ANSI流明, 激光光源, 投射比 0.25:1', 'NORMAL'),
('蓝光播放器', 'OPPO UDP-203', 'PLAYER', '2022-11-08', '客厅设备柜', '4K UHD蓝光播放, HDR10+, Dolby Vision, SACD播放', 'NORMAL'),
('多声道功放', 'Denon AVR-X6700H', 'AMPLIFIER', '2023-01-10', '客厅设备柜', '11.2声道, 每声道140W, 8K HDMI, Dolby Atmos, DTS:X', 'NORMAL'),
('环绕音响组', 'KEF Q50a', 'SPEAKER', '2023-05-22', '客厅侧墙', 'Dolby Atmos Enabled, 频响 80Hz-40kHz, 100W', 'MAINTENANCE'),
('卧室便携投影', 'Anker Nebula Capsule II', 'PROJECTOR', '2024-02-14', '卧室床头柜', '720p分辨率, 200ANSI流明, 内置Android TV', 'FAULTY'),
('流媒体播放器', 'Apple TV 4K', 'PLAYER', '2023-09-30', '客厅设备柜', 'A15芯片, 64GB存储, HDMI 2.1, Wi-Fi 6, Thread', 'NORMAL'),
('书架音响系统', 'Sonos Five', 'SPEAKER', '2024-01-05', '书房', '六驱动器, 三高音, 三中频, Wi-Fi, AirPlay 2', 'NORMAL');

-- 预置使用记录
INSERT INTO usage_record (device_id, usage_date, duration_minutes, scenario, remark) VALUES
(1, '2024-06-10', 180, '观影', '观看《星际穿越》蓝光碟'),
(2, '2024-06-10', 180, '观影', '观看《星际穿越》蓝光碟'),
(3, '2024-06-10', 180, '观影', '播放蓝光碟'),
(4, '2024-06-10', 180, '观影', '5.1声道输出'),
(1, '2024-06-11', 90, '音乐', '播放古典音乐专辑'),
(7, '2024-06-11', 120, '影视', 'Apple TV+剧集'),
(8, '2024-06-12', 60, '音乐', '书房背景音乐'),
(2, '2024-06-13', 240, '观影', '家庭聚会观看动作片'),
(4, '2024-06-13', 240, '观影', '7.1声道环绕输出'),
(7, '2024-06-14', 75, '游戏', '连接PS5游戏');

-- 预置检修记录
INSERT INTO repair_record (device_id, repair_time, symptom, cause, fix_method, repair_person, cost, remark) VALUES
(6, '2024-05-20 14:30:00', '投影画面模糊, 色彩失真', 'DMD芯片老化', '更换DMD芯片模组', '专业维修中心', 850.00, '保修期内免费更换'),
(2, '2024-04-15 10:00:00', '开机无图像输出', 'HDMI接口松脱', '重新固定HDMI接口, 清洁触点', '自行处理', 0.00, '日常维护'),
(5, '2024-06-01 09:00:00', '环绕音效失真, 单侧无声', '分频器电容损坏', '更换分频器组件', '授权维修点', 320.00, '正在维修中');

-- 预置养护记录
INSERT INTO maintenance_record (device_id, maintenance_time, maintenance_type, content, operator, remark) VALUES
(1, '2024-05-01 10:00:00', 'CLEANING', '擦拭音响网罩, 清理扬声器灰尘, 检查连接线', '户主', '定期月度养护'),
(2, '2024-05-15 14:00:00', 'FIRMWARE', '升级投影仪固件至v3.2.1版本, 优化4K HDR显示效果', '户主', '官方固件推送更新'),
(3, '2024-04-20 16:00:00', 'CLEANING', '清洁光盘仓激光头, 擦拭机身外壳', '户主', '蓝光机季度养护'),
(4, '2024-05-20 11:00:00', 'CABLE', '整理功放背部HDMI线缆, 重新插拔并绑扎', '户主', '新增设备后整理线缆'),
(7, '2024-06-01 09:00:00', 'FIRMWARE', '升级tvOS至17.5版本, 更新应用程序', '户主', '系统自动更新'),
(8, '2024-06-05 20:00:00', 'CLEANING', '清理音响出风口灰尘, 检查散热情况', '户主', '夏季高温前检查'),
(2, '2024-06-10 15:00:00', 'CLEANING', '清洁投影镜头, 使用专用镜头布擦拭', '户主', '镜头出现轻微指纹');

-- 预置固件版本记录
INSERT INTO firmware_record (device_id, firmware_version, update_time, description, operator, remark) VALUES
(2, 'v3.0.0', '2023-06-20 10:00:00', '出厂默认固件版本, 支持基础4K投影功能', '厂商', '出厂预装'),
(2, 'v3.1.0', '2023-09-10 14:30:00', '新增自动梯形校正功能, 优化对焦速度', '户主', '官方固件推送'),
(2, 'v3.2.0', '2024-02-15 16:00:00', '支持Dolby Vision, 优化HDR10+显示效果', '户主', '重大版本更新'),
(2, 'v3.2.1', '2024-05-15 14:00:00', '修复已知Bug, 优化散热控制策略', '户主', '官方固件推送更新'),
(3, 'v1.0.0', '2022-11-08 09:00:00', '出厂默认固件版本, 支持4K UHD蓝光播放', '厂商', '出厂预装'),
(3, 'v1.1.5', '2023-03-20 11:00:00', '新增SACD播放支持, 优化音频解码', '户主', '官方固件更新'),
(3, 'v1.2.0', '2023-08-05 15:30:00', '支持Dolby Atmos源码输出, 修复部分光盘兼容性问题', '户主', '重要功能更新'),
(4, 'v2.0.0', '2023-01-10 10:00:00', '出厂默认固件版本, 支持8K HDMI输入', '厂商', '出厂预装'),
(4, 'v2.1.0', '2023-06-18 14:00:00', '新增eARC支持, 优化Dirac Live校准', '户主', '官方固件推送'),
(4, 'v2.2.1', '2024-04-10 09:30:00', '修复HDMI 2.1兼容性问题, 优化环绕声效果', '户主', '稳定性更新'),
(7, 'tvOS 16.0', '2023-09-30 10:00:00', '出厂系统版本, 支持4K HDR输出', '厂商', '出厂预装'),
(7, 'tvOS 17.0', '2023-11-15 16:00:00', '升级至tvOS 17, 新增增强对话功能', '户主', '系统大版本更新'),
(7, 'tvOS 17.5', '2024-06-01 09:00:00', '优化性能, 修复安全漏洞, 更新应用程序', '户主', '系统自动更新'),
(6, 'v1.5.0', '2024-02-14 10:00:00', '出厂默认固件版本', '厂商', '出厂预装'),
(6, 'v1.5.2', '2024-04-01 14:00:00', '优化亮度输出, 修复自动对焦问题', '户主', '官方固件更新');
