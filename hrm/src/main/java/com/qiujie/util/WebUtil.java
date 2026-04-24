package com.qiujie.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class WebUtil {

    /**
     * 渲染字符串到前端
     *
     * @param response
     * @param str
     */
    public static void renderString(HttpServletResponse response, String str){
        try{
            // 只在状态码未设置时才设置为200，避免覆盖已设置的状态码（如401、403）
            if (response.getStatus() == 200) {
                response.setStatus(200);
            }
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
