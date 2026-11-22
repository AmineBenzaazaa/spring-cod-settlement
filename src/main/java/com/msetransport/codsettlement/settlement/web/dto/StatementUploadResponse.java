package com.msetransport.codsettlement.settlement.web.dto;

/**
 * @param duplicate {@code true} when byte-identical content was already uploaded by this
 *                  merchant; {@code statement} is then the existing statement
 */
public record StatementUploadResponse(boolean duplicate, StatementResponse statement) {

    public static StatementUploadResponse created(StatementResponse statement) {
        return new StatementUploadResponse(false, statement);
    }

    public static StatementUploadResponse duplicateOf(StatementResponse statement) {
        return new StatementUploadResponse(true, statement);
    }
}
