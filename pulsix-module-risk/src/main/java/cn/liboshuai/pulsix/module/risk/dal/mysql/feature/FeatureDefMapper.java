package cn.liboshuai.pulsix.module.risk.dal.mysql.feature;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDefDO;
import cn.liboshuai.pulsix.module.risk.enums.feature.RiskFeatureTypeEnum;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FeatureDefMapper extends BaseMapperX<FeatureDefDO> {

    default PageResult<FeatureDefDO> selectStreamFeaturePage(FeatureStreamPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FeatureDefDO>()
                .eq(FeatureDefDO::getFeatureType, RiskFeatureTypeEnum.STREAM.getType())
                .eqIfPresent(FeatureDefDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(FeatureDefDO::getFeatureCode, reqVO.getFeatureCode())
                .likeIfPresent(FeatureDefDO::getFeatureName, reqVO.getFeatureName())
                .eqIfPresent(FeatureDefDO::getEntityType, reqVO.getEntityType())
                .eqIfPresent(FeatureDefDO::getStatus, reqVO.getStatus())
                .orderByDesc(FeatureDefDO::getUpdateTime)
                .orderByDesc(FeatureDefDO::getId));
    }

    default PageResult<FeatureDefDO> selectLookupFeaturePage(FeatureLookupPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FeatureDefDO>()
                .eq(FeatureDefDO::getFeatureType, RiskFeatureTypeEnum.LOOKUP.getType())
                .eqIfPresent(FeatureDefDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(FeatureDefDO::getFeatureCode, reqVO.getFeatureCode())
                .likeIfPresent(FeatureDefDO::getFeatureName, reqVO.getFeatureName())
                .eqIfPresent(FeatureDefDO::getStatus, reqVO.getStatus())
                .orderByDesc(FeatureDefDO::getUpdateTime)
                .orderByDesc(FeatureDefDO::getId));
    }

    default PageResult<FeatureDefDO> selectDerivedFeaturePage(FeatureDerivedPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FeatureDefDO>()
                .eq(FeatureDefDO::getFeatureType, RiskFeatureTypeEnum.DERIVED.getType())
                .eqIfPresent(FeatureDefDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(FeatureDefDO::getFeatureCode, reqVO.getFeatureCode())
                .likeIfPresent(FeatureDefDO::getFeatureName, reqVO.getFeatureName())
                .eqIfPresent(FeatureDefDO::getStatus, reqVO.getStatus())
                .orderByDesc(FeatureDefDO::getUpdateTime)
                .orderByDesc(FeatureDefDO::getId));
    }

}
