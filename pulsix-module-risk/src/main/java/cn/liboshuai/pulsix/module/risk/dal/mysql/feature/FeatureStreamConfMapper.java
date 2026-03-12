package cn.liboshuai.pulsix.module.risk.dal.mysql.feature;

import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.feature.FeatureStreamConfDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface FeatureStreamConfMapper extends BaseMapperX<FeatureStreamConfDO> {

    default FeatureStreamConfDO selectBySceneAndFeatureCode(String sceneCode, String featureCode) {
        return selectOne(FeatureStreamConfDO::getSceneCode, sceneCode,
                FeatureStreamConfDO::getFeatureCode, featureCode);
    }

    default List<FeatureStreamConfDO> selectListBySceneCodesAndFeatureCodes(Collection<String> sceneCodes,
                                                                            Collection<String> featureCodes) {
        return selectList(new LambdaQueryWrapperX<FeatureStreamConfDO>()
                .inIfPresent(FeatureStreamConfDO::getSceneCode, sceneCodes)
                .inIfPresent(FeatureStreamConfDO::getFeatureCode, featureCodes));
    }

}
