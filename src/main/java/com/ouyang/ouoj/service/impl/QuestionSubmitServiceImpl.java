package com.ouyang.ouoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouyang.ouoj.common.ErrorCode;
import com.ouyang.ouoj.exception.BusinessException;
import com.ouyang.ouoj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.ouyang.ouoj.model.entity.Question;
import com.ouyang.ouoj.model.entity.QuestionSubmit;
import com.ouyang.ouoj.model.entity.User;
import com.ouyang.ouoj.model.enums.QuestionSubmitLanguageEnum;
import com.ouyang.ouoj.model.enums.QuestionSubmitStatusEnum;
import com.ouyang.ouoj.service.QuestionService;
import com.ouyang.ouoj.service.QuestionSubmitService;
import com.ouyang.ouoj.mapper.QuestionSubmitMapper;
import com.ouyang.ouoj.service.UserService;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author yifei
 * @description 针对表【question_submit(题目提交)】的数据库操作Service实现
 * @createDate 2024-11-05 11:19:47
 */
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
        implements QuestionSubmitService {

    @Resource
    private QuestionService questionService;


    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return 提交记录id
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        //校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"编程语言错误");
        }


        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        // 锁必须要包裹住事务方法
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        //设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        return questionSubmit.getId();


    }

}




