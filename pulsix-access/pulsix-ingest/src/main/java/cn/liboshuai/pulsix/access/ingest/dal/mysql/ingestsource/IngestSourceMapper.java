package cn.liboshuai.pulsix.access.ingest.dal.mysql.ingestsource;

import cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingestsource.IngestSourceDO;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestSourceMapper extends BaseMapperX<IngestSourceDO> {

    default IngestSourceDO selectBySourceCode(String sourceCode) {
        return selectFirstOne(IngestSourceDO::getSourceCode, sourceCode);
    }

}
