package com.winter.labforgeai.service;

public interface ScreenshotService {


    /**
     * 生成网页截图并上传的方法
     * 该方法接收一个网页URL，生成该网页的截图，并将截图上传到指定位置
     *
     * @param webUrl 需要生成截图的网页URL地址
     * @return 返回处理结果的字符串，可能包含上传成功的URL或其他相关信息
     */
    String generateAndUploadScreenshot(String webUrl);
}
