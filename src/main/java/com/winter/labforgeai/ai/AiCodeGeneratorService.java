package com.winter.labforgeai.ai;

import com.winter.labforgeai.ai.model.HtmlCodeResult;
import com.winter.labforgeai.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成代码的方法
     * 根据用户输入的消息生成相应的代码字符串
     *
     * @param userMessage 用户输入的消息内容，作为生成代码的依据
     * @return 返回生成的代码字符串
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 使用从资源文件中加载的系统消息来生成多文件代码的方法
     * 该方法会从指定的资源文件中读取提示信息，并根据用户输入的消息生成相应的多文件代码
     *
     * @param userMessage 用户输入的消息，将用于生成代码的依据
     * @return 返回生成的多文件代码字符串
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成HTML代码流的接口方法
     * 使用SystemMessage注解从指定资源文件中加载系统提示信息
     * 该方法返回一个Flux<String>类型的响应流，用于流式输出生成的HTML代码
     *
     * @param userMessage 用户输入的消息内容，将作为生成HTML代码的输入
     * @return 返回一个包含生成HTML代码的字符串流(Flux<String>)
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 根据用户消息生成多文件代码 (流式)
     *
     * @param userMessage 用户输入的消息，将作为生成代码的依据
     * @return 返回一个Flux<String>类型的响应流，包含生成的多文件代码内容
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);


    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);

}
