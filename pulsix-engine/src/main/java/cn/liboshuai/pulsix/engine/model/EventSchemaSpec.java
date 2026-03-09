package cn.liboshuai.pulsix.engine.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
public class EventSchemaSpec implements Serializable {

    private String eventCode;

    private String eventType;

    private List<String> requiredFields;

    private List<String> optionalFields;

}
