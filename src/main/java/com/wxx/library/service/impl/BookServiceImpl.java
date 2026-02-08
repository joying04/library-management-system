package com.wxx.library.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxx.library.constant.SystemConstant;
import com.wxx.library.enums.ResultCode;
import com.wxx.library.dto.BookQueryDTO;
import com.wxx.library.entity.Book;
import com.wxx.library.exception.BusinessException;
import com.wxx.library.mapper.BookMapper;
import com.wxx.library.service.BookService;
import com.wxx.library.util.RedisUtil;
import com.wxx.library.util.Result;
import com.wxx.library.vo.BookVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图书服务实现类（含Redis缓存、MP高级查询、逻辑删除）
 */
@Service
@Slf4j
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Autowired
    private RedisUtil redisUtil;

    @Value("${library.redis.hot-book-expire}")
    private Long hotBookExpire; // 缓存过期时间（秒）

    @Value("${library.redis.cache-null-ttl}")
    private long CACHE_NULL_TTL; // 空值缓存过期时间（30分钟，避免缓存穿透）

    @Value("${library.redis.cache-ttl}")
    private long CACHE_TTL; // 正常缓存过期时间（1小时)

    /**
     * 图书分页查询（MP分页插件+多条件查询）
     */
    @Override
    public Result<Page<BookVO>> getBookPage(BookQueryDTO queryDTO) {
        // 1. 构建MP分页对象
        Page<Book> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构建查询条件（LambdaQueryWrapper）
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        // 模糊查询：图书名称
        if (StringUtils.hasText(queryDTO.getName())) {
            queryWrapper.like(Book::getName, queryDTO.getName());
        }
        // 模糊查询：作者
        if (StringUtils.hasText(queryDTO.getAuthor())) {
            queryWrapper.like(Book::getAuthor, queryDTO.getAuthor());
        }
        // 精确查询：分类
        if (StringUtils.hasText(queryDTO.getCategory())) {
            queryWrapper.eq(Book::getCategory, queryDTO.getCategory());
        }
        // 精确查询：ISBN
        if (StringUtils.hasText(queryDTO.getIsbn())) {
            queryWrapper.eq(Book::getIsbn, queryDTO.getIsbn());
        }
        // 排序：按创建时间降序
        queryWrapper.orderByDesc(Book::getCreateTime);

        // 3. MP分页查询（自动分页，无需手动写LIMIT）
        Page<Book> bookPage = page(page, queryWrapper);

        // 4. 转换为VO（隐藏不必要字段，返回给前端）
        Page<BookVO> bookVOPage = new Page<>();
        BeanUtils.copyProperties(bookPage, bookVOPage); // 复制分页元数据
        List<BookVO> bookVOList = bookPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        bookVOPage.setRecords(bookVOList); //设置records

        log.info("图书分页查询成功，页码：{}，每页条数：{}，总条数：{}",
                queryDTO.getPageNum(), queryDTO.getPageSize(), bookPage.getTotal());
        return Result.success(bookVOPage);
    }

    /**
     * 根据ID查询图书详情
     * Redis缓存 + 空值缓存，防止缓存穿透
     */
    @Override
    public Result<BookVO> getBookById(Long id) {
        // 1.查询Redis缓存
        String cacheKey = SystemConstant.BOOK_CACHE_KEY_PREFIX + id;
        BookVO bookVO = redisUtil.get(cacheKey, BookVO.class); //无需强转
        if (bookVO != null) {
            return Result.success(bookVO); // 缓存命中，直接返回
        }

        // 2.查询数据库（缓存未命中）
        Book book = getById(id);
        if (book == null) {
            redisUtil.set(cacheKey, null, CACHE_NULL_TTL, TimeUnit.MINUTES);  //30分钟后过期
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }

        // 3.数据库存在，转换VO并写入缓存
        bookVO = convertToVO(book);
        redisUtil.set(cacheKey, bookVO, CACHE_TTL, TimeUnit.SECONDS);
        return Result.success(bookVO);
    }

    /**
     * 获取热门图书
     */
    @Override
    public Result<List<BookVO>> getHotBooks() {
        // 1. 先查Redis缓存
        List<BookVO> hotBookVOList = getHotBooksFromCache();
        if (!hotBookVOList.isEmpty()) {
            log.info("从Redis缓存获取热门图书");
            return Result.success(hotBookVOList);
        }

        // 2. 缓存未命中，查询数据库（借阅次数前10）
        log.info("Redis缓存未命中，查询数据库获取热门图书");
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(Book::getBorrowCount, 0) // 借阅次数>0
                .orderByDesc(Book::getBorrowCount) // 按借阅次数降序
                .last("LIMIT 10"); // 限制10条
        List<Book> hotBookList = list(queryWrapper);

        // 3. 转换为VO
        hotBookVOList = hotBookList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 4. 存入Redis缓存（设置过期时间3600+-300秒）  //避免缓存雪崩
        long randomExpire = hotBookExpire + new Random().nextInt(600)-300;
        redisUtil.set(SystemConstant.hotBookKey, hotBookVOList, randomExpire, TimeUnit.SECONDS);

        return Result.success(hotBookVOList);
    }

    /**
     * 新增图书（仅管理员）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Book> addBook(Book book) {
        // 1. 校验ISBN唯一性
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getIsbn, book.getIsbn());
        if (count(queryWrapper) > 0) {
            throw new BusinessException("ISBN已存在，无法重复添加");
        }

        // 2. 初始化字段
        book.setBorrowCount(0); // 初始借阅次数0
        if (book.getStock() == null) {
            book.setStock(0);
        }
        if (book.getTotalStock() == null) {
            book.setTotalStock(0);
        }

        // 3. 保存图书
        boolean saveSuccess = save(book);
        if (!saveSuccess) {
            throw new BusinessException("新增图书失败");
        }

        log.info("新增图书成功，图书ID:{},图书名称：{}，ISBN：{}", book.getId(),book.getName(), book.getIsbn());
        return Result.success(book);
    }

    /**
     * 更新图书信息（仅管理员，MP乐观锁）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> updateBook(Book book) {
        // 1. 校验图书是否存在
        if (baseMapper.selectById(book.getId()) == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }

        // 2. 校验ISBN唯一性（排除自身）
        LambdaQueryWrapper<Book> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Book::getIsbn, book.getIsbn())
                .ne(Book::getId, book.getId());
        if (count(queryWrapper) > 0) {
            throw new BusinessException("ISBN已存在，无法更新");
        }

        // 3. MP更新（乐观锁自动生效，version字段控制）
        boolean updateSuccess = updateById(book);
        if (!updateSuccess) {
            throw new BusinessException("更新图书失败（可能已被其他用户修改，请刷新重试）");
        }

        // 4. 清除热门图书缓存（图书信息变化，缓存失效）
        redisUtil.delete(SystemConstant.hotBookKey);

        log.info("更新图书成功，图书ID：{}，图书名称：{}", book.getId(), book.getName());
        return Result.success(true);
    }

    /**
     * 删除图书（逻辑删除，MP自动处理）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> deleteBook(Long id) {
        // 1. 校验图书是否存在
        if (baseMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }

        // 2. MP逻辑删除（删除后deleted=1，查询时自动过滤）
        boolean removeSuccess = removeById(id);
        if (!removeSuccess) {
            throw new BusinessException("删除图书失败");
        }

        // 3. 清除热门图书缓存
        redisUtil.delete(SystemConstant.hotBookKey);

        log.info("删除图书成功，图书ID：{}", id);
        return Result.success(true);
    }

    /**
     * 更新图书库存（内部调用，事务中使用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStock(Long bookId, Integer change) {
        Book book = getById(bookId);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_EXIST);
        }

        // 计算新库存
        int newStock = book.getStock() + change;
        if (newStock < 0) {
            throw new BusinessException(ResultCode.BOOK_STOCK_EMPTY);
        }

        // 传递版本号，使用乐观锁更新
        int updateCount = baseMapper.updateStockWithVersion(bookId,change,book.getVersion());

        if(updateCount == 0) {
            throw new BusinessException("库存更新失败，可能被其他用户修改，请重试");
        }
        return true;
    }

    // ==================== 私有工具方法 ====================
    /**
     * Book实体转换为BookVO(隐藏敏感字段，仅返回前端所需信息)
     */
    private BookVO convertToVO(Book book) {
        BookVO bookVO = new BookVO();
        BeanUtils.copyProperties(book, bookVO);
        return bookVO;
    }

    /**
     * 从Redis缓存获取热门图书
     */
    @SuppressWarnings("unchecked")
    private List<BookVO> getHotBooksFromCache() {
        try {
            Object cacheObj = redisUtil.get(SystemConstant.hotBookKey);
            if (cacheObj instanceof List) {
                return (List<BookVO>) cacheObj;
            }
        } catch (Exception e) {
            log.warn("从Redis获取热门图书缓存失败,将查询数据库", e);
        }
        return new ArrayList<>();
    }

    /**
     *乐观锁扣减库存
     */

    @Override
    public boolean decreaseStockWithVersion(Long bookId, Integer version) {
        // 调用Mapper的乐观锁更新方法
        int updateCount = baseMapper.decreaseStockWithVersion(bookId, version);
        return updateCount > 0; // 更新成功返回true，失败返回false
    }
}
