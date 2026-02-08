package com.wxx.library.base;

import com.wxx.library.entity.Book;
import com.wxx.library.entity.User;
import com.wxx.library.enums.UserRoleEnum;
import com.wxx.library.enums.UserStatusEnum;
import com.wxx.library.mapper.BookMapper;
import com.wxx.library.mapper.UserMapper;
import com.wxx.library.util.PasswordUtil;
import com.wxx.library.util.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试父类：抽离公共逻辑，子类继承复用
 */
@SpringBootTest
@Transactional // 所有测试自动回滚，避免污染数据库
public abstract class BaseTest {

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected BookMapper bookMapper;

    // 测试数据
    protected Long testUserId; // 普通用户ID
    protected Long testOtherUserId;
    protected Long testAdminId; // 管理员ID
    protected Long testBookId; // 测试图书ID

    /**
     * 测试前初始化公共数据（子类可覆盖）
     */
    protected void initCommonData() {
        // 1. 创建普通用户
        User commonUser1 = new User();
        commonUser1.setPhone("13800138001");
        commonUser1.setPassword(PasswordUtil.encryptPassword("897521"));
        commonUser1.setName("普通测试用户1");
        commonUser1.setRole(UserRoleEnum.COMMON.getCode());
        commonUser1.setStatus(UserStatusEnum.ENABLE.getCode());
        userMapper.insert(commonUser1);
        testUserId = commonUser1.getId();

        User commonUser2 = new User();
        commonUser2.setPhone("13800138002");
        commonUser2.setPassword(PasswordUtil.encryptPassword("897522"));
        commonUser2.setName("普通测试用户2");
        commonUser2.setRole(UserRoleEnum.COMMON.getCode());
        commonUser2.setStatus(UserStatusEnum.ENABLE.getCode());
        userMapper.insert(commonUser2);
        testOtherUserId = commonUser2.getId();


        // 2. 创建管理员用户
        User adminUser = new User();
        adminUser.setPhone("13800138010");
        adminUser.setPassword(PasswordUtil.encryptPassword("admin123"));
        adminUser.setName("测试管理员");
        adminUser.setRole(UserRoleEnum.ADMIN.getCode());
        adminUser.setStatus(UserStatusEnum.ENABLE.getCode());
        userMapper.insert(adminUser);
        testAdminId = adminUser.getId();

        // 3. 创建测试图书
        Book testBook = new Book();
        testBook.setName("测试图书");
        testBook.setAuthor("测试作者");
        testBook.setIsbn("MIDDLE-TEST-123456");
        testBook.setCategory("测试分类");
        testBook.setStock(5);
        testBook.setTotalStock(10);
        testBook.setBorrowCount(0);
        bookMapper.insert(testBook);
        testBookId = testBook.getId();
    }

    /**
     * 测试后清理线程上下文（防内存泄漏）
     */
    @AfterEach
    void tearDown() {
        UserContext.clear();
    }
}
