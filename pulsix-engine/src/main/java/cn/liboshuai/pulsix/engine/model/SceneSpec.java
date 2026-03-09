package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class SceneSpec implements Serializable {

    private String defaultPolicyCode;

    private List<String> allowedEventTypes;

    private Integer decisionTimeoutMs;

    private String logLevel;

}
