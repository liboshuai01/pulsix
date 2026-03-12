package cn.liboshuai.pulsix.module.risk.service.featurestream;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.EntityTypeRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.featurestream.vo.FeatureStreamSaveReqVO;

import java.util.List;

public interface FeatureStreamService {

    Long createFeatureStream(FeatureStreamSaveReqVO createReqVO);

    void updateFeatureStream(FeatureStreamSaveReqVO updateReqVO);

    void deleteFeatureStream(Long id);

    FeatureStreamRespVO getFeatureStream(Long id);

    PageResult<FeatureStreamRespVO> getFeatureStreamPage(FeatureStreamPageReqVO pageReqVO);

    List<EntityTypeRespVO> getEntityTypeList();

}
