package com.ouyang.ouoj.judge.strategy;

import com.ouyang.ouoj.model.dto.questionsubmit.JudgeInfo;

/**
 * 判题策略
 */

public interface JudgeStrategy {

    /**
     * 执行判题
     * @param judgeContext
     * @return
     */
    JudgeInfo doJudge(JudgeContext judgeContext);

}
