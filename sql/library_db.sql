CREATE DATABASE IF NOT EXISTS library_db;

USE library_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (

    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone` VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号（登录账号）',
    `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    `name` VARCHAR(50) NOT NULL COMMENT '用户姓名',
    `role` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '角色：0-普通用户，1-管理员',
    `status` TINYINT(1) NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-正常',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
    `version` INT(11) NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_role_status` (`role`,`status`) COMMENT '角色+状态索引，优化查询'
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 图书表
CREATE TABLE IF NOT EXISTS `book` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '图书ID',
    `name` VARCHAR(100) NOT NULL COMMENT '图书名称',
    `author` VARCHAR(50) NOT NULL COMMENT '作者',
    `isbn` VARCHAR(20) NOT NULL UNIQUE COMMENT 'ISBN编号（唯一）',
    `publisher` VARCHAR(50) DEFAULT NULL COMMENT '出版社',
    `publish_date` DATE DEFAULT NULL COMMENT '出版日期',
    `stock` INT(11) NOT NULL DEFAULT '0' COMMENT '当前库存',
    `total_stock` INT(11) NOT NULL DEFAULT '0' COMMENT '总库存',
    `category` VARCHAR(50) NOT NULL COMMENT '图书分类',
    `description` TEXT COMMENT '图书描述',
    `borrow_count` INT(11) NOT NULL DEFAULT '0' COMMENT '借阅次数',
    `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '图书封面URL',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
    `version` INT(11) NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_isbn` (`isbn`),
    KEY `idx_name` (`name`) COMMENT '图书名称索引，优化模糊查询',
    KEY `idx_category_stock` (`category`,`stock`) COMMENT '分类+库存索引，优化热门图书查询'
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 借阅记录表
CREATE TABLE IF NOT EXISTS `borrow_record` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT(20) NOT NULL COMMENT '用户ID',
    `book_id` BIGINT(20) NOT NULL COMMENT '图书ID',
    `borrow_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '借阅时间',
    `return_time` DATETIME DEFAULT NULL COMMENT '归还时间',
    `expected_return_time` DATETIME NOT NULL COMMENT '预计归还时间',
    `status` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '状态：1-借阅中，2-已归还，3-逾期未还',
    `overdue_days` INT(11) DEFAULT '0' COMMENT '逾期天数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
    `version` INT(11) NOT NULL DEFAULT '1' COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    -- 复合覆盖索引
    KEY `idx_br_deleted_userid_status_borrowtime_bookid` (`deleted`,`user_id`,`status`,`borrow_time` DESC,`book_id`) COMMENT '覆盖逻辑删除+用户ID+状态+借阅时间排序+图书ID，优化用户借阅记录分页查询',
    KEY `idx_br_deleted_bookid_status_borrowtime_userid` (`deleted`,`book_id`,`status`,`borrow_time` DESC,`user_id`) COMMENT '覆盖逻辑删除+图书ID+状态+借阅时间排序+用户ID，优化图书借阅记录分页查询',
    KEY `idx_br_deleted_expectedreturntime_status` (`deleted`,`expected_return_time`,`status`) COMMENT '覆盖逻辑删除+预计归还时间+状态，优化逾期提醒查询',
    -- 外键约束
    CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_borrow_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';


-- 图书续借记录表
CREATE TABLE IF NOT EXISTS book_renew (
    id BIGINT AUTO_INCREMENT COMMENT '续借记录ID（自增主键）' PRIMARY KEY,
    borrow_id BIGINT NOT NULL COMMENT '关联借阅记录ID（关联borrow_record表的id）',
    user_id BIGINT NOT NULL COMMENT '续借用户ID（关联user表的id）',
    book_id BIGINT NOT NULL COMMENT '续借图书ID（关联book表的id）',
    original_expected_return_time DATETIME NOT NULL COMMENT '原预计归还时间',
    new_expected_return_time DATETIME NOT NULL COMMENT '续借后预计归还时间',
    renew_time DATETIME NOT NULL COMMENT '续借时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    -- 索引优化：支持续借次数统计、按借阅记录查询
    INDEX idx_renew_user_book (user_id, book_id),
    INDEX idx_renew_borrow (borrow_id),
    -- 外键约束：确保关联数据完整性
    CONSTRAINT fk_renew_borrow FOREIGN KEY (borrow_id) REFERENCES borrow_record (id) ON DELETE CASCADE,
    CONSTRAINT fk_renew_user FOREIGN KEY (user_id) REFERENCES USER (id) ON DELETE RESTRICT,
    CONSTRAINT fk_renew_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE RESTRICT
    ) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='图书续借记录表';



-- 测试数据：管理员账号（手机号13800138000，密码123456，BCrypt加密后）
INSERT INTO `user` (phone, password, name, role) VALUES
    ('13800138000', '$2a$10$7q6x5c4e3k8z1m0a2h9wzb682f3t5s9h5v1b9t9z1y', '管理员', 1);
-- 测试数据：普通用户
INSERT INTO `user` (phone, password, name, role) VALUES
    ('13900139000', '$2a$10$7q6x5c4e3k8z1m0a2h9wzb682f3t5s9h5v1b9t9z1y', '测试用户', 0);
-- 测试数据：图书
INSERT INTO `book` (name, author, isbn, stock, total_stock, category) VALUES
    ('Java编程思想', 'Bruce Eckel', '9787111213826', 10, 10, '计算机');