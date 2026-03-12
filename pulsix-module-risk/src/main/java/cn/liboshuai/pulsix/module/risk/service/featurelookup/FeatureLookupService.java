package cn.liboshuai.pulsix.module.risk.service.featurelookup;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurelookup.vo.FeatureLookupSaveReqVO;

public interface FeatureLookupService {

    Long createFeatureLookup(FeatureLookupSaveReqVO createReqVO);

    void updateFeatureLookup(FeatureLookupSaveReqVO updateReqVO);

    void deleteFeatureLookup(Long id);

    FeatureLookupRespVO getFeatureLookup(Long id);

    PageResult<FeatureLookupRespVO> getFeatureLookupPage(FeatureLookupPageReqVO pageReqVO);

}
