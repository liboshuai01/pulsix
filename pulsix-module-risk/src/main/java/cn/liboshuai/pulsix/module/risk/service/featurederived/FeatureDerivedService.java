package cn.liboshuai.pulsix.module.risk.service.featurederived;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedDependencyOptionRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurederived.vo.FeatureDerivedValidateRespVO;

import java.util.List;

public interface FeatureDerivedService {

    Long createFeatureDerived(FeatureDerivedSaveReqVO createReqVO);

    void updateFeatureDerived(FeatureDerivedSaveReqVO updateReqVO);

    void deleteFeatureDerived(Long id);

    FeatureDerivedRespVO getFeatureDerived(Long id);

    PageResult<FeatureDerivedRespVO> getFeatureDerivedPage(FeatureDerivedPageReqVO pageReqVO);

    List<FeatureDerivedDependencyOptionRespVO> getDependencyOptions(String sceneCode, String currentFeatureCode);

    FeatureDerivedValidateRespVO validateExpression(FeatureDerivedValidateReqVO reqVO);

}
