package com.rayworld.firesafety.alert.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.rayworld.firesafety.alert.event.AlertNotificationEvent;
import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.config.firebase.FirebaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushService {

    private final AuthMapper authMapper;
    private final FirebaseProperties firebaseProperties;

    // 경보 푸시 발송
    // Firebase 설정이 없으면 local/test로 보고 발송을 건너뛴다.
    public void sendAlert(AlertNotificationEvent event) {
        if (!firebaseProperties.isReady() || event.getSiteId() == null) {
            return;
        }

        List<String> tokens = authMapper.findFcmTokensForAlertSite(event.getSiteId());
        if (CollectionUtils.isEmpty(tokens)) {
            return;
        }

        try {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle("전기화재 경보 발생")
                            .setBody(buildBody(event))
                            .build())
                    .putData("eventType", event.getEventType())
                    .putData("alertId", String.valueOf(event.getAlertId()))
                    .build();

            getFirebaseMessaging().sendEachForMulticast(message);
        } catch (RuntimeException | IOException | FirebaseMessagingException e) {
            // 푸시 실패가 경보 저장 흐름을 깨면 안 되므로 로그만 남긴다.
            log.warn("FCM 경보 발송 실패 - alertId={}, message={}", event.getAlertId(), e.getMessage());
        }
    }

    // Firebase Admin SDK 인스턴스 준비
    private FirebaseMessaging getFirebaseMessaging() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try (FileInputStream inputStream = new FileInputStream(firebaseProperties.getCredentialsPath())) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }
        return FirebaseMessaging.getInstance();
    }

    // 사용자에게 보여줄 짧은 푸시 본문 생성
    private String buildBody(AlertNotificationEvent event) {
        return event.getSource().name() + " " + event.getType().name() + " 경보가 발생했습니다";
    }
}
