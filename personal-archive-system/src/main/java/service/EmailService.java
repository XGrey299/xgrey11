package service;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailService {
    
    // 配置你的邮箱信息（以QQ邮箱为例）
    private static final String SMTP_HOST = "smtp.qq.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "your_email@qq.com";  // 改成你的邮箱
    private static final String PASSWORD = "your_smtp_password"; // 改成你的SMTP授权码
    private static final String FROM_EMAIL = "your_email@qq.com"; // 改成你的邮箱
    
    public static void sendVerificationEmail(String toEmail, String verificationCode) {
        String subject = "请验证您的邮箱地址 - 个人档案管理系统";
        String verificationLink = "http://localhost:4567/verify?email=" + toEmail + "&code=" + verificationCode;
        String content = "<h3>欢迎注册个人档案管理系统</h3>" +
                        "<p>请点击以下链接验证您的邮箱地址：</p>" +
                        "<a href=\"" + verificationLink + "\">" + verificationLink + "</a>" +
                        "<p>如果链接无法点击，请复制以上链接到浏览器地址栏中打开。</p>" +
                        "<p>此链接24小时内有效。</p>";
        
        sendEmail(toEmail, subject, content);
    }
    
    public static void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "重置您的密码 - 个人档案管理系统";
        String resetLink = "http://localhost:4567/reset-password?token=" + resetToken + "&email=" + toEmail;
        String content = "<h3>密码重置请求</h3>" +
                        "<p>我们收到了您重置密码的请求，请点击以下链接重置密码：</p>" +
                        "<a href=\"" + resetLink + "\">" + resetLink + "</a>" +
                        "<p>如果这不是您发起的请求，请忽略此邮件。</p>" +
                        "<p>此链接1小时内有效。</p>";
        
        sendEmail(toEmail, subject, content);
    }
    
    private static void sendEmail(String toEmail, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });
        
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");
            
            Transport.send(message);
            System.out.println("邮件发送成功至: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("邮件发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}