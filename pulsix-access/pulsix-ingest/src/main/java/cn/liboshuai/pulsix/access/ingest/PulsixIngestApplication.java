package cn.liboshuai.pulsix.access.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pulsix Ingest 启动类
 */
@SuppressWarnings("SpringComponentScan")
@SpringBootApplication(scanBasePackages = "cn.liboshuai.pulsix.access.ingest")
public class PulsixIngestApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulsixIngestApplication.class, args);
    }

}
