package com.quickcash.dto;

import java.math.BigDecimal;

public class ManagerDashboardResponse {

    private long totalBorrowers;
    private long pendingBorrowers;
    private long approvedBorrowers;

    private long totalLoans;
    private long pendingLoans;
    private long approvedLoans;
    private long activeLoans;
    private long completedLoans;

    private BigDecimal totalDisbursed;
    private BigDecimal totalCollected;
    private BigDecimal totalOutstanding;

    public ManagerDashboardResponse() {
    }

    public long getTotalBorrowers() {
        return totalBorrowers;
    }

    public void setTotalBorrowers(long totalBorrowers) {
        this.totalBorrowers = totalBorrowers;
    }

    public long getPendingBorrowers() {
        return pendingBorrowers;
    }

    public void setPendingBorrowers(long pendingBorrowers) {
        this.pendingBorrowers = pendingBorrowers;
    }

    public long getApprovedBorrowers() {
        return approvedBorrowers;
    }

    public void setApprovedBorrowers(long approvedBorrowers) {
        this.approvedBorrowers = approvedBorrowers;
    }

    public long getTotalLoans() {
        return totalLoans;
    }

    public void setTotalLoans(long totalLoans) {
        this.totalLoans = totalLoans;
    }

    public long getPendingLoans() {
        return pendingLoans;
    }

    public void setPendingLoans(long pendingLoans) {
        this.pendingLoans = pendingLoans;
    }

    public long getApprovedLoans() {
        return approvedLoans;
    }

    public void setApprovedLoans(long approvedLoans) {
        this.approvedLoans = approvedLoans;
    }

    public long getActiveLoans() {
        return activeLoans;
    }

    public void setActiveLoans(long activeLoans) {
        this.activeLoans = activeLoans;
    }

    public long getCompletedLoans() {
        return completedLoans;
    }

    public void setCompletedLoans(long completedLoans) {
        this.completedLoans = completedLoans;
    }

    public BigDecimal getTotalDisbursed() {
        return totalDisbursed;
    }

    public void setTotalDisbursed(BigDecimal totalDisbursed) {
        this.totalDisbursed = totalDisbursed;
    }

    public BigDecimal getTotalCollected() {
        return totalCollected;
    }

    public void setTotalCollected(BigDecimal totalCollected) {
        this.totalCollected = totalCollected;
    }

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(BigDecimal totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
    }
}