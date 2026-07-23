package com.rayworld.firesafety.auth.service;

import com.rayworld.firesafety.auth.config.PasswordResetProperties;
import com.rayworld.firesafety.auth.exception.AuthErrorCode;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.common.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class PasswordResetMailService {

    // SMTP 제공자가 Gmail/SES로 바뀌어도 JavaMailSender 설정만 교체
    private final JavaMailSender javaMailSender;

    // 비밀번호 재설정 링크와 발신자 정보
    private final PasswordResetProperties passwordResetProperties;

    // 비밀번호 재설정 링크 메일 발송
    public void sendPasswordResetMail(User user, String originalToken) {
        validateMailSettings();

        String resetLink = passwordResetProperties.getBaseUrl() + "?token=" + originalToken;

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(passwordResetProperties.getMailFromAddress(), passwordResetProperties.getMailFromName());
            helper.setTo(user.getEmail());
            helper.setSubject("[전기화재 예방 시스템] 비밀번호 재설정 안내");
            helper.setText("""
                    비밀번호 재설정 요청이 접수되었습니다.

                    아래 링크에서 새 비밀번호를 설정해주세요.
                    %s

                    이 링크는 %d분 동안만 사용할 수 있습니다.
                    본인이 요청하지 않았다면 이 메일을 무시해주세요.
                    """.formatted(resetLink, passwordResetProperties.getTokenExpirationMinutes()));

            javaMailSender.send(message);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_MAIL_SEND_FAILED);
        }
    }

    // SMTP 설정은 환경변수로 주입되어야 함
    private void validateMailSettings() {
        if (!StringUtils.hasText(passwordResetProperties.getBaseUrl())
                || !StringUtils.hasText(passwordResetProperties.getMailFromAddress())
                || !StringUtils.hasText(passwordResetProperties.getMailFromName())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_RESET_MAIL_SEND_FAILED);
        }
    }
}
