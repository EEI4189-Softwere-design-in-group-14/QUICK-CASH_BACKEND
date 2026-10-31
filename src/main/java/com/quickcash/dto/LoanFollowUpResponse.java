
package com.quickcash.dto;

import com.quickcash.entity.FollowUpOutcome;
import com.quickcash.entity.LoanFollowUp;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanFollowUpResponse(

        Long noteId,
        Long loanId,
        String loanCode,
        Long collectorUserId,
        String collectorName,
        FollowUpOutcome outcome,
        String note,
        LocalDate nextFollowUpDate,
        LocalDateTime createdAt

) {
    public static LoanFollowUpResponse from(
            LoanFollowUp followUp
    ) {
        return new LoanFollowUpResponse(
                followUp.getNoteId(),
                followUp.getLoan().getLoanId(),
                followUp.getLoan().getLoanCode(),
                followUp.getCollector().getUserId(),
                followUp.getCollector().getFullName(),
                followUp.getOutcome(),
                followUp.getNote(),
                followUp.getNextFollowUpDate(),
                followUp.getCreatedAt()
        );
    }
}
