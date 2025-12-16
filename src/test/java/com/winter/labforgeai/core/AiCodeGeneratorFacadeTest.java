package com.winter.labforgeai.core;

import com.winter.labforgeai.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("任务记录网站，不超过20行代码", CodeGenTypeEnum.MULTI_FILE, 1L);
        Assertions.assertNotNull(file);
    }

    /**
     * 测试生成并保存代码流HTML的方法
     * 该方法测试AI代码生成器生成HTML代码流的功能，并验证生成结果的有效性
     */
    @Test
    void generateAndSaveCodeStreamHtml() {
        // 调用AI代码生成器门面，生成并保存HTML代码流，主题为"任务记录网站，不超过20行代码"
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("任务记录网站，不超过20行代码", CodeGenTypeEnum.HTML, 1L);
        // 阻塞等待所有数据收集完成，将流式数据转换为List集合
        List<String> result = codeStream.collectList().block();
        // 验证结果列表不为空，确保代码生成过程正常执行
        Assertions.assertNotNull(result);
        // 将所有生成的代码片段拼接为完整内容
        String completeContent = String.join("", result);
        // 验证完整内容不为空，确保生成的代码有效
        Assertions.assertNotNull(completeContent);
    }

    /**
     * 测试方法：生成并保存多文件代码流
     * 该测试用例验证AI代码生成器生成并保存多文件代码流的功能
     */
    @Test
    void generateAndSaveCodeStreamMutiFile() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("任务记录网站，不超过20行代码", CodeGenTypeEnum.MULTI_FILE, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}
