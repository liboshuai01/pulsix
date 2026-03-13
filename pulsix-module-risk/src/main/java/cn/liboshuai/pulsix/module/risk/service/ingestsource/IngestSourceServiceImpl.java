package cn.liboshuai.pulsix.module.risk.service.ingestsource;

import cn.hutool.core.util.ObjectUtil;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourcePageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.ingestsource.vo.IngestSourceSaveReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.ingestsource.IngestSourceDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.ingestsource.IngestSourceMapper;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.INGEST_SOURCE_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.INGEST_SOURCE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE_STATUS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_INGEST_SOURCE;

@Service
public class IngestSourceServiceImpl implements IngestSourceService {

    @Resource
    private IngestSourceMapper ingestSourceMapper;

    @Resource
    private AuditLogService auditLogService;

    @Override
    public Long createIngestSource(IngestSourceSaveReqVO createReqVO) {
        validateIngestSourceCodeUnique(null, createReqVO.getSourceCode());
        IngestSourceDO ingestSource = BeanUtils.toBean(createReqVO, IngestSourceDO.class);
        ingestSourceMapper.insert(ingestSource);
        auditLogService.createAuditLog(null, BIZ_TYPE_INGEST_SOURCE, ingestSource.getSourceCode(), ACTION_CREATE,
                null, ingestSourceMapper.selectById(ingestSource.getId()), "新增接入来源 " + ingestSource.getSourceCode());
        return ingestSource.getId();
    }

    @Override
    public void updateIngestSource(IngestSourceSaveReqVO updateReqVO) {
        IngestSourceDO ingestSource = validateIngestSourceExists(updateReqVO.getId());
        IngestSourceDO updateObj = BeanUtils.toBean(updateReqVO, IngestSourceDO.class);
        updateObj.setSourceCode(ingestSource.getSourceCode());
        ingestSourceMapper.updateById(updateObj);
        auditLogService.createAuditLog(null, BIZ_TYPE_INGEST_SOURCE, ingestSource.getSourceCode(), ACTION_UPDATE,
                ingestSource, ingestSourceMapper.selectById(ingestSource.getId()), "修改接入来源 " + ingestSource.getSourceCode());
    }

    @Override
    public void updateIngestSourceStatus(Long id, Integer status) {
        IngestSourceDO ingestSource = validateIngestSourceExists(id);
        if (ObjectUtil.equal(ingestSource.getStatus(), status)) {
            return;
        }
        IngestSourceDO updateObj = new IngestSourceDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        ingestSourceMapper.updateById(updateObj);
        auditLogService.createAuditLog(null, BIZ_TYPE_INGEST_SOURCE, ingestSource.getSourceCode(), ACTION_UPDATE_STATUS,
                ingestSource, ingestSourceMapper.selectById(id), "更新接入来源状态为 " + status + "：" + ingestSource.getSourceCode());
    }

    @Override
    public IngestSourceDO getIngestSource(Long id) {
        return ingestSourceMapper.selectById(id);
    }

    @Override
    public PageResult<IngestSourceDO> getIngestSourcePage(IngestSourcePageReqVO pageReqVO) {
        return ingestSourceMapper.selectPage(pageReqVO);
    }

    private IngestSourceDO validateIngestSourceExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(INGEST_SOURCE_NOT_EXISTS);
        }
        IngestSourceDO ingestSource = ingestSourceMapper.selectById(id);
        if (ingestSource == null) {
            throw ServiceExceptionUtil.exception(INGEST_SOURCE_NOT_EXISTS);
        }
        return ingestSource;
    }

    private void validateIngestSourceCodeUnique(Long id, String sourceCode) {
        IngestSourceDO ingestSource = ingestSourceMapper.selectOne(IngestSourceDO::getSourceCode, sourceCode);
        if (ingestSource == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(ingestSource.getId(), id)) {
            throw ServiceExceptionUtil.exception(INGEST_SOURCE_CODE_DUPLICATE);
        }
    }

}
