package model;
import java.time.LocalDateTime;
public class User {
    private int id;
    private String email;
    private String passwordHash;
    private String username;
    private boolean isVerified;
    private String verificationCode;
    private LocalDateTime verificationExpires;
    private String resetToken;
    private LocalDateTime resetExpires;
    private LocalDateTime createdAt;
    
    // 默认构造函数
    public User() {}
    
    // 带参数构造函数
    public User(int id, String email, String username, boolean isVerified) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.isVerified = isVerified;
    }
    
    // Getter 和 Setter 方法
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }
    
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    
    public LocalDateTime getVerificationExpires() { return verificationExpires; }
    public void setVerificationExpires(LocalDateTime verificationExpires) { this.verificationExpires = verificationExpires; }
    
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }
    
    public LocalDateTime getResetExpires() { return resetExpires; }
    public void setResetExpires(LocalDateTime resetExpires) { this.resetExpires = resetExpires; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
