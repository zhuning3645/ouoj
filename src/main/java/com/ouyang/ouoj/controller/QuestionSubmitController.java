package com.ouyang.ouoj.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ouyang.ouoj.common.BaseResponse;
import com.ouyang.ouoj.common.ErrorCode;
import com.ouyang.ouoj.common.ResultUtils;
import com.ouyang.ouoj.exception.BusinessException;
import com.ouyang.ouoj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.ouyang.ouoj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.ouyang.ouoj.model.entity.QuestionSubmit;
import com.ouyang.ouoj.model.entity.User;
import com.ouyang.ouoj.model.vo.QuestionSubmitVO;
import com.ouyang.ouoj.service.QuestionSubmitService;
import com.ouyang.ouoj.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题目提交接口
 */
@RestController
@RequestMapping("/question_submit")
@Slf4j
@Deprecated
public class QuestionSubmitController {


}
