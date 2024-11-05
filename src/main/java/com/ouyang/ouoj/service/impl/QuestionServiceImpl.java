package com.ouyang.ouoj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ouyang.ouoj.model.entity.Question;
import com.ouyang.ouoj.service.QuestionService;
import com.ouyang.ouoj.mapper.QuestionMapper;
import org.springframework.stereotype.Service;

/**
* @author yifei
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2024-11-05 11:17:39
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService{

}




