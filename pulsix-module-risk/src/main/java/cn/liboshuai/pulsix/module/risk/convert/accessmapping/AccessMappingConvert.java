package cn.liboshuai.pulsix.module.risk.convert.accessmapping;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingRuleItemVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessMappingSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accessmapping.vo.AccessRawFieldItemVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessMappingRuleDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accessmapping.EventAccessRawFieldDefDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.EventAccessBindingDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface AccessMappingConvert {

    AccessMappingConvert INSTANCE = Mappers.getMapper(AccessMappingConvert.class);

    EventAccessBindingDO convert(AccessMappingSaveReqVO bean);

    EventAccessRawFieldDefDO convert(AccessRawFieldItemVO bean);

    EventAccessMappingRuleDO convert(AccessMappingRuleItemVO bean);

    List<EventAccessRawFieldDefDO> convertRawFieldDOList(List<AccessRawFieldItemVO> list);

    List<EventAccessMappingRuleDO> convertMappingRuleDOList(List<AccessMappingRuleItemVO> list);

    AccessMappingRespVO convert(EventAccessBindingDO bean);

    AccessRawFieldItemVO convert(EventAccessRawFieldDefDO bean);

    AccessMappingRuleItemVO convert(EventAccessMappingRuleDO bean);

    List<AccessRawFieldItemVO> convertRawFieldList(List<EventAccessRawFieldDefDO> list);

    List<AccessMappingRuleItemVO> convertMappingRuleList(List<EventAccessMappingRuleDO> list);

    PageResult<AccessMappingRespVO> convertPage(PageResult<EventAccessBindingDO> page);

}
