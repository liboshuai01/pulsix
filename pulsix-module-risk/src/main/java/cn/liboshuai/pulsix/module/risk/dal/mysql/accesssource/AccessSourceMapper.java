package cn.liboshuai.pulsix.module.risk.dal.mysql.accesssource;

import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.accesssource.vo.AccessSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.accesssource.AccessSourceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface AccessSourceMapper extends BaseMapperX<AccessSourceDO> {

    default AccessSourceDO selectBySourceCode(String sourceCode) {
        return selectOne(AccessSourceDO::getSourceCode, sourceCode);
    }

    default List<AccessSourceDO> selectListBySourceCodes(Collection<String> sourceCodes) {
        return selectList(new LambdaQueryWrapperX<AccessSourceDO>()
                .inIfPresent(AccessSourceDO::getSourceCode, sourceCodes)
                .orderByAsc(AccessSourceDO::getSourceCode));
    }

    default PageResult<AccessSourceDO> selectPage(AccessSourcePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AccessSourceDO>()
                .likeIfPresent(AccessSourceDO::getSourceCode, reqVO.getSourceCode())
                .likeIfPresent(AccessSourceDO::getSourceName, reqVO.getSourceName())
                .eqIfPresent(AccessSourceDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(AccessSourceDO::getTopicName, reqVO.getTopicName())
                .eqIfPresent(AccessSourceDO::getStatus, reqVO.getStatus())
                .orderByDesc(AccessSourceDO::getId));
    }

    @Select("""
            <script>
            SELECT * FROM access_source_def
            WHERE deleted = b'0'
              AND status = #{status}
            <if test='sceneCode != null and sceneCode != ""'>
              AND JSON_CONTAINS(COALESCE(allowed_scene_codes_json, JSON_ARRAY()), JSON_QUOTE(#{sceneCode}))
            </if>
            ORDER BY source_code ASC
            </script>
            """)
    List<AccessSourceDO> selectEnabledListBySceneCode(@Param("sceneCode") String sceneCode,
                                                      @Param("status") Integer status);

    default List<AccessSourceDO> selectEnabledListBySceneCode(String sceneCode) {
        return selectEnabledListBySceneCode(sceneCode, CommonStatusEnum.ENABLE.getStatus());
    }

    @Select("""
            SELECT COUNT(1)
            FROM access_source_def
            WHERE deleted = b'0'
              AND JSON_CONTAINS(COALESCE(allowed_scene_codes_json, JSON_ARRAY()), JSON_QUOTE(#{sceneCode}))
            """)
    long selectSceneReferenceCount(@Param("sceneCode") String sceneCode);

}
