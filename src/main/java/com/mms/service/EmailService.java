// src/main/java/com/mms/service/EmailService.java
package com.mms.service;

import com.mms.entity.Student;
import com.mms.repository.StudentRepository;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final FeeService feeService;
    private final StudentRepository studentRepository;

    @Value("${email.from:mms@madrasa.edu}")
    private String fromEmail;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${email.reminder-cron:0 0 12 28 * ?}")
    private String reminderCron;

    private static final String SUBJECT_2FA_LOGIN = "Your MMS Verification Code";
    private static final String SUBJECT_2FA_ENABLE = "Enable Two-Factor Authentication - MMS";
    private static final String SUBJECT_PASSWORD_RESET = "Password Reset - MMS";
    private static final String SUBJECT_FEE_REMINDER = "Tuition Fee Reminder - MMS";
    private static final String SUBJECT_WELCOME = "Welcome to MMS - Madrasa Management System";
    private static final String SUBJECT_INVOICE = "Tuition Invoice - MMS";
    private static final String SUBJECT_RECEIPT = "Payment Receipt - MMS";

    // ==================== 2FA Methods ====================

    public void sendTwoFactorCode(String to, String name, String code) {
        sendTwoFactorCode(to, name, code, "LOGIN");
    }

    public void sendTwoFactorCode(String to, String name, String code, String purpose) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send code to: {}, code: {}, purpose: {}", to, code, purpose);
            return;
        }

        try {
            log.info("📧 Sending 2FA code to: {} for purpose: {}", to, purpose);

            Context context = createBaseContext(name);
            context.setVariable("code", code);
            context.setVariable("expiresIn", "5");
            context.setVariable("purpose", purpose);

            String html = templateEngine.process("2fa-code", context);
            String subject = getSubjectForPurpose(purpose);

            sendHtmlEmail(to, subject, html);
            log.info("✅ 2FA code sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send 2FA code to: {}", to, e);
        }
    }

    // ==================== Forgot Password ====================

    public void sendPasswordResetCode(String to, String name, String code) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send password reset code to: {}, code: {}", to, code);
            return;
        }

        try {
            log.info("📧 Sending password reset code to: {}", to);

            Context context = createBaseContext(name);
            context.setVariable("code", code);
            context.setVariable("expiresIn", "5");
            context.setVariable("purpose", "PASSWORD_RESET");

            String html = templateEngine.process("2fa-code", context);
            sendHtmlEmail(to, SUBJECT_PASSWORD_RESET, html);

            log.info("✅ Password reset code sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send password reset code to: {}", to, e);
        }
    }

    // ==================== Fee Reminders ====================

    public void sendFeeReminder(String to, String studentName, int overdueAmount, int months) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send fee reminder to: {}, student: {}, amount: {}",
                    to, studentName, overdueAmount);
            return;
        }

        try {
            log.info("📧 Sending fee reminder to: {} for student: {}", to, studentName);

            Context context = createBaseContext(studentName);
            context.setVariable("studentName", studentName);
            context.setVariable("overdueAmount", overdueAmount);
            context.setVariable("months", months);
            context.setVariable("dueDate", LocalDate.now().plusDays(5)
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

            String html = templateEngine.process("fee-reminder", context);
            sendHtmlEmail(to, SUBJECT_FEE_REMINDER, html);

            log.info("✅ Fee reminder sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send fee reminder to: {}", to, e);
        }
    }

    @Scheduled(cron = "${email.reminder-cron:0 0 12 28 * ?}")
    public void sendMonthlyReminders() {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Skipping monthly reminders.");
            return;
        }

        log.info("📧 Starting monthly fee reminders...");

        try {
            LocalDate today = LocalDate.now();
            // Send reminders on 25th-28th of each month
            if (today.getDayOfMonth() < 25 || today.getDayOfMonth() > 28) {
                log.info("⏰ Not sending reminders - current day is: {}", today.getDayOfMonth());
                return;
            }

            var defaulters = feeService.getDefaulters();
            log.info("📊 Found {} defaulters", defaulters.size());

            int sentCount = 0;
            for (var defaulter : defaulters) {
                // Fetch student to get guardian email
                Student student = studentRepository.findById(defaulter.getStudentId()).orElse(null);
                if (student == null) {
                    log.warn("⚠️ Student not found for ID: {}", defaulter.getStudentId());
                    continue;
                }

                String guardianEmail = student.getGuardianEmail();
                if (guardianEmail != null && !guardianEmail.isEmpty()) {
                    sendFeeReminder(
                            guardianEmail,
                            defaulter.getStudentName(),
                            defaulter.getOverdueAmount(),
                            defaulter.getMonthsOverdue()
                    );
                    sentCount++;
                } else {
                    log.warn("⚠️ No guardian email for student: {}", defaulter.getStudentName());
                }
            }

            log.info("✅ Sent {} fee reminders", sentCount);
        } catch (Exception e) {
            log.error("❌ Failed to send monthly reminders", e);
        }
    }

    // ==================== Welcome Emails ====================

    public void sendWelcomeEmail(String to, String name, String username, String role) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send welcome email to: {}, username: {}", to, username);
            return;
        }

        try {
            log.info("📧 Sending welcome email to: {}", to);

            Context context = createBaseContext(name);
            context.setVariable("username", username);
            context.setVariable("role", role);
            context.setVariable("loginUrl", appUrl + "/login");

            String html = templateEngine.process("welcome", context);
            sendHtmlEmail(to, SUBJECT_WELCOME, html);

            log.info("✅ Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send welcome email to: {}", to, e);
        }
    }

    // ==================== Invoice Emails ====================

    public void sendInvoiceEmail(String to, String studentName, int amount, String month, String description) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send invoice to: {}, student: {}, amount: {}",
                    to, studentName, amount);
            return;
        }

        try {
            log.info("📧 Sending invoice to: {} for student: {}", to, studentName);

            Context context = createBaseContext(studentName);
            context.setVariable("studentName", studentName);
            context.setVariable("amount", amount);
            context.setVariable("month", month);
            context.setVariable("description", description);
            context.setVariable("dueDate", LocalDate.now().plusDays(15)
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

            String html = templateEngine.process("invoice", context);
            sendHtmlEmail(to, SUBJECT_INVOICE + " - " + month, html);

            log.info("✅ Invoice sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send invoice to: {}", to, e);
        }
    }

    // ==================== Receipt Emails ====================

    public void sendReceiptEmail(String to, String studentName, String receiptNo, int amount, String method) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send receipt to: {}, student: {}, amount: {}",
                    to, studentName, amount);
            return;
        }

        try {
            log.info("📧 Sending receipt to: {} for student: {}", to, studentName);

            Context context = createBaseContext(studentName);
            context.setVariable("studentName", studentName);
            context.setVariable("receiptNo", receiptNo);
            context.setVariable("amount", amount);
            context.setVariable("method", method);
            context.setVariable("date", LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));

            String html = templateEngine.process("receipt-email", context);
            sendHtmlEmail(to, SUBJECT_RECEIPT + " - " + receiptNo, html);

            log.info("✅ Receipt email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send receipt email to: {}", to, e);
        }
    }

    // ==================== Bulk Email Methods ====================

    public void sendBulkFeeReminders(List<Map<String, Object>> reminders) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send {} fee reminders", reminders.size());
            return;
        }

        log.info("📧 Sending {} fee reminders...", reminders.size());
        int successCount = 0;
        int failureCount = 0;

        for (Map<String, Object> reminder : reminders) {
            try {
                String to = (String) reminder.get("email");
                String studentName = (String) reminder.get("studentName");
                int overdueAmount = (int) reminder.get("overdueAmount");
                int months = (int) reminder.get("months");

                sendFeeReminder(to, studentName, overdueAmount, months);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("❌ Failed to send reminder: {}", e.getMessage());
            }
        }

        log.info("✅ Sent {} reminders, {} failed", successCount, failureCount);
    }

    // ==================== Helper Methods ====================

    private Context createBaseContext(String name) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("appUrl", appUrl);
        context.setVariable("year", LocalDate.now().getYear());
        return context;
    }

    private String getSubjectForPurpose(String purpose) {
        switch (purpose) {
            case "ENABLE":
                return SUBJECT_2FA_ENABLE;
            case "PASSWORD_RESET":
                return SUBJECT_PASSWORD_RESET;
            case "LOGIN":
            default:
                return SUBJECT_2FA_LOGIN;
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
            log.debug("📧 Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("❌ Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendTestEmail(String to) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL DISABLED] Would send test email to: {}", to);
            return;
        }

        try {
            log.info("📧 Sending test email to: {}", to);

            Context context = createBaseContext("Test User");
            context.setVariable("message", "This is a test email from MMS.");

            String html = templateEngine.process("test-email", context);
            sendHtmlEmail(to, "Test Email - MMS", html);

            log.info("✅ Test email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send test email to: {}", to, e);
        }
    }
}