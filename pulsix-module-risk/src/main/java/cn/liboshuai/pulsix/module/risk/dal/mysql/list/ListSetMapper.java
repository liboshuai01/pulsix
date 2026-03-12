package cn.liboshuai.pulsix.module.risk.dal.mysql.list;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListSetDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ListSetMapper extends BaseMapperX<ListSetDO> {

    default PageResult<ListSetDO> selectPage(ListSetPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ListSetDO>()
                .likeIfPresent(ListSetDO::getSceneCode, reqVO.getSceneCode())
                .likeIfPresent(ListSetDO::getListCode, reqVO.getListCode())
                .likeIfPresent(ListSetDO::getListName, reqVO.getListName())
                .eqIfPresent(ListSetDO::getMatchType, reqVO.getMatchType())
                .eqIfPresent(ListSetDO::getListType, reqVO.getListType())
                .eqIfPresent(ListSetDO::getSyncStatus, reqVO.getSyncStatus())
                .eqIfPresent(ListSetDO::getStatus, reqVO.getStatus())
                .orderByAsc(ListSetDO::getSceneCode)
                .orderByAsc(ListSetDO::getListCode)
                .orderByAsc(ListSetDO::getId));
    }

}
