package com.rayworld.firesafety.alert.service;

import com.rayworld.firesafety.alert.event.AlertNotificationEvent;
import com.rayworld.firesafety.alert.model.AlertSource;
import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.alert.model.AlertType;
import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.config.firebase.FirebaseProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock
    private AuthMapper authMapper;

    private FcmPushService fcmPushService;

    @BeforeEach
    void setUp() {
        FirebaseProperties firebaseProperties = new FirebaseProperties();
        firebaseProperties.setEnabled(false);
        fcmPushService = new FcmPushService(authMapper, firebaseProperties);
    }

    @Test
    @DisplayName("FR-04-02: Firebase 설정이 꺼져 있으면 FCM 발송을 건너뛴다")
    void skipFcmWhenFirebaseDisabled() {
        // when
        fcmPushService.sendAlert(event());

        // then
        verify(authMapper, never()).findFcmTokensForAlertSite(org.mockito.Mockito.anyLong());
    }

    private AlertNotificationEvent event() {
        return new AlertNotificationEvent(
                100L,
                3L,
                10L,
                20L,
                AlertSource.DEVICE,
                AlertType.ARC,
                AlertStatus.UNCONFIRMED,
                "ALERT_CREATED"
        );
    }
}
