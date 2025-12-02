package service;

import model.User;
import util.DatabaseUtil;
import util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserService {
    
    // 用户注册
    public static boolean registerUser(String email, String password, String username) {
        // 检查邮箱是否已存在
        if (isEmailExists(email)) {
            return false;
        }
        
        String sql = "INSERT INTO users (email, password_hash, username, verification_code, verification_expires) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String verificationCode = UUID.randomUUID().toString();
            LocalDateTime expires = LocalDateTime.now().plusHours(24); // 24小时有效
            
            pstmt.setString(1, email);
            pstmt.setString(2, PasswordUtil.hashPassword(password));
            pstmt.setString(3, username);
            pstmt.setString(4, verificationCode);
            pstmt.setTimestamp(5, Timestamp.valueOf(expires));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // 发送验证邮件
                EmailService.sendVerificationEmail(email, verificationCode);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("注册用户失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // 检查邮箱是否存在
    private static boolean isEmailExists(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // 用户登录
    public static User loginUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                boolean isVerified = rs.getBoolean("is_verified");
                
                // 检查密码
                if (PasswordUtil.checkPassword(password, storedHash)) {
                    if (!isVerified) {
                        throw new RuntimeException("邮箱未验证，请先验证邮箱");
                    }
                    
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setEmail(rs.getString("email"));
                    user.setUsername(rs.getString("username"));
                    user.setVerified(isVerified);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("登录失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    // 邮箱验证
    public static boolean verifyEmail(String email, String code) {
        String sql = "UPDATE users SET is_verified = TRUE, verification_code = NULL, verification_expires = NULL " +
                    "WHERE email = ? AND verification_code = ? AND verification_expires > ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            pstmt.setString(2, code);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("邮箱验证失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // 生成密码重置令牌
    public static boolean generateResetToken(String email) {
        String sql = "UPDATE users SET reset_token = ?, reset_expires = ? WHERE email = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String resetToken = UUID.randomUUID().toString();
            LocalDateTime expires = LocalDateTime.now().plusHours(1); // 1小时有效
            
            pstmt.setString(1, resetToken);
            pstmt.setTimestamp(2, Timestamp.valueOf(expires));
            pstmt.setString(3, email);
            
            if (pstmt.executeUpdate() > 0) {
                EmailService.sendPasswordResetEmail(email, resetToken);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("生成重置令牌失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // 重置密码
    public static boolean resetPassword(String email, String token, String newPassword) {
        String sql = "UPDATE users SET password_hash = ?, reset_token = NULL, reset_expires = NULL " +
                    "WHERE email = ? AND reset_token = ? AND reset_expires > ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, PasswordUtil.hashPassword(newPassword));
            pstmt.setString(2, email);
            pstmt.setString(3, token);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("重置密码失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // 根据ID获取用户
    public static User getUserById(int userId) {
        String sql = "SELECT id, email, username, is_verified FROM users WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setUsername(rs.getString("username"));
                user.setVerified(rs.getBoolean("is_verified"));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("获取用户失败: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
