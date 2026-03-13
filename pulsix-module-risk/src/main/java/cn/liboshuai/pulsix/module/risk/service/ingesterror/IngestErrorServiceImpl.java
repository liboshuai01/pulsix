package cn.liboshuai.pulsix.module.risk.service.ingesterror;

import cn.hutool.core.collection.CollUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorDetailRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingesterror.vo.IngestErrorRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingesterror.IngestErrorLogDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.ingesterror.IngestErrorLogMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.INGEST_ERROR_LOG_NOT_EXISTS;

@Service
public class IngestErrorServiceImpl implements IngestErrorService {

    @Resource
    private IngestErrorLogMapper ingestErrorLogMapper;

    @Override
    public PageResult<IngestErrorRespVO> getIngestErrorPage(IngestErrorPageReqVO pageReqVO) {
        PageResult<IngestErrorLogDO> pageResult = ingestErrorLogMapper.selectPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        List<IngestErrorRespVO> list = pageResult.getList().stream()
                .map(item -> BeanUtils.toBean(item, IngestErrorRespVO.class))
                .toList();
        return new PageResult<>(list, pageResult.getTotal());
    }

    @Override
    public IngestErrorDetailRespVO getIngestError(Long id) {
        IngestErrorLogDO ingestErrorLog = validateIngestErrorExists(id);
        return BeanUtils.toBean(ingestErrorLog, IngestErrorDetailRespVO.class);
    }

    private IngestErrorLogDO validateIngestErrorExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(INGEST_ERROR_LOG_NOT_EXISTS);
        }
        IngestErrorLogDO ingestErrorLog = ingestErrorLogMapper.selectById(id);
        if (ingestErrorLog == null) {
            throw ServiceExceptionUtil.exception(INGEST_ERROR_LOG_NOT_EXISTS);
        }
        return ingestErrorLog;
    }

}
