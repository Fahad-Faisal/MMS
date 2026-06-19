// src/main/java/com/mms/service/EmailService.java
package com.mms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final FeeService feeService;

    @Value("${email.from}")
    private String fromEmail;

    @Value("${app.url}")
    private String appUrl;

    public void sendTwoFactorCode(String to, String name, String code) {
        try {
            log.info("Sending 2FA code to: {}", to);

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("code", code);
            context.setVariable("expiresIn", "5");
            context.setVariable("purpose", "LOGIN");
            context.setVariable("appUrl", appUrl);

            String html = templateEngine.process("2fa-code", context);
            sendHtmlEmail(to, "Your MMS Verification Code", html);

            log.info("2FA code sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send 2FA code to: {}", to, e);
            // Don't throw exception - just log it
        }
    }

    public void sendTwoFactorCode(String to, String name, String code, String purpose) {
        try {
            log.info("Sending 2FA code to: {} for purpose: {}", to, purpose);

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("code", code);
            context.setVariable("expiresIn", "5");
            context.setVariable("purpose", purpose);
            context.setVariable("appUrl", appUrl);

            String html = templateEngine.process("2fa-code", context);
            String subject = purpose.equals("ENABLE") ?
                    "Enable Two-Factor Authentication - MMS" :
                    "Your MMS Verification Code";
            sendHtmlEmail(to, subject, html);

            log.info("2FA code sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send 2FA code to: {}", to, e);
            // Don't throw exception - just log it
        }
    }

    public void sendFeeReminder(String to, String studentName, int overdueAmount, int months) {
        try {
            log.info("Sending fee reminder to: {} for student: {}", to, studentName);

            Context context = new Context();
            context.setVariable("studentName", studentName);
            context.setVariable("overdueAmount", overdueAmount);
            context.setVariable("months", months);
            context.setVariable("dueDate", LocalDate.now().plusDays(5).format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            context.setVariable("appUrl", appUrl);

            String html = templateEngine.process("fee-reminder", context);
            sendHtmlEmail(to, "Tuition Fee Reminder - MMS", html);

            log.info("Fee reminder sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send fee reminder to: {}", to, e);
            // Don't throw exception - just log it
        }
    }

    public void sendWelcomeEmail(String to, String name, String username, String role) {
        try {
            log.info("Sending welcome email to: {}", to);

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("username", username);
            context.setVariable("role", role);
            context.setVariable("loginUrl", appUrl + "/login");

            String html = templateEngine.process("welcome", context);
            sendHtmlEmail(to, "Welcome to MMS - Madrasa Management System", html);

            log.info("Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
            // Don't throw exception - just log it
        }
    }

    @Scheduled(cron = "${email.reminder-cron:0 0 12 28 * ?}")
    public void sendMonthlyReminders() {
        log.info("Starting monthly fee reminders...");

        try {
            LocalDate today = LocalDate.now();
            // Send reminders on 25th-28th of each month
            if (today.getDayOfMonth() < 25 || today.getDayOfMonth() > 28) {
                log.info("Not sending reminders - current day is: {}", today.getDayOfMonth());
                return;
            }

            var defaulters = feeService.getDefaulters();
            log.info("Found {} defaulters", defaulters.size());

            int sentCount = 0;
            for (var defaulter : defaulters) {
                // In production, you would fetch the guardian email from the student record
                // This is a placeholder - you need to implement email lookup
                String guardianEmail = "guardian@example.com"; // Replace with actual lookup

                if (guardianEmail != null && !guardianEmail.isEmpty()) {
                    sendFeeReminder(
                            guardianEmail,
                            defaulter.getStudentName(),
                            defaulter.getOverdueAmount(),
                            defaulter.getMonthsOverdue()
                    );
                    sentCount++;
                }
            }

            log.info("Sent {} fee reminders", sentCount);
        } catch (Exception e) {
            log.error("Failed to send monthly reminders", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
            log.debug("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}