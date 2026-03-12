package cn.liboshuai.pulsix.module.risk.dal.mysql.feature;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureLookupConfDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface FeatureLookupConfMapper extends BaseMapperX<FeatureLookupConfDO> {

    default FeatureLookupConfDO selectBySceneAndFeatureCode(String sceneCode, String featureCode) {
        return selectOne(FeatureLookupConfDO::getSceneCode, sceneCode,
                FeatureLookupConfDO::getFeatureCode, featureCode);
    }

    default List<FeatureLookupConfDO> selectListBySceneCodesAndFeatureCodes(Collection<String> sceneCodes,
                                                                            Collection<String> featureCodes) {
        return selectList(new LambdaQueryWrapperX<FeatureLookupConfDO>()
                .inIfPresent(FeatureLookupConfDO::getSceneCode, sceneCodes)
                .inIfPresent(FeatureLookupConfDO::getFeatureCode, featureCodes));
    }

}
