package com.msetransport.codsettlement.settlement.service;

public record ProcessingResult(long statementId, Outcome outcome, ReconciliationSummary summary, String error) {

    public enum Outcome { RECONCILED, SKIPPED, FAILED }

    static ProcessingResult reconciled(long id, ReconciliationSummary summary) {
        return new ProcessingResult(id, Outcome.RECONCILED, summary, null);
    }

    static ProcessingResult skipped(long id) {
        return new ProcessingResult(id, Outcome.SKIPPED, null, null);
    }

    static ProcessingResult failed(long id, String error) {
        return new ProcessingResult(id, Outcome.FAILED, null, error);
    }
}
