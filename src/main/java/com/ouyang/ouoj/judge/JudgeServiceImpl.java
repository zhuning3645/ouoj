package com.ouyang.ouoj.judge;

import cn.hutool.json.JSONUtil;
import com.ouyang.ouoj.common.ErrorCode;
import com.ouyang.ouoj.exception.BusinessException;
import com.ouyang.ouoj.judge.codesandbox.CodeSandbox;
import com.ouyang.ouoj.judge.codesandbox.CodeSandboxFactory;
import com.ouyang.ouoj.judge.codesandbox.CodeSandboxProxy;
import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.ouyang.ouoj.judge.strategy.DefaultJudgeStrategy;
import com.ouyang.ouoj.judge.strategy.JudgeContext;
import com.ouyang.ouoj.judge.strategy.JudgeStrategy;
import com.ouyang.ouoj.model.dto.question.JudgeCase;
import com.ouyang.ouoj.model.dto.questionsubmit.JudgeInfo;
import com.ouyang.ouoj.model.entity.Question;
import com.ouyang.ouoj.model.entity.QuestionSubmit;
import com.ouyang.ouoj.model.enums.QuestionSubmitStatusEnum;
import com.ouyang.ouoj.service.QuestionService;
import com.ouyang.ouoj.service.QuestionSubmitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Resource
    private JudgeManager judgeManager;

    @Value("${codesandbox.type:example}")
    private String type;

    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {

        //0.传入题目的提交id 获取到对应的题目、提交信息（包含代码、编程语言）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }

        //1.更改判题的状态 只去判断等待中的题目
        if (questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }

        //2.更改题目状态为“判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }

        //3.调用代码沙箱，获取执行结果
        CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
        codeSandbox = new CodeSandboxProxy(codeSandbox);
        String code = questionSubmit.getCode();
        String language = questionSubmit.getLanguage();
        //4.获取输入用例 输入到测试
        String judgeCaseStr = question.getJudgeCase();
        List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
        List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
        ExecuteCodeRequest exampleCodeRequest = ExecuteCodeRequest.builder()
                .code(code)
                .language(language)
                .inputList(inputList)
                .build();
        ExecuteCodeResponse executeCodeResponse = codeSandbox.executeCode(exampleCodeRequest);
        List<String> outputList = executeCodeResponse.getOutputList();
        //5.根据沙箱的执行结果，设置题目的判题状态和信息
        JudgeContext judgeContext = new JudgeContext();
        judgeContext.setJudgeInfo(executeCodeResponse.getJudgeInfo());
        judgeContext.setInputList(inputList);
        judgeContext.setOutputList(outputList);
        judgeContext.setJudgeCaseList(judgeCaseList);
        judgeContext.setQuestion(question);
        judgeContext.setQuestionSubmit(questionSubmit);

        JudgeInfo judgeInfo = judgeManager.doJudge(judgeContext);
        //6.修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
        update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        //7.再获取一次最新的状态
        QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionId);
        return questionSubmitResult;
    }

}
