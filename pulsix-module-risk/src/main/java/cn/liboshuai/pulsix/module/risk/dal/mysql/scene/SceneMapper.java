package cn.liboshuai.pulsix.module.risk.dal.mysql.scene;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.scene.vo.ScenePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SceneMapper extends BaseMapperX<SceneDO> {

    default SceneDO selectBySceneCode(String sceneCode) {
        return selectOne(SceneDO::getSceneCode, sceneCode);
    }

    default PageResult<SceneDO> selectPage(ScenePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SceneDO>()
                .likeIfPresent(SceneDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(SceneDO::getSceneName, reqVO.getSceneName())
                .eqIfPresent(SceneDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SceneDO::getRuntimeMode, reqVO.getRuntimeMode())
                .orderByDesc(SceneDO::getId));
    }

    default List<SceneDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<SceneDO>()
                .eq(SceneDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(SceneDO::getSceneCode));
    }

    @Select("SELECT COUNT(1) FROM event_schema WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectEventSchemaCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("SELECT COUNT(1) FROM list_set WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectListSetCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("SELECT COUNT(1) FROM feature_def WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectFeatureCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("SELECT COUNT(1) FROM rule_def WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectRuleCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("SELECT COUNT(1) FROM policy_def WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectPolicyCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("SELECT COUNT(1) FROM scene_release WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectSceneReleaseCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("SELECT COUNT(1) FROM simulation_case WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectSimulationCaseCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("SELECT COUNT(1) FROM alert_rule_def WHERE scene_code = #{sceneCode} AND deleted = b'0'")
    long selectAlertRuleCountBySceneCode(@Param("sceneCode") String sceneCode);

    @Select("""
            SELECT COUNT(1)
            FROM access_source_def
            WHERE deleted = b'0'
              AND JSON_CONTAINS(COALESCE(allowed_scene_codes_json, JSON_ARRAY()), JSON_QUOTE(#{sceneCode}))
            """)
    long selectAccessSourceCountBySceneCode(@Param("sceneCode") String sceneCode);

}
