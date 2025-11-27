package com.ouyang.ouoj.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouyang.ouoj.common.ErrorCode;
import com.ouyang.ouoj.constant.CommonConstant;
import com.ouyang.ouoj.exception.BusinessException;
import com.ouyang.ouoj.exception.ThrowUtils;
import com.ouyang.ouoj.model.dto.question.QuestionQueryRequest;
import com.ouyang.ouoj.model.entity.*;
import com.ouyang.ouoj.model.vo.QuestionVO;
import com.ouyang.ouoj.model.vo.UserVO;
import com.ouyang.ouoj.service.QuestionService;
import com.ouyang.ouoj.mapper.QuestionMapper;
import com.ouyang.ouoj.service.UserService;
import com.ouyang.ouoj.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author yifei
 * @description 针对表【question(题目)】的数据库操作Service实现
 * @createDate 2024-11-05 11:17:39
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
        implements QuestionService {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 配置Gson以避免Long类型精度丢失
    private static final Gson GSON = new GsonBuilder()
            .setLongSerializationPolicy(LongSerializationPolicy.STRING)
            .create();
    
    // Redis缓存key前缀
    private static final String QUESTION_CACHE_PREFIX = "question:";
    private static final String QUESTION_LIST_CACHE_PREFIX = "question_list:";
    
    // 缓存过期时间（秒）
    private static final long CACHE_EXPIRE_TIME = 3600; // 1小时
    
    // 缓存开关 - 可以通过配置文件控制
    @Value("${cache.redis.enabled:true}")
    private boolean cacheEnabled = true;

    /**
     * 校验题目是否合法
     *
     * @param question
     * @param add
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        String answer = question.getAnswer();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();

        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (StringUtils.isNotBlank(answer) && answer.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案过长");
        }
        if (StringUtils.isNotBlank(judgeCase) && judgeCase.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题用例过长");
        }
        if (StringUtils.isNotBlank(judgeConfig) && judgeConfig.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
        }
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到mybatis框架支持的QueryWrapper类）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        int current = questionQueryRequest.getCurrent();
        int pageSize = questionQueryRequest.getPageSize();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();


        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        //queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        questionVO.setUserVO(userVO);

        return questionVO;
    }

    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollUtil.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充信息
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userService.getUserVO(user));
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    /**
     * 带缓存的根据ID获取题目
     *
     * @param id 题目ID
     * @return Question对象
     */
    @Override
    public Question getById(Serializable id) {
        if (id == null) {
            return null;
        }
        
        Long questionId;
        if (id instanceof Long) {
            questionId = (Long) id;
        } else {
            try {
                questionId = Long.valueOf(id.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return getQuestionById(questionId);
    }
    
    /**
     * 根据Long类型ID获取题目（带缓存）
     *
     * @param id 题目ID
     * @return Question对象
     */
    public Question getQuestionById(Long id) {
        if (id == null || id <= 0) {
            log.warn("getQuestionById: 无效的ID参数: {}", id);
            return null;
        }
        
        // 如果缓存被禁用，直接查询数据库
        if (!cacheEnabled) {
            log.info("getQuestionById: Redis缓存已禁用，直接查询数据库, ID={}", id);
            return super.getById(id);
        }
        
        String cacheKey = QUESTION_CACHE_PREFIX + id;
        log.info("getQuestionById: 开始查询题目, ID={}, cacheKey={}", id, cacheKey);
        
        try {
            // 1. 先从Redis缓存中获取
            Object cachedQuestion = redisTemplate.opsForValue().get(cacheKey);
            if (cachedQuestion != null) {
                log.info("getQuestionById: Redis缓存命中, ID={}, cachedData={}", id, cachedQuestion.toString());
                try {
                    Question question = GSON.fromJson(cachedQuestion.toString(), Question.class);
                    
                    // 缓存一致性检查：验证反序列化后的数据是否有效
                    if (question != null && question.getId() != null && question.getId().equals(id)) {
                        log.info("getQuestionById: 缓存数据有效, ID={}, questionId={}", id, question.getId());
                        return question;
                    } else {
                        log.warn("getQuestionById: 缓存数据无效或ID不匹配, 清除缓存, ID={}, cachedQuestionId={}", 
                                id, question != null ? question.getId() : "null");
                        // 清除无效缓存
                        redisTemplate.delete(cacheKey);
                    }
                } catch (Exception e) {
                    log.error("getQuestionById: 缓存反序列化失败, 清除缓存, ID={}, error={}", id, e.getMessage());
                    // 清除损坏的缓存
                    redisTemplate.delete(cacheKey);
                }
            }
            
            log.info("getQuestionById: Redis缓存未命中, 查询数据库, ID={}", id);
            // 2. 缓存未命中，从数据库查询
            Question question = super.getById(id);
            
            // 3. 如果查询到数据，存入缓存
            if (question != null) {
                String jsonData = GSON.toJson(question);
                log.info("getQuestionById: 数据库查询成功, 存入缓存, ID={}, jsonData={}", id, jsonData);
                redisTemplate.opsForValue().set(cacheKey, jsonData, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
                log.info("getQuestionById: 缓存存储成功, ID={}", id);
            } else {
                log.warn("getQuestionById: 数据库中未找到数据, ID={}", id);
            }
            
            return question;
        } catch (Exception e) {
            log.error("getQuestionById: Redis操作异常, 直接查询数据库, ID={}, error={}", id, e.getMessage(), e);
            // Redis异常时直接查询数据库，保证服务可用性
            return super.getById(id);
        }
    }

    /**
     * 带缓存的获取题目VO
     *
     * @param id 题目ID
     * @param request HTTP请求
     * @return QuestionVO对象
     */
    public QuestionVO getQuestionVOById(Long id, HttpServletRequest request) {
        Question question = this.getQuestionById(id);
        if (question == null) {
            return null;
        }
        return this.getQuestionVO(question, request);
    }

    /**
     * 清除题目缓存
     *
     * @param id 题目ID
     */
    public void clearQuestionCache(Long id) {
        if (id == null || id <= 0) {
            return;
        }
        
        // 如果缓存被禁用，直接返回
        if (!cacheEnabled) {
            log.info("clearQuestionCache: Redis缓存已禁用，跳过缓存清除, ID={}", id);
            return;
        }
        
        try {
            String cacheKey = QUESTION_CACHE_PREFIX + id;
            redisTemplate.delete(cacheKey);
            
            // 同时清除相关的列表缓存
            clearQuestionListCache();
        } catch (Exception e) {
            // 缓存清除失败不影响业务逻辑
        }
    }

    /**
     * 清除题目列表缓存
     */
    public void clearQuestionListCache() {
        // 如果缓存被禁用，直接返回
        if (!cacheEnabled) {
            log.info("clearQuestionListCache: Redis缓存已禁用，跳过缓存清除");
            return;
        }
        
        try {
            Set<String> keys = redisTemplate.keys(QUESTION_LIST_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存清除失败不影响业务逻辑
        }
    }





    /**
     * 带缓存的分页获取题目VO列表
     *
     * @param questionQueryRequest 查询请求
     * @param request HTTP请求
     * @return 分页的题目VO列表
     */
    public Page<QuestionVO> getQuestionVOPageWithCache(QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        
        // 如果缓存被禁用，直接查询数据库
        if (!cacheEnabled) {
            log.info("getQuestionVOPageWithCache: Redis缓存已禁用，直接查询数据库");
            Page<Question> questionPage = this.page(new Page<>(current, size),
                    this.getQueryWrapper(questionQueryRequest));
            return this.getQuestionVOPage(questionPage, request);
        }
        
        // 生成缓存key（基于查询条件）
        String cacheKey = generateListCacheKey(questionQueryRequest);
        
        try {
            // 1. 先从Redis缓存中获取
            Object cachedPage = redisTemplate.opsForValue().get(cacheKey);
            if (cachedPage != null) {
                return GSON.fromJson(cachedPage.toString(), Page.class);
            }
            
            // 2. 缓存未命中，从数据库查询
            Page<Question> questionPage = this.page(new Page<>(current, size),
                    this.getQueryWrapper(questionQueryRequest));
            Page<QuestionVO> questionVOPage = this.getQuestionVOPage(questionPage, request);
            
            // 3. 如果查询到数据，存入缓存（缓存时间较短，因为列表数据变化频繁）
            if (questionVOPage != null && !CollUtil.isEmpty(questionVOPage.getRecords())) {
                redisTemplate.opsForValue().set(cacheKey, GSON.toJson(questionVOPage), 
                    CACHE_EXPIRE_TIME / 2, TimeUnit.SECONDS); // 列表缓存时间减半
            }
            
            return questionVOPage;
        } catch (Exception e) {
            // Redis异常时直接查询数据库，保证服务可用性
            Page<Question> questionPage = this.page(new Page<>(current, size),
                    this.getQueryWrapper(questionQueryRequest));
            return this.getQuestionVOPage(questionPage, request);
        }
    }

    /**
     * 生成列表查询的缓存key
     *
     * @param questionQueryRequest 查询请求
     * @return 缓存key
     */
    private String generateListCacheKey(QuestionQueryRequest questionQueryRequest) {
        StringBuilder keyBuilder = new StringBuilder(QUESTION_LIST_CACHE_PREFIX);
        
        if (questionQueryRequest.getId() != null) {
            keyBuilder.append("id:").append(questionQueryRequest.getId()).append(":");
        }
        if (StringUtils.isNotBlank(questionQueryRequest.getTitle())) {
            keyBuilder.append("title:").append(questionQueryRequest.getTitle().hashCode()).append(":");
        }
        if (StringUtils.isNotBlank(questionQueryRequest.getContent())) {
            keyBuilder.append("content:").append(questionQueryRequest.getContent().hashCode()).append(":");
        }
        if (CollUtil.isNotEmpty(questionQueryRequest.getTags())) {
            keyBuilder.append("tags:").append(questionQueryRequest.getTags().hashCode()).append(":");
        }
        if (questionQueryRequest.getUserId() != null) {
            keyBuilder.append("userId:").append(questionQueryRequest.getUserId()).append(":");
        }
        
        keyBuilder.append("current:").append(questionQueryRequest.getCurrent()).append(":");
        keyBuilder.append("size:").append(questionQueryRequest.getPageSize()).append(":");
        
        if (StringUtils.isNotBlank(questionQueryRequest.getSortField())) {
            keyBuilder.append("sort:").append(questionQueryRequest.getSortField()).append(":");
        }
        if (StringUtils.isNotBlank(questionQueryRequest.getSortOrder())) {
            keyBuilder.append("order:").append(questionQueryRequest.getSortOrder());
        }
        
        return keyBuilder.toString();
    }

    /**
     * 重写保存方法，保存后清除缓存
     */
    @Override
    public boolean save(Question entity) {
        boolean result = super.save(entity);
        if (result) {
            // 清除列表缓存
            clearQuestionListCache();
        }
        return result;
    }

    /**
     * 重写批量保存方法，保存后清除缓存
     */
    @Override
    public boolean saveBatch(Collection<Question> entityList) {
        boolean result = super.saveBatch(entityList);
        if (result) {
            // 清除列表缓存
            clearQuestionListCache();
        }
        return result;
    }

    /**
     * 重写更新方法，更新后清除相关缓存
     */
    @Override
    public boolean updateById(Question entity) {
        boolean result = super.updateById(entity);
        if (result && entity.getId() != null) {
            // 清除单个题目缓存和列表缓存
            clearQuestionCache(entity.getId());
        }
        return result;
    }

    /**
     * 重写批量更新方法，更新后清除缓存
     */
    @Override
    public boolean updateBatchById(Collection<Question> entityList) {
        boolean result = super.updateBatchById(entityList);
        if (result) {
            // 清除相关缓存
            for (Question question : entityList) {
                clearQuestionCache(question.getId());
            }
            clearQuestionListCache();
        }
        return result;
    }

    /**
     * 重写删除方法，删除后清除相关缓存
     */
    @Override
    public boolean removeById(Serializable id) {
        boolean result = super.removeById(id);
        if (result) {
            // 清除相关缓存
            if (id instanceof Long) {
                clearQuestionCache((Long) id);
            } else {
                try {
                    Long longId = Long.valueOf(id.toString());
                    clearQuestionCache(longId);
                } catch (NumberFormatException e) {
                    // 忽略无效的ID
                }
            }
            clearQuestionListCache();
        }
        return result;
    }

    /**
     * 重写批量删除方法，删除后清除缓存
     */
    @Override
    public boolean removeByIds(Collection<?> idList) {
        boolean result = super.removeByIds(idList);
        if (result) {
            // 清除相关题目缓存
            for (Object id : idList) {
                if (id instanceof Long) {
                    clearQuestionCache((Long) id);
                } else if (id instanceof Serializable) {
                    try {
                        Long longId = Long.valueOf(id.toString());
                        clearQuestionCache(longId);
                    } catch (NumberFormatException e) {
                        // 忽略无效的ID
                    }
                }
            }
        }
        return result;
    }

}




