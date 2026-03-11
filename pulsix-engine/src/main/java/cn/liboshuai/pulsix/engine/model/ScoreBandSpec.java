package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class ScoreBandSpec implements Serializable {

    private String code;

    private Integer minScore;

    private Integer maxScore;

    private ActionType action;

    private String reason;

}
