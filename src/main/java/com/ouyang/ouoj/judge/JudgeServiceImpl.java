package com.ouyang.ouoj.judge;

import com.ouyang.ouoj.common.ErrorCode;
import com.ouyang.ouoj.exception.BusinessException;
import com.ouyang.ouoj.model.entity.QuestionSubmit;
import com.ouyang.ouoj.model.vo.QuestionSubmitVO;
import com.ouyang.ouoj.service.QuestionService;
import com.ouyang.ouoj.service.QuestionSubmitService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class JudgeServiceImpl implements JudgeService{

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Override
    public QuestionSubmitVO doJudge(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if(questionSubmit == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"提交信息不存在");
        }
        Long id = questionSubmit.getId();
        String language = questionSubmit.getLanguage();
        String code = questionSubmit.getCode();
        String judgeInfo = questionSubmit.getJudgeInfo();
        Integer status = questionSubmit.getStatus();
        Long questionId = questionSubmit.getQuestionId();
        Long userId = questionSubmit.getUserId();
        Date createTime = questionSubmit.getCreateTime();
        Date updateTime = questionSubmit.getUpdateTime();

        return null;
    }

}
