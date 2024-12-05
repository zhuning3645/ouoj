package com.ouyang.ouoj.judge.strategy;


import com.ouyang.ouoj.model.dto.question.JudgeCase;
import com.ouyang.ouoj.judge.codesandbox.model.JudgeInfo;
import com.ouyang.ouoj.model.entity.Question;
import com.ouyang.ouoj.model.entity.QuestionSubmit;
import lombok.Data;

import java.util.List;

/**
 * 上下文（用于定义在策略中传递的参数）
 */
@Data
public class JudgeContext {

    private JudgeInfo judgeInfo;

    private List<String> inputList;

    private List<String> outputList;

    private Question question;

    private List<JudgeCase> judgeCaseList;

    private QuestionSubmit questionSubmit;
}
