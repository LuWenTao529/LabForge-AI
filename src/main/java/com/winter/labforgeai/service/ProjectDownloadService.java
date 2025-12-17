package com.winter.labforgeai.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {


    /**
     * 将指定项目路径的内容下载为ZIP压缩文件
     *
     * @param projectPath 需要下载的项目路径
     * @param downloadFileName 下载时指定的文件名
     * @param response HTTP响应对象，用于输出下载文件流
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
