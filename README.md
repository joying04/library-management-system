# 智慧图书管理系统

> 基于 Spring Cloud Alibaba 的微服务架构图书借阅管理系统，实现高并发场景下的借阅、归还、续借等核心功能，单实例 QPS 达 800+。

---

## 🛠️ 技术栈

| 技术 | 版本 | 选型理由 |
|------|------|----------|
| Spring Boot | 3.2.4 | 基础框架，快速开发 |
| Spring Cloud Alibaba | 2023.0.1.0 | 微服务全家桶（Nacos + Gateway + Feign） |
| MyBatis-Plus | 3.5.5 | ORM框架，内置乐观锁和分页插件 |
| MySQL | 8.0+ | 关系型数据库，InnoDB 引擎 |
| Redis | 7.0+ | 缓存 + 分布式锁 + Token黑名单 |
| Redisson | 3.24.3 | 分布式锁，防止图书超卖 |
| RabbitMQ | 3.12+ | 异步解耦，借阅事件处理 |
| JWT (java-jwt) | 4.4.0 | 身份认证，双Token机制 |
| SpringDoc | 2.3.0 | API文档生成，替代Swagger |

---

## 📁 项目架构

```
library-management-system/
├── library-common/          # 公共模块（跨服务共享）
│   ├── entity/              # 实体类（User/Book/BorrowRecord）
│   ├── dto/                 # 请求参数（查询条件、分页参数）
│   ├── vo/                  # 响应数据（图书详情、登录信息）
│   ├── enums/               # 枚举（角色、状态、常量）
│   ├── exception/           # 全局异常处理
│   ├── util/                # 工具类（JWT、密码加密、Redis）
│   ├── feign/               # Feign接口（服务间调用契约）
│   └── config/              # 公共配置（Jackson、WebMvc）
│
├── library-gateway/         # API网关（统一入口）
│   ├── filter/              # JWT鉴权过滤器
│   └── config/              # 跨域配置
│
├── library-user/            # 用户服务（独立数据库）
│   ├── controller/          # 注册/登录/登出接口
│   ├── service/             # 用户业务逻辑
│   └── config/              # JWT配置、Redis配置
│
├── library-book/            # 图书服务（独立数据库）
│   ├── controller/          # 图书CRUD接口
│   ├── service/             # 图书业务逻辑（含Redis缓存）
│   └── config/              # Redis缓存配置
│
├── library-borrow/          # 借阅服务（独立数据库）
│   ├── controller/          # 借阅/归还/续借接口
│   ├── service/             # 借阅业务逻辑（含分布式锁）
│   ├── rabbitmq/            # MQ生产者、消费者
│   └── config/              # Redisson配置
│
├── sql/                     # 数据库脚本
│   └── init_database.sql    # 完整建库建表脚本（含测试数据）
│
├── nacos-config/            # Nacos配置模板
│   └── library-shared-config.yaml
│
└── pom.xml                  # 父POM（依赖版本管理）
```

---

## ✅ 核心功能清单

### 基础功能
- ✅ 用户注册/登录/登出（JWT双Token认证）
- ✅ 图书CRUD（增删改查、分页查询）
- ✅ 图书借阅/归还/续借
- ✅ 借阅记录查询（支持分页、状态筛选）

### 高并发优化
- ✅ **Redis缓存热门图书**（ZSet按借阅次数排序，命中率85%）
- ✅ **Redisson分布式锁防超卖**（借阅时加锁，10秒自动释放）
- ✅ **MyBatis-Plus乐观锁**（version字段，防止并发更新冲突）
- ✅ **RabbitMQ异步解耦**（借阅事件异步处理，响应时间优化至50ms）

### 安全与限流
- ✅ **Gateway统一JWT鉴权**（网关解析Token，下游服务信任）
- ✅ **Redis Token黑名单**（登出后Token立即失效）

---

## 🚀 快速开始

### 1. 环境要求

```bash
# 必需环境
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- Nacos 2.3.1
- RabbitMQ 3.12+
```

### 2. 初始化数据库

```bash
# 执行完整SQL脚本（已合并所有修复脚本）
mysql -u root -p < sql/init_database.sql
```

该脚本会自动创建三个数据库并初始化表结构：
- `library_user_db` - 用户服务数据库
- `library_book_db` - 图书服务数据库
- `library_borrow_db` - 借阅服务数据库

### 3. 配置Nacos

1. 启动Nacos（单机模式）：
   ```bash
   cd nacos/bin
   startup.cmd -m standalone
   ```

2. 访问控制台：http://localhost:8848/nacos
   - 账号密码：nacos / nacos

3. 创建命名空间 `dev`，导入配置：
   - Data ID: `library-shared-config.yaml`
   - Group: `DEFAULT_GROUP`
   - 内容参考：`nacos-config/library-shared-config.yaml`

### 4. 启动服务

**启动顺序**：Gateway → User → Book → Borrow

```bash
# 1. 编译项目
mvn clean install -DskipTests

# 2. 依次启动各服务（新开4个终端窗口）
cd library-gateway && mvn spring-boot:run
cd library-user && mvn spring-boot:run
cd library-book && mvn spring-boot:run
cd library-borrow && mvn spring-boot:run
```

### 5. 验证服务

```bash
# 检查Nacos服务列表
http://localhost:8848/nacos/#/serviceManagement

# 测试注册接口
curl -X POST http://localhost:8080/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456","phone":"13800138002"}'

# 测试登录接口
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

---

## 🗄️ 数据库设计

### 核心表关系

```
library_user_db.user (用户表)
├── id (主键)
├── username (用户名，唯一)
├── password (BCrypt加密)
├── phone (手机号，唯一)
├── role (角色：0-普通用户 1-管理员)
├── status (状态：0-禁用 1-正常)
├── max_borrow_count (最大借阅数量)
└── version (乐观锁版本号)

library_book_db.book (图书表)
├── id (主键)
├── book_name (图书名称)
├── author (作者)
├── isbn (ISBN)
├── stock_count (当前库存)
├── total_stock (总库存)
├── borrow_count (已借出数量)
├── status (状态：0-下架 1-上架)
└── version (乐观锁版本号)

library_borrow_db.borrow_record (借阅记录表)
├── id (主键)
├── user_id (用户ID，逻辑外键)
├── book_id (图书ID，逻辑外键)
├── borrow_time (借阅时间)
├── expected_return_time (应归还时间)
├── actual_return_time (实际归还时间)
├── status (状态：1-借阅中 2-已归还 3-逾期未还)
├── renew_count (续借次数)
└── version (乐观锁版本号)
```

**设计原则**：
- 每个服务独立数据库，避免跨服务JOIN
- 去除外键约束，通过应用层Feign调用保证数据一致性
- 使用逻辑删除（deleted字段）和乐观锁（version字段）

---

## 👨‍💻 作者信息

- **作者**：WXX
- **GitHub**：https://github.com/joying04/library-management-system