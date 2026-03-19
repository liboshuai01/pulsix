package cn.liboshuai.pulsix.module.risk.convert.accesssource;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourceSimpleRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.eventmodel.vo.EventBindingSourceItemVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface AccessSourceConvert {

    AccessSourceConvert INSTANCE = Mappers.getMapper(AccessSourceConvert.class);

    AccessSourceDO convert(AccessSourceSaveReqVO bean);

    @Mapping(target = "deletable", ignore = true)
    @Mapping(target = "deleteBlockedReason", ignore = true)
    AccessSourceRespVO convert(AccessSourceDO bean);

    AccessSourceSimpleRespVO convertSimple(AccessSourceDO bean);

    EventBindingSourceItemVO convertBindingItem(AccessSourceDO bean);

    List<EventBindingSourceItemVO> convertBindingItemList(List<AccessSourceDO> list);

    List<AccessSourceSimpleRespVO> convertSimpleList(List<AccessSourceDO> list);

    PageResult<AccessSourceRespVO> convertPage(PageResult<AccessSourceDO> page);

}
