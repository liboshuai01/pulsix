package cn.liboshuai.pulsix.module.risk.dal.mysql.feature;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureDerivedConfDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface FeatureDerivedConfMapper extends BaseMapperX<FeatureDerivedConfDO> {

    default FeatureDerivedConfDO selectBySceneAndFeatureCode(String sceneCode, String featureCode) {
        return selectOne(FeatureDerivedConfDO::getSceneCode, sceneCode,
                FeatureDerivedConfDO::getFeatureCode, featureCode);
    }

    default List<FeatureDerivedConfDO> selectListBySceneCodesAndFeatureCodes(Collection<String> sceneCodes,
                                                                             Collection<String> featureCodes) {
        return selectList(new LambdaQueryWrapperX<FeatureDerivedConfDO>()
                .inIfPresent(FeatureDerivedConfDO::getSceneCode, sceneCodes)
                .inIfPresent(FeatureDerivedConfDO::getFeatureCode, featureCodes));
    }

}
