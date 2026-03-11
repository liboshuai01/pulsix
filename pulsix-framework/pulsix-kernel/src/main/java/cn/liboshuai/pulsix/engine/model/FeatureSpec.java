package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public abstract class FeatureSpec implements Serializable {

    private String code;

    private String name;

    private FeatureType type;

    private String valueType;

    private String description;

}
