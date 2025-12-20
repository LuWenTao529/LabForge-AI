package com.winter.labforgeai;

import com.winter.labforgeai.ai.AiCodeGenTypeRoutingService;
import com.winter.labforgeai.ai.AiCodeGenTypeRoutingServiceFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AI并发测试类
 * 使用虚拟线程测试AI代码生成类型路由服务的并发调用
 */
@Slf4j
@SpringBootTest
public class AiConcurrentTest {

    @Resource  // 依赖注入注解，自动注入routingServiceFactory
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    /**
     * 测试并发路由调用方法
     * @throws InterruptedException 如果线程被中断
     */
    @Test
    public void testConcurrentRoutingCalls() throws InterruptedException {
        // 定义测试提示词数组
        String[] prompts = {
                "做一个简单的HTML页面",
                "做一个简单多页面网站项目",
                "做一个Vue管理系统"
        };
        // 使用虚拟线程并发执行
        Thread[] threads = new Thread[prompts.length];
        for (int i = 0; i < prompts.length; i++) {
            final String prompt = prompts[i];
            final int index = i + 1;
            threads[i] = Thread.ofVirtual().start(() -> {
                AiCodeGenTypeRoutingService service = routingServiceFactory.createAiCodeGenTypeRoutingService();
                var result = service.routeCodeGenType(prompt);
                log.info("线程 {}: {} -> {}", index, prompt, result.getValue());
            });
        }
        // 等待所有任务完成
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
