package com.ouyang.ouoj.judge.codesandbox.impl;

import com.ouyang.ouoj.judge.codesandbox.CodeSandbox;
import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeResponse;
import com.ouyang.ouoj.model.dto.questionsubmit.JudgeInfo;
import com.ouyang.ouoj.model.enums.JudgeInfoMessageEnum;
import com.ouyang.ouoj.model.enums.QuestionSubmitStatusEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例代码沙箱
 */
@Slf4j
public class ExampleCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();


        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(inputList);
        executeCodeResponse.setMessage("测试成功执行");
        executeCodeResponse.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMessage(JudgeInfoMessageEnum.ACCEPTED.getText());
        judgeInfo.setMemory(100L);
        judgeInfo.setTime(100L);
        executeCodeResponse.setJudgeInfo(judgeInfo);

        System.out.println("示例代码沙箱");
        return executeCodeResponse;
    }
}
