package cn.liboshuai.pulsix.module.risk.support;

import cn.liboshuai.pulsix.framework.common.enums.UserTypeEnum;
import cn.liboshuai.pulsix.framework.mybatis.core.handler.DefaultDBFieldHandler;
import cn.liboshuai.pulsix.framework.security.core.LoginUser;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.eventmodel.EventSchemaDO;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultDBFieldHandlerTest {

    private final DefaultDBFieldHandler handler = new DefaultDBFieldHandler();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void insertFill_fillsAuditFieldsForRiskBaseDo() {
        mockLoginUser(9527L);
        EventSchemaDO schema = new EventSchemaDO();

        handler.insertFill(SystemMetaObject.forObject(schema));

        assertThat(schema.getCreateTime()).isNotNull();
        assertThat(schema.getUpdateTime()).isNotNull();
        assertThat(schema.getCreator()).isEqualTo("9527");
        assertThat(schema.getUpdater()).isEqualTo("9527");
    }

    @Test
    void updateFill_fillsUpdaterAndUpdateTimeForRiskBaseDo() {
        mockLoginUser(9528L);
        EventSchemaDO schema = new EventSchemaDO();

        handler.updateFill(SystemMetaObject.forObject(schema));

        assertThat(schema.getUpdateTime()).isNotNull();
        assertThat(schema.getUpdater()).isEqualTo("9528");
    }

    @Test
    void riskDo_doesNotExposeDeletedProperty() {
        assertThat(BeanUtils.getPropertyDescriptor(EventSchemaDO.class, "deleted")).isNull();
    }

    private void mockLoginUser(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        loginUser.setUserType(UserTypeEnum.ADMIN.getValue());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

}
