package me.rrs.headdrop;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 网站控制器类
 * 处理排行榜网站的HTTP请求和响应
 * 作者: RRS
 */
public class WebsiteController {

    private HttpServer server;

    /**
     * 启动Web服务器
     * @param port 端口号
     * @throws IOException 启动失败时抛出
     */
    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // 为排行榜端点创建上下文
        server.createContext("/" + HeadDrop.getInstance().getConfiguration().getString("Web.Endpoint"), new LeaderboardHandler());
        server.start();
    }

    /**
     * 停止Web服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /**
     * 排行榜请求处理器
     */
    static class LeaderboardHandler implements HttpHandler {
        private static final int ENTRIES_PER_PAGE = 10; // 每页显示条目数

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Entry<String, Integer>> sortedPlayerData = getSortedPlayerData();

            int page = getRequestedPage(exchange.getRequestURI().getQuery());
            int startIndex = (page - 1) * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, sortedPlayerData.size());

            String response = generateHtmlResponse(sortedPlayerData, startIndex, endIndex, page);
            sendResponse(exchange, response);
        }

        /**
         * 获取排序后的玩家数据
         * @return 排序后的玩家数据列表
         */
        private List<Entry<String, Integer>> getSortedPlayerData() {
            Map<String, Integer> playerData = HeadDrop.getInstance().getDatabase().getPlayerData();
            return playerData.entrySet()
                    .stream()
                    .sorted(Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        }

        /**
         * 从查询参数中获取请求的页码
         * @param query 查询字符串
         * @return 页码
         */
        private int getRequestedPage(String query) {
            if (query != null) {
                return Arrays.stream(query.split("&"))
                        .filter(param -> param.startsWith("page="))
                        .map(param -> param.split("=")[1])
                        .filter(page -> page.matches("\\d+")) // 验证数字
                        .mapToInt(Integer::parseInt)
                        .findFirst()
                        .orElse(1); // 默认第一页
            }
            return 1; // 默认第一页
        }

        /**
         * 生成HTML响应
         * @param sortedPlayerData 排序后的玩家数据
         * @param startIndex 开始索引
         * @param endIndex 结束索引
         * @param page 当前页码
         * @return HTML响应字符串
         */
        private String generateHtmlResponse(List<Entry<String, Integer>> sortedPlayerData, int startIndex, int endIndex, int page) {
            return "<!DOCTYPE html>" +
                    "<html lang=\"zh-CN\">\n<head>\n<title>头颅收集排行榜</title>\n" +
                    "<meta charset=\"UTF-8\">\n" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "<meta name=\"description\" content=\"服务器玩家头颅收集排行榜\">\n" +
                    "<link href=\"https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@400;700&display=swap\" rel=\"stylesheet\">\n<style>\n" +
                    generateCssStyles() +
                    "</style>\n</head>\n<body>\n" +
                    "<div class=\"container\">\n" +
                    "<header>\n" +
                    "<h1>🏆 头颅收集排行榜</h1>\n" +
                    "<p class=\"subtitle\">展示服务器中最出色的头颅猎人们</p>\n" +
                    "</header>\n" +
                    "<button id=\"theme-toggle\" onclick=\"toggleTheme()\" title=\"切换主题\">🌙</button>\n" +
                    generateTable(sortedPlayerData, startIndex, endIndex) +
                    generatePaginationLinks(sortedPlayerData.size(), page) +
                    "<footer>\n" +
                    "<p class=\"footer-text\">数据更新于: " + new Date() + "</p>\n" +
                    "</footer>\n" +
                    "</div>\n" +
                    "<script>\n" +
                    "function toggleTheme() {\n" +
                    "    const body = document.body;\n" +
                    "    const isDarkMode = body.classList.toggle('dark-mode');\n" +
                    "    localStorage.setItem('theme', isDarkMode ? 'dark' : 'light');\n" +
                    "    const themeToggle = document.getElementById('theme-toggle');\n" +
                    "    themeToggle.innerHTML = isDarkMode ? '🌞' : '🌙';\n" +
                    "    themeToggle.setAttribute('title', isDarkMode ? '切换到浅色模式' : '切换到深色模式');\n" +
                    "}\n" +
                    "function loadTheme() {\n" +
                    "    const savedTheme = localStorage.getItem('theme');\n" +
                    "    if (savedTheme === 'dark') {\n" +
                    "        document.body.classList.add('dark-mode');\n" +
                    "        document.getElementById('theme-toggle').innerHTML = '🌞';\n" +
                    "        document.getElementById('theme-toggle').setAttribute('title', '切换到浅色模式');\n" +
                    "    }\n" +
                    "}\n" +
                    "loadTheme();\n" +
                    "</script>\n" +
                    "</body>\n</html>";
        }

        /**
         * 生成CSS样式
         * @return CSS样式字符串
         */
        private String generateCssStyles() {
            return """
                    body { 
                        font-family: 'Noto Sans SC', 'Microsoft YaHei', sans-serif; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        margin: 0; 
                        padding: 0; 
                        min-height: 100vh;
                        transition: background-color 0.3s, color 0.3s; 
                    }
                    .dark-mode { 
                        background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
                        color: #e0e0e0; 
                    }
                    .container { 
                        max-width: 900px; 
                        margin: 2rem auto; 
                        padding: 2rem; 
                        background: rgba(255, 255, 255, 0.95); 
                        border-radius: 15px; 
                        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
                        backdrop-filter: blur(10px);
                        transition: background 0.3s; 
                    }
                    .dark-mode .container { 
                        background: rgba(30, 30, 30, 0.95); 
                    }
                    header {
                        text-align: center;
                        margin-bottom: 2rem;
                        border-bottom: 2px solid #f0f0f0;
                        padding-bottom: 1rem;
                    }
                    .dark-mode header {
                        border-bottom-color: #444;
                    }
                    h1 { 
                        color: #2c3e50; 
                        margin-bottom: 0.5rem;
                        font-size: 2.5rem;
                        text-shadow: 2px 2px 4px rgba(0,0,0,0.1);
                    }
                    .dark-mode h1 { 
                        color: #ecf0f1; 
                    }
                    .subtitle {
                        color: #7f8c8d;
                        font-size: 1.1rem;
                        margin-top: 0;
                    }
                    .dark-mode .subtitle {
                        color: #bdc3c7;
                    }
                    table { 
                        width: 100%; 
                        border-collapse: collapse; 
                        margin-bottom: 2rem;
                        border-radius: 10px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    th, td { 
                        padding: 15px; 
                        text-align: left; 
                        border-bottom: 1px solid #e0e0e0; 
                    }
                    th { 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white; 
                        font-weight: 700;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                    .dark-mode th { 
                        background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%);
                    }
                    tr:hover { 
                        background-color: rgba(102, 126, 234, 0.1); 
                        transform: translateY(-1px);
                        transition: all 0.2s ease;
                    }
                    .dark-mode tr:hover { 
                        background-color: rgba(52, 73, 94, 0.3); 
                    }
                    .pagination { 
                        display: flex; 
                        justify-content: center; 
                        margin-top: 2rem;
                        gap: 10px;
                    }
                    .pagination a { 
                        display: inline-block; 
                        margin: 0 5px; 
                        padding: 12px 20px; 
                        border-radius: 25px; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white; 
                        text-decoration: none; 
                        transition: all 0.3s ease;
                        font-weight: 600;
                        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
                    }
                    .dark-mode .pagination a { 
                        background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%);
                    }
                    .pagination a:hover { 
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
                    }
                    .dark-mode .pagination a:hover { 
                        box-shadow: 0 6px 20px rgba(52, 73, 94, 0.6);
                    }
                    #theme-toggle { 
                        position: absolute; 
                        top: 20px; 
                        right: 20px; 
                        padding: 12px; 
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white; 
                        border: none; 
                        border-radius: 50%; 
                        cursor: pointer; 
                        transition: all 0.3s ease;
                        font-size: 1.2rem;
                        width: 50px;
                        height: 50px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
                    }
                    .dark-mode #theme-toggle { 
                        background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%);
                    }
                    #theme-toggle:hover {
                        transform: scale(1.1);
                    }
                    footer {
                        text-align: center;
                        margin-top: 2rem;
                        padding-top: 1rem;
                        border-top: 1px solid #e0e0e0;
                    }
                    .dark-mode footer {
                        border-top-color: #444;
                    }
                    .footer-text {
                        color: #7f8c8d;
                        font-size: 0.9rem;
                    }
                    .dark-mode .footer-text {
                        color: #95a5a6;
                    }
                    @media (max-width: 768px) {
                        .container { 
                            margin: 1rem; 
                            padding: 1.5rem; 
                        }
                        h1 { 
                            font-size: 2rem; 
                        }
                        th, td { 
                            padding: 12px 8px; 
                            font-size: 14px; 
                        }
                        #theme-toggle {
                            top: 10px;
                            right: 10px;
                            width: 45px;
                            height: 45px;
                        }
                    }
                    @media (max-width: 480px) {
                        .container { 
                            padding: 1rem; 
                        }
                        h1 { 
                            font-size: 1.5rem; 
                        }
                        th, td { 
                            padding: 10px 6px; 
                            font-size: 12px; 
                        }
                        .pagination a {
                            padding: 10px 16px;
                            font-size: 14px;
                        }
                    }
                    """;
        }

        /**
         * 生成表格HTML
         * @param sortedPlayerData 排序后的玩家数据
         * @param startIndex 开始索引
         * @param endIndex 结束索引
         * @return 表格HTML字符串
         */
        private String generateTable(List<Entry<String, Integer>> sortedPlayerData, int startIndex, int endIndex) {
            StringBuilder tableHtml = new StringBuilder();
            tableHtml.append("<table>\n<tr>\n<th>排名</th>\n<th>玩家名称</th>\n<th>收集数量</th>\n</tr>\n");

            for (int i = startIndex; i < endIndex; i++) {
                Entry<String, Integer> entry = sortedPlayerData.get(i);
                String rankClass = getRankClass(i + 1);
                tableHtml.append("<tr>\n")
                         .append("<td class=\"").append(rankClass).append("\">").append(i + 1).append("</td>\n")
                         .append("<td>").append(escapeHtml(entry.getKey())).append("</td>\n")
                         .append("<td><strong>").append(entry.getValue()).append(" 个</strong></td>\n")
                         .append("</tr>\n");
            }
            tableHtml.append("</table>\n");
            return tableHtml.toString();
        }

        /**
         * 根据排名获取CSS类名
         * @param rank 排名
         * @return CSS类名
         */
        private String getRankClass(int rank) {
            switch (rank) {
                case 1: return "rank-first";
                case 2: return "rank-second";
                case 3: return "rank-third";
                default: return "rank-other";
            }
        }

        /**
         * 转义HTML特殊字符
         * @param text 原始文本
         * @return 转义后的文本
         */
        private String escapeHtml(String text) {
            return text.replace("&", "&amp;")
                      .replace("<", "&lt;")
                      .replace(">", "&gt;")
                      .replace("\"", "&quot;")
                      .replace("'", "&#39;");
        }

        /**
         * 生成分页链接
         * @param totalEntries 总条目数
         * @param currentPage 当前页码
         * @return 分页链接HTML字符串
         */
        private String generatePaginationLinks(int totalEntries, int currentPage) {
            int totalPages = (int) Math.ceil((double) totalEntries / ENTRIES_PER_PAGE);
            StringBuilder paginationHtml = new StringBuilder();

            if (totalPages > 1) {
                paginationHtml.append("<div class=\"pagination\">\n");
                
                // 上一页链接
                if (currentPage > 1) {
                    paginationHtml.append("<a href=\"?page=").append(currentPage - 1).append("\" class=\"pagination-link\">⬅️ 上一页</a>\n");
                }
                
                // 页码信息
                paginationHtml.append("<span style=\"margin: 0 15px; color: #7f8c8d; font-weight: 600;\">")
                             .append("第 ").append(currentPage).append(" 页 / 共 ").append(totalPages).append(" 页")
                             .append("</span>\n");
                
                // 下一页链接
                if (currentPage < totalPages) {
                    paginationHtml.append("<a href=\"?page=").append(currentPage + 1).append("\" class=\"pagination-link\">下一页 ➡️</a>\n");
                }
                
                paginationHtml.append("</div>\n");
            }
            return paginationHtml.toString();
        }

        /**
         * 发送HTTP响应
         * @param exchange HTTP交换对象
         * @param response 响应内容
         * @throws IOException 发送失败时抛出
         */
        private void sendResponse(HttpExchange exchange, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
            
            exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes("UTF-8"));
            }
        }
    }
}