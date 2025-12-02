package Main1;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.User;
import service.UserService;
import util.DatabaseUtil;
import spark.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static void main(String[] args) {
        // 第一步：配置端口
        port(4567);
        
        // 第二步：配置静态文件目录（必须放在路由之前）
        staticFiles.location("/public");
        staticFiles.expireTime(600L);
        
        // 第三步：启用 CORS
        enableCORS();
        
        // 第四步：初始化数据库
        initDatabase();
        
        // 第五步：全局前置过滤器
        before((request, response) -> {
            response.type("application/json");
        });
        
        // 第六步：认证过滤器
        before("/api/*", (request, response) -> {
            String path = request.pathInfo();
            if (!path.startsWith("/api/login") && 
                !path.startsWith("/api/register") && 
                !path.startsWith("/api/forgot-password") &&
                !path.startsWith("/api/reset-password") &&
                !path.equals("/api/user")) {
                
                User user = request.session().attribute("user");
                if (user == null) {
                    halt(401, "{\"error\": \"请先登录\"}");
                }
            }
        });
        
        // 第七步：注册路由（最后一步）
        setupRoutes();
        
        System.out.println("服务器启动成功：http://localhost:4567");
    }
    
    private static void enableCORS() {
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            
            return "OK";
        });
        
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });
    }
    
    private static void setupRoutes() {
        // 测试接口
        get("/api/test", (request, response) -> {
            return "{\"message\": \"服务器运行正常\"}";
        });
        
        // 注册接口
        post("/api/register", (request, response) -> {
            try {
                Map<String, String> params = objectMapper.readValue(request.body(), Map.class);
                String email = params.get("email");
                String password = params.get("password");
                String username = params.get("username");
                
                Map<String, Object> result = new HashMap<>();
                
                if (email == null || password == null || username == null) {
                    result.put("success", false);
                    result.put("message", "请填写所有必填字段");
                    return objectMapper.writeValueAsString(result);
                }
                
                if (UserService.registerUser(email, password, username)) {
                    result.put("success", true);
                    result.put("message", "注册成功，请查收验证邮件");
                } else {
                    result.put("success", false);
                    result.put("message", "注册失败，邮箱可能已被使用");
                }
                
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                response.status(400);
                return "{\"success\": false, \"message\": \"请求格式错误\"}";
            }
        });
        
        // 登录接口
        post("/api/login", (request, response) -> {
            try {
                Map<String, String> params = objectMapper.readValue(request.body(), Map.class);
                String email = params.get("email");
                String password = params.get("password");
                
                Map<String, Object> result = new HashMap<>();
                
                if (email == null || password == null) {
                    result.put("success", false);
                    result.put("message", "请填写邮箱和密码");
                    return objectMapper.writeValueAsString(result);
                }
                
                User user = UserService.loginUser(email, password);
                
                if (user != null) {
                    Session session = request.session(true);
                    session.attribute("user", user);
                    
                    result.put("success", true);
                    result.put("message", "登录成功");
                    result.put("user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "username", user.getUsername()
                    ));
                } else {
                    result.put("success", false);
                    result.put("message", "登录失败，请检查邮箱和密码");
                }
                
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                response.status(400);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", e.getMessage());
                try {
                    return objectMapper.writeValueAsString(result);
                } catch (Exception ex) {
                    return "{\"success\": false, \"message\": \"系统错误\"}";
                }
            }
        });
        
        // 邮箱验证接口
        get("/verify", (request, response) -> {
            String email = request.queryParams("email");
            String code = request.queryParams("code");
            
            if (email == null || code == null) {
                return "<html><body><h3>验证失败：参数不完整</h3></body></html>";
            }
            
            if (UserService.verifyEmail(email, code)) {
                return "<html><body>" +
                       "<h3>邮箱验证成功！</h3>" +
                       "<p>您的邮箱已成功验证，现在可以<a href='/login.html'>登录</a>系统了。</p>" +
                       "</body></html>";
            } else {
                return "<html><body>" +
                       "<h3>邮箱验证失败</h3>" +
                       "<p>验证链接无效或已过期，请重新注册或联系管理员。</p>" +
                       "</body></html>";
            }
        });
        
        // 请求重置密码
        post("/api/forgot-password", (request, response) -> {
            try {
                Map<String, String> params = objectMapper.readValue(request.body(), Map.class);
                String email = params.get("email");
                
                Map<String, Object> result = new HashMap<>();
                
                if (email == null) {
                    result.put("success", false);
                    result.put("message", "请输入邮箱地址");
                    return objectMapper.writeValueAsString(result);
                }
                
                if (UserService.generateResetToken(email)) {
                    result.put("success", true);
                    result.put("message", "重置链接已发送到您的邮箱");
                } else {
                    result.put("success", false);
                    result.put("message", "发送失败，请检查邮箱地址");
                }
                
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                response.status(400);
                return "{\"success\": false, \"message\": \"请求格式错误\"}";
            }
        });
        
        // 重置密码页面
        get("/reset-password", (request, response) -> {
            String email = request.queryParams("email");
            String token = request.queryParams("token");
            
            if (email == null || token == null) {
                return "<html><body><h3>错误：参数不完整</h3></body></html>";
            }
            
            return "<html>" +
                   "<head><title>重置密码</title></head>" +
                   "<body>" +
                   "<h3>重置密码</h3>" +
                   "<form id=\"resetForm\">" +
                   "<input type=\"hidden\" id=\"email\" value=\"" + email + "\">" +
                   "<input type=\"hidden\" id=\"token\" value=\"" + token + "\">" +
                   "<div><label>新密码：</label><input type=\"password\" id=\"newPassword\" required></div>" +
                   "<div><label>确认密码：</label><input type=\"password\" id=\"confirmPassword\" required></div>" +
                   "<button type=\"submit\">重置密码</button>" +
                   "</form>" +
                   "<div id=\"message\"></div>" +
                   "<script>" +
                   "document.getElementById('resetForm').addEventListener('submit', async function(e) {" +
                   "e.preventDefault();" +
                   "const newPassword = document.getElementById('newPassword').value;" +
                   "const confirmPassword = document.getElementById('confirmPassword').value;" +
                   "if (newPassword !== confirmPassword) {" +
                   "document.getElementById('message').innerHTML = '密码不一致'; return; }" +
                   "const response = await fetch('/api/reset-password', {" +
                   "method: 'POST', headers: {'Content-Type': 'application/json'}," +
                   "body: JSON.stringify({" +
                   "email: document.getElementById('email').value," +
                   "token: document.getElementById('token').value," +
                   "newPassword: newPassword" +
                   "})});" +
                   "const result = await response.json();" +
                   "document.getElementById('message').innerHTML = result.message;" +
                   "});" +
                   "</script>" +
                   "</body></html>";
        });
        
        // 执行密码重置
        post("/api/reset-password", (request, response) -> {
            try {
                Map<String, String> params = objectMapper.readValue(request.body(), Map.class);
                String email = params.get("email");
                String token = params.get("token");
                String newPassword = params.get("newPassword");
                
                Map<String, Object> result = new HashMap<>();
                
                if (email == null || token == null || newPassword == null) {
                    result.put("success", false);
                    result.put("message", "参数不完整");
                    return objectMapper.writeValueAsString(result);
                }
                
                if (newPassword.length() < 6) {
                    result.put("success", false);
                    result.put("message", "密码长度至少6位");
                    return objectMapper.writeValueAsString(result);
                }
                
                if (UserService.resetPassword(email, token, newPassword)) {
                    result.put("success", true);
                    result.put("message", "密码重置成功");
                } else {
                    result.put("success", false);
                    result.put("message", "重置失败，链接可能已过期");
                }
                
                return objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                response.status(400);
                return "{\"success\": false, \"message\": \"请求格式错误\"}";
            }
        });
        
        // 获取当前用户信息
        get("/api/user", (request, response) -> {
            User user = request.session().attribute("user");
            
            if (user != null) {
                try {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("username", user.getUsername());
                    return objectMapper.writeValueAsString(userInfo);
                } catch (Exception e) {
                    response.status(500);
                    return "{\"error\": \"服务器错误\"}";
                }
            } else {
                response.status(401);
                return "{\"error\": \"未登录\"}";
            }
        });
        
        // 退出登录
        post("/api/logout", (request, response) -> {
            request.session().invalidate();
            return "{\"success\": true, \"message\": \"退出成功\"}";
        });
    }
    
    private static void initDatabase() {
        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INT AUTO_INCREMENT PRIMARY KEY, " +
            "email VARCHAR(255) UNIQUE NOT NULL, " +
            "password_hash VARCHAR(255) NOT NULL, " +
            "username VARCHAR(100), " +
            "is_verified BOOLEAN DEFAULT FALSE, " +
            "verification_code VARCHAR(100), " +
            "verification_expires DATETIME, " +
            "reset_token VARCHAR(100), " +
            "reset_expires DATETIME, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")";
        
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("数据库表初始化成功");
        } catch (SQLException e) {
            System.err.println("数据库表初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}