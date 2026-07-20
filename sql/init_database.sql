-- ==========================================
-- 智慧图书管理系统
-- 包含：建库建表 + 所有字段修复 + 测试数据
-- ==========================================

-- ==========================================
-- 1. 用户服务数据库
-- ==========================================
CREATE DATABASE IF NOT EXISTS library_user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE library_user_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `phone` VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户 1-管理员',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `max_borrow_count` INT NOT NULL DEFAULT 5 COMMENT '最大借阅数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 插入测试用户（密码均为：123456，BCrypt加密）
INSERT INTO `user` (`username`, `password`, `phone`, `role`, `status`, `max_borrow_count`) VALUES
('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjKfKMOqGgK7QKQxVwzZb9X8VqJqMvi', '13800138000', 1, 1, 20),
('test_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjKfKMOqGgK7QKQxVwzZb9X8VqJqMvi', '13800138001', 0, 1, 5);


-- ==========================================
-- 2. 图书服务数据库
-- ==========================================
CREATE DATABASE IF NOT EXISTS library_book_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE library_book_db;

-- 图书表
CREATE TABLE IF NOT EXISTS `book` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图书ID',
    `book_name` VARCHAR(100) NOT NULL COMMENT '图书名称',
    `author` VARCHAR(50) NOT NULL COMMENT '作者',
    `isbn` VARCHAR(20) NOT NULL COMMENT 'ISBN',
    `publisher` VARCHAR(100) COMMENT '出版社',
    `publish_date` DATE COMMENT '出版日期',
    `category_id` BIGINT COMMENT '分类ID',
    `description` TEXT COMMENT '图书描述',
    `stock_count` INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    `total_stock` INT NOT NULL DEFAULT 0 COMMENT '总库存',
    `borrow_count` INT NOT NULL DEFAULT 0 COMMENT '已借出数量',
    `cover_url` VARCHAR(255) COMMENT '图书封面URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    KEY `idx_book_name` (`book_name`),
    KEY `idx_author` (`author`),
    KEY `idx_isbn` (`isbn`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 插入测试图书
INSERT INTO `book` (`book_name`, `author`, `isbn`, `publisher`, `stock_count`, `total_stock`, `borrow_count`, `status`) VALUES
('深入理解Java虚拟机', '周志明', '9787111641247', '机械工业出版社', 10, 10, 0, 1),
('Spring Boot实战', '汪云飞', '9787121301706', '电子工业出版社', 8, 8, 0, 1),
('Redis设计与实现', '黄健宏', '9787111464747', '机械工业出版社', 6, 6, 0, 1);


-- ==========================================
-- 3. 借阅服务数据库
-- ==========================================
CREATE DATABASE IF NOT EXISTS library_borrow_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE library_borrow_db;

-- 借阅记录表
CREATE TABLE IF NOT EXISTS `borrow_record` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '借阅记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `borrow_time` DATETIME NOT NULL COMMENT '借阅时间',
    `expected_return_time` DATETIME COMMENT '应归还时间',
    `actual_return_time` DATETIME COMMENT '实际归还时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-借阅中 2-已归还 3-逾期未还',
    `renew_count` INT NOT NULL DEFAULT 0 COMMENT '续借次数',
    `overdue_days` INT DEFAULT 0 COMMENT '逾期天数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- 续借记录表
CREATE TABLE IF NOT EXISTS `book_renew` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '续借记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `borrow_record_id` BIGINT NOT NULL COMMENT '借阅记录ID',
    `original_expected_return_time` DATETIME COMMENT '原预计归还时间',
    `new_expected_return_time` DATETIME COMMENT '续借后预计归还时间',
    `renew_time` DATETIME NOT NULL COMMENT '续借时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_borrow_record_id` (`borrow_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='续借记录表';

-- 借阅事件记录表（用于RabbitMQ消费者入库）
CREATE TABLE IF NOT EXISTS `borrow_event` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '事件ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `book_id` BIGINT COMMENT '图书ID',
    `borrow_record_id` BIGINT COMMENT '借阅记录ID',
    `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型：BORROW-借阅, RETURN-归还, RENEW-续借, OVERDUE-逾期',
    `event_data` TEXT COMMENT '事件详情（JSON格式）',
    `message_id` VARCHAR(100) COMMENT 'RabbitMQ消息ID',
    `consume_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-已处理 2-处理失败',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `idx_user_id` (`user_id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_consume_time` (`consume_time`),
    KEY `idx_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅事件记录表';
