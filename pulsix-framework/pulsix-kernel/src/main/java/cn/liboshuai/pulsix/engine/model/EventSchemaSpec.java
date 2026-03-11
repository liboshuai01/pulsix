package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.support.CollectionCopier;
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

    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = CollectionCopier.copyList(requiredFields);
    }

    public void setOptionalFields(List<String> optionalFields) {
        this.optionalFields = CollectionCopier.copyList(optionalFields);
    }

}
