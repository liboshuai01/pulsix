package cn.liboshuai.pulsix.access.ingest.dal.mysql.ingesterror;

import cn.liboshuai.pulsix.access.ingest.dal.dataobject.ingesterror.IngestErrorLogDO;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestErrorLogMapper extends BaseMapperX<IngestErrorLogDO> {

}
