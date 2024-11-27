package com.ouyang.ouoj.judge;


import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.ouyang.ouoj.model.entity.QuestionSubmit;
import com.ouyang.ouoj.model.vo.QuestionSubmitVO;

public interface JudgeService {

    /**
     * 判题服务
     * @param questionSubmitId
     * @return
     */
    QuestionSubmitVO doJudge(long questionSubmitId);
}
