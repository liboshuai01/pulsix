package cn.liboshuai.pulsix.module.risk.service.accessmapping.bo;

import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessMappingRuleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessRawFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventFieldDefDO;
import lombok.Data;

import java.util.List;

/**
 * 接入映射运行态聚合 BO
 */
@Data
public class AccessMappingRuntimeBO {

    private EventAccessBindingDO binding;

    private List<EventFieldDefDO> standardFields;

    private List<EventAccessRawFieldDefDO> rawFields;

    private List<EventAccessMappingRuleDO> mappingRules;

}
