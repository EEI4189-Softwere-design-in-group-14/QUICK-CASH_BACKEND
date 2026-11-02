
package com.quickcash.service;

import com.quickcash.entity.SmsNotification;
import com.quickcash.entity.SmsNotificationStatus;
import com.quickcash.repository.SmsNotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(
        name = "quickcash.sms.dispatch-enabled",
        havingValue = "true"
)
public class SmsDispatchService {

    private static final Logger log =
            LoggerFactory.getLogger(SmsDispatchService.class);

    private final SmsNotificationRepository smsRepository;
    private final SmsGateway smsGateway;

    public SmsDispatchService(
            SmsNotificationRepository smsRepository,
            SmsGateway smsGateway
    ) {
        this.smsRepository = smsRepository;
        this.smsGateway = smsGateway;
    }

    @Scheduled(
            fixedDelayString = "${quickcash.sms.poll-ms:30000}",
            initialDelayString = "${quickcash.sms.initial-delay-ms:10000}"
    )
    public void processPendingSms() {

        List<SmsNotification> pendingMessages =
                smsRepository.findTop10ByStatusOrderByCreatedAtAsc(
                        SmsNotificationStatus.PENDING
                );

        for (SmsNotification sms : pendingMessages) {

            Long smsId = sms.getSmsId();

            // Only one worker can claim this SMS.
            int claimed = smsRepository.claimSms(
                    smsId,
                    SmsNotificationStatus.PENDING,
                    SmsNotificationStatus.PROCESSING
            );

            if (claimed != 1) {
                continue;
            }

            try {
                SmsNotification claimedSms =
                        smsRepository.findById(smsId)
                                .orElseThrow();

                // External gateway call happens outside
                // the payment database transaction.
                smsGateway.sendSms(
                        claimedSms.getRecipientPhone(),
                        claimedSms.getMessageBody()
                );

                int updated = smsRepository.markSent(
                        smsId,
                        SmsNotificationStatus.PROCESSING,
                        SmsNotificationStatus.SENT,
                        LocalDateTime.now()
                );

                if (updated == 1) {
                    log.info(
                            "SMS {} accepted by gateway",
                            smsId
                    );
                } else {
                    log.warn(
                            "SMS {} needs status review",
                            smsId
                    );
                }

            } catch (Exception exception) {

                // Do not automatically resend.
                // Gateway outcome may be uncertain.
                try {
                    smsRepository.markFailed(
                            smsId,
                            SmsNotificationStatus.PROCESSING,
                            SmsNotificationStatus.FAILED,
                            "Dispatch failed; verify delivery before retry"
                    );
                } catch (Exception databaseException) {
                    log.error(
                            "Could not update status for SMS {}",
                            smsId
                    );
                }

                log.warn(
                        "SMS {} dispatch failed; manual review required",
                        smsId
                );
            }
        }
    }
}
