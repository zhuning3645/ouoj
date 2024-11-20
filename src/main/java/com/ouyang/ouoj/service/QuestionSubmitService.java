package com.ouyang.ouoj.service;

import com.ouyang.ouoj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.ouyang.ouoj.model.entity.QuestionSubmit;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ouyang.ouoj.model.entity.User;

/**
 * @author yifei
 * @description 针对表【question_submit(题目提交)】的数据库操作Service
 * @createDate 2024-11-05 11:19:47
 */
public interface QuestionSubmitService extends IService<QuestionSubmit> {
    /**
     * 题目提交
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser);


}
