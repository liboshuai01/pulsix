package cn.liboshuai.pulsix.engine.core;

import cn.liboshuai.pulsix.engine.model.EngineErrorCodes;
import cn.liboshuai.pulsix.engine.model.EngineErrorTypes;
import cn.liboshuai.pulsix.engine.model.EngineType;

public class DecisionExecutionException extends RuntimeException {

    private final String stage;

    private final String errorType;

    private final String errorCode;

    private final String featureCode;

    private final String ruleCode;

    private final EngineType engineType;

    private DecisionExecutionException(String stage,
                                       String errorType,
                                       String errorCode,
                                       String featureCode,
                                       String ruleCode,
                                       EngineType engineType,
                                       String message,
                                       Throwable cause) {
        super(message, cause);
        this.stage = stage;
        this.errorType = errorType;
        this.errorCode = errorCode;
        this.featureCode = featureCode;
        this.ruleCode = ruleCode;
        this.engineType = engineType;
    }

    public static DecisionExecutionException eventValidation(Throwable cause) {
        return new DecisionExecutionException(
                "decision-validate",
                EngineErrorTypes.INPUT,
                EngineErrorCodes.EVENT_VALIDATION_FAILED,
                null,
                null,
                null,
                cause == null ? "validate event failed" : cause.getMessage(),
                cause
        );
    }

    public static DecisionExecutionException stateAccess(String featureCode, Throwable cause) {
        return new DecisionExecutionException(
                "decision-stream-feature",
                EngineErrorTypes.STATE,
                EngineErrorCodes.STATE_ACCESS_FAILED,
                featureCode,
                null,
                null,
                buildMessage("stream feature state access failed", featureCode, cause),
                cause
        );
    }

    public static DecisionExecutionException derivedFeature(String featureCode,
                                                            EngineType engineType,
                                                            Throwable cause) {
        return new DecisionExecutionException(
                "decision-derived",
                EngineErrorTypes.EXECUTION,
                EngineErrorCodes.DERIVED_EXECUTION_FAILED,
                featureCode,
                null,
                engineType,
                buildMessage("derived feature execution failed", featureCode, cause),
                cause
        );
    }

    public static DecisionExecutionException ruleEvaluation(String ruleCode,
                                                            EngineType engineType,
                                                            Throwable cause) {
        return new DecisionExecutionException(
                "decision-rule",
                EngineErrorTypes.EXECUTION,
                EngineErrorCodes.RULE_EXECUTION_FAILED,
                null,
                ruleCode,
                engineType,
                buildMessage("rule execution failed", ruleCode, cause),
                cause
        );
    }

    public String getStage() {
        return stage;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public EngineType getEngineType() {
        return engineType;
    }

    private static String buildMessage(String prefix, String code, Throwable cause) {
        String base = code == null || code.isBlank() ? prefix : prefix + ": " + code;
        String causeMessage = cause == null ? null : cause.getMessage();
        if (causeMessage == null || causeMessage.isBlank()) {
            return base;
        }
        return base + ", cause=" + causeMessage;
    }

}
