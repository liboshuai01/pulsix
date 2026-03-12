package cn.liboshuai.pulsix.module.risk.dal.mysql.list;

import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.mybatis.core.mapper.BaseMapperX;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemPageReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ListItemMapper extends BaseMapperX<ListItemDO> {

    default PageResult<ListItemDO> selectPage(ListItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ListItemDO>()
                .eqIfPresent(ListItemDO::getSceneCode, reqVO.getSceneCode())
                .eqIfPresent(ListItemDO::getListCode, reqVO.getListCode())
                .likeIfPresent(ListItemDO::getMatchValue, reqVO.getMatchValue())
                .eqIfPresent(ListItemDO::getStatus, reqVO.getStatus())
                .orderByAsc(ListItemDO::getExpireAt)
                .orderByDesc(ListItemDO::getId));
    }

    default List<ListItemDO> selectSyncItems(String sceneCode, String listCode, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<ListItemDO>()
                .eq(ListItemDO::getSceneCode, sceneCode)
                .eq(ListItemDO::getListCode, listCode)
                .eq(ListItemDO::getStatus, cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum.ENABLE.getStatus())
                .and(wrapper -> wrapper.isNull(ListItemDO::getExpireAt).or().gt(ListItemDO::getExpireAt, now))
                .orderByAsc(ListItemDO::getExpireAt)
                .orderByAsc(ListItemDO::getId));
    }

    default List<ListItemDO> selectAllByList(String sceneCode, String listCode) {
        return selectList(new LambdaQueryWrapperX<ListItemDO>()
                .eq(ListItemDO::getSceneCode, sceneCode)
                .eq(ListItemDO::getListCode, listCode)
                .orderByAsc(ListItemDO::getId));
    }

}
