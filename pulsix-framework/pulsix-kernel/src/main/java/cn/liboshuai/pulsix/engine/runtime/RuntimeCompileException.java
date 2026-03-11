package cn.liboshuai.pulsix.engine.runtime;

import cn.liboshuai.pulsix.engine.model.EngineType;

public class RuntimeCompileException extends RuntimeException {

    private final String stage;

    private final String errorCode;

    private final String featureCode;

    private final String ruleCode;

    private final EngineType engineType;

    private RuntimeCompileException(String errorCode,
                                    String featureCode,
                                    String ruleCode,
                                    EngineType engineType,
                                    String message,
                                    Throwable cause) {
        super(message, cause);
        this.stage = "snapshot-compile";
        this.errorCode = errorCode;
        this.featureCode = featureCode;
        this.ruleCode = ruleCode;
        this.engineType = engineType;
    }

    public static RuntimeCompileException streamFeature(String featureCode,
                                                        EngineType engineType,
                                                        String errorCode,
                                                        Throwable cause) {
        return new RuntimeCompileException(errorCode,
                featureCode,
                null,
                engineType,
                buildMessage("compile stream feature failed", featureCode, cause),
                cause);
    }

    public static RuntimeCompileException lookupFeature(String featureCode,
                                                        EngineType engineType,
                                                        String errorCode,
                                                        Throwable cause) {
        return new RuntimeCompileException(errorCode,
                featureCode,
                null,
                engineType,
                buildMessage("compile lookup feature failed", featureCode, cause),
                cause);
    }

    public static RuntimeCompileException derivedFeature(String featureCode,
                                                         EngineType engineType,
                                                         String errorCode,
                                                         Throwable cause) {
        return new RuntimeCompileException(errorCode,
                featureCode,
                null,
                engineType,
                buildMessage("compile derived feature failed", featureCode, cause),
                cause);
    }

    public static RuntimeCompileException rule(String ruleCode,
                                               EngineType engineType,
                                               String errorCode,
                                               Throwable cause) {
        return new RuntimeCompileException(errorCode,
                null,
                ruleCode,
                engineType,
                buildMessage("compile rule failed", ruleCode, cause),
                cause);
    }

    public String getStage() {
        return stage;
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
