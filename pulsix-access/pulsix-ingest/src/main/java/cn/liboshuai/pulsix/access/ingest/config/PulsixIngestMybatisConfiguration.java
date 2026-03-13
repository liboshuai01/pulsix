package cn.liboshuai.pulsix.access.ingest.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@MapperScan("cn.liboshuai.pulsix.access.ingest.dal.mysql")
public class PulsixIngestMybatisConfiguration {

}
