package cn.liboshuai.pulsix.module.risk.convert.scene;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.SceneSimpleRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface SceneConvert {

    SceneConvert INSTANCE = Mappers.getMapper(SceneConvert.class);

    SceneDO convert(SceneSaveReqVO bean);

    SceneRespVO convert(SceneDO bean);

    SceneSimpleRespVO convertSimple(SceneDO bean);

    List<SceneSimpleRespVO> convertSimpleList(List<SceneDO> list);

    PageResult<SceneRespVO> convertPage(PageResult<SceneDO> page);

}
