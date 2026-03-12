package cn.liboshuai.pulsix.module.risk.dal.mysql.rule;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.rule.vo.RulePageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.rule.RuleDefDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RuleDefMapper extends BaseMapperX<RuleDefDO> {

    default PageResult<RuleDefDO> selectRulePage(RulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<RuleDefDO>()
                .eqIfPresent(RuleDefDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(RuleDefDO::getRuleCode, reqVO.getRuleCode())
                .likeIfPresent(RuleDefDO::getRuleName, reqVO.getRuleName())
                .eqIfPresent(RuleDefDO::getHitAction, reqVO.getHitAction())
                .eqIfPresent(RuleDefDO::getStatus, reqVO.getStatus())
                .orderByDesc(RuleDefDO::getPriority)
                .orderByDesc(RuleDefDO::getUpdateTime)
                .orderByDesc(RuleDefDO::getId));
    }

    default RuleDefDO selectBySceneAndRuleCode(String sceneCode, String ruleCode) {
        return selectOne(RuleDefDO::getSceneCode, sceneCode,
                RuleDefDO::getRuleCode, ruleCode);
    }

}
