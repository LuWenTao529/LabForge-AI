package com.winter.labforgeai.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.winter.labforgeai.langgraph4j.model.ImageResource;
import com.winter.labforgeai.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UndrawIllustrationTool {

    private static final String PIXABAY_API_URL = "https://pixabay.com/api/";

    /**
     * 建议不要硬编码 key，放到配置里：
     * pixabay.api.key=xxxx
     */
    @Value("${pixabay.api.key:}")
    private String pixabayApiKey;

    @Value("${pixabay.api.lang:zh}")
    private String lang;

    @Value("${pixabay.api.safesearch:true}")
    private boolean safeSearch;

    @Tool("搜索插画图片，用于网站美化和装饰")
    public List<ImageResource> searchIllustrations(@P("搜索关键词") String query) {
        List<ImageResource> imageList = new ArrayList<>();
        int searchCount = 12;
        if (StrUtil.isBlank(query)) {
            return imageList;
        }
        if (StrUtil.isBlank(pixabayApiKey)) {
            log.warn("Pixabay API key 未配置：请在配置中设置 pixabay.api.key");
            return imageList;
        }
        // Pixabay 文档：q 最多 100 字符
        String q = query.length() > 100 ? query.substring(0, 100) : query;
        try (HttpResponse response = HttpRequest.get(PIXABAY_API_URL)
                .form("key", pixabayApiKey)
                .form("q", q)
                // 插画：illustration（如想要矢量可改 vector；或 all）
                .form("image_type", "illustration")
                .form("lang", StrUtil.blankToDefault(lang, "zh"))
                .form("order", "popular")
                .form("safesearch", String.valueOf(safeSearch))
                .form("page", "1")
                .form("per_page", String.valueOf(searchCount))
                .timeout(10000)
                .execute()) {
            if (!response.isOk()) {
                return imageList;
            }
            JSONObject result = JSONUtil.parseObj(response.body());
            JSONArray hits = result.getJSONArray("hits");
            if (hits == null || hits.isEmpty()) {
                return imageList;
            }
            int actualCount = Math.min(searchCount, hits.size());
            for (int i = 0; i < actualCount; i++) {
                JSONObject hit = hits.getJSONObject(i);

                // Pixabay 没有 title 字段，用 tags 作为描述（逗号分隔）
                String tags = hit.getStr("tags", "");
                String description = StrUtil.isNotBlank(tags) ? tags : "插画";

                // URL 优先级：largeImageURL > webformatURL > previewURL
                String url = hit.getStr("largeImageURL");
                if (StrUtil.isBlank(url)) {
                    url = hit.getStr("webformatURL");
                }
                if (StrUtil.isBlank(url)) {
                    url = hit.getStr("previewURL");
                }

                if (StrUtil.isNotBlank(url)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.ILLUSTRATION)
                            .description(description)
                            .url(url)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("搜索插画失败：{}", e.getMessage(), e);
        }
        return imageList;
    }
}
