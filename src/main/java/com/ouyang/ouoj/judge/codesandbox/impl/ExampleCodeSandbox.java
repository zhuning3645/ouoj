package com.ouyang.ouoj.judge.codesandbox.impl;

import com.ouyang.ouoj.judge.codesandbox.CodeSandbox;
import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeRequest;
import com.ouyang.ouoj.judge.codesandbox.model.ExecuteCodeResponse;

/**
 * 示例代码沙箱
 */
public class ExampleCodeSandbox implements CodeSandbox {

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        System.out.println("示例代码沙箱");
        return null;
    }
}
