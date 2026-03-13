package cn.liboshuai.pulsix.module.risk.service.preview;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StandardEventPreviewServiceTest {

    @Test
    void shouldBindPreviewZoneIdFromIngestProperty() throws Exception {
        Field field = StandardEventPreviewService.class.getDeclaredField("ingestZoneId");

        Value annotation = field.getAnnotation(Value.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("${pulsix.access.ingest.zone-id:Asia/Shanghai}");
    }

    @Test
    void shouldNotFallbackToSystemDefaultZoneInSource() throws Exception {
        Path sourcePath = Path.of("src/main/java/cn/liboshuai/pulsix/module/risk/service/preview/StandardEventPreviewService.java");
        String source = Files.readString(sourcePath);

        assertThat(source).contains("ZoneId.of(ingestZoneId)");
        assertThat(source).doesNotContain("ZoneId.systemDefault()");
    }

}
