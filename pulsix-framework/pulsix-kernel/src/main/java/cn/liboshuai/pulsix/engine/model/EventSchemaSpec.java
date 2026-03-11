package cn.liboshuai.pulsix.engine.model;

import cn.liboshuai.pulsix.engine.flink.typeinfo.EngineTypeInfoFactories;
import cn.liboshuai.pulsix.engine.support.CollectionCopier;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@TypeInfo(EngineTypeInfoFactories.EventSchemaSpecTypeInfoFactory.class)
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
