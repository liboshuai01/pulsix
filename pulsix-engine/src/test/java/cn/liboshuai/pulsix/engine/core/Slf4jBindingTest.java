package cn.liboshuai.pulsix.engine.core;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Slf4jBindingTest {

    @Test
    void shouldUseLog4jBindingInsteadOfConnectorShadowClass() {
        String loggerFactorySource = String.valueOf(LoggerFactory.class.getProtectionDomain().getCodeSource());

        assertFalse(loggerFactorySource.contains("flink-doris-connector"), loggerFactorySource);
        assertFalse(LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory);
        assertEquals("org.apache.logging.slf4j.Log4jLoggerFactory", LoggerFactory.getILoggerFactory().getClass().getName());
    }

}
