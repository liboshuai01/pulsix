package cn.liboshuai.pulsix.access.ingest.infra.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestKafkaSendResult {

    private String topicName;

    private String messageKey;

}
