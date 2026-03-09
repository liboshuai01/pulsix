package cn.liboshuai.pulsix.engine.support;

import org.apache.commons.text.StringSubstitutor;

import java.util.Map;

public final class TemplateRenderer {

    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, Object> values) {
        if (template == null || template.isBlank()) {
            return "";
        }
        return new StringSubstitutor(values, "{", "}").replace(template);
    }

}
