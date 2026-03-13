package cn.liboshuai.pulsix.module.risk.service.list;

import cn.hutool.core.util.ObjectUtil;
import cn.liboshuai.pulsix.framework.common.enums.CommonStatusEnum;
import cn.liboshuai.pulsix.framework.common.exception.util.ServiceExceptionUtil;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSyncRespVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListItemDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListSetDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.scene.SceneDO;
import cn.liboshuai.pulsix.module.risk.dal.mysql.list.ListItemMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.list.ListSetMapper;
import cn.liboshuai.pulsix.module.risk.dal.mysql.scene.SceneMapper;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListStorageTypeEnum;
import cn.liboshuai.pulsix.module.risk.enums.list.RiskListSyncStatusEnum;
import cn.liboshuai.pulsix.module.risk.service.auditlog.AuditLogService;
import cn.liboshuai.pulsix.module.risk.util.RiskListRedisUtils;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.LIST_ITEM_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.LIST_ITEM_VALUE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.LIST_SET_CODE_DUPLICATE;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.LIST_SET_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.LIST_SYNC_FAILED;
import static cn.liboshuai.pulsix.module.risk.enums.ErrorCodeConstants.SCENE_NOT_EXISTS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_CREATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_DELETE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_SYNC;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.ACTION_UPDATE_STATUS;
import static cn.liboshuai.pulsix.module.risk.enums.RiskAuditConstants.BIZ_TYPE_LIST;

@Service
public class RiskListServiceImpl implements RiskListService {

    @Resource
    private ListSetMapper listSetMapper;

    @Resource
    private ListItemMapper listItemMapper;

    @Resource
    private SceneMapper sceneMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AuditLogService auditLogService;

    @Override
    public Long createListSet(ListSetSaveReqVO createReqVO) {
        validateSceneExists(createReqVO.getSceneCode());
        validateListSetCodeUnique(createReqVO.getSceneCode(), createReqVO.getListCode(), null);
        ListSetDO listSet = BeanUtils.toBean(createReqVO, ListSetDO.class);
        listSet.setSyncStatus(RiskListSyncStatusEnum.PENDING.getType());
        listSet.setLastSyncTime(null);
        listSetMapper.insert(listSet);
        auditLogService.createAuditLog(listSet.getSceneCode(), BIZ_TYPE_LIST, buildListSetBizCode(listSet), ACTION_CREATE,
                null, listSetMapper.selectById(listSet.getId()), "新增名单集 " + listSet.getListCode());
        return listSet.getId();
    }

    @Override
    public void updateListSet(ListSetSaveReqVO updateReqVO) {
        ListSetDO listSet = validateListSetExists(updateReqVO.getId());
        ListSetDO updateObj = BeanUtils.toBean(updateReqVO, ListSetDO.class);
        updateObj.setSceneCode(listSet.getSceneCode());
        updateObj.setListCode(listSet.getListCode());
        updateObj.setSyncStatus(RiskListSyncStatusEnum.PENDING.getType());
        listSetMapper.updateById(updateObj);
        auditLogService.createAuditLog(listSet.getSceneCode(), BIZ_TYPE_LIST, buildListSetBizCode(listSet), ACTION_UPDATE,
                listSet, listSetMapper.selectById(listSet.getId()), "修改名单集 " + listSet.getListCode());
    }

    @Override
    public void updateListSetStatus(Long id, Integer status) {
        ListSetDO listSet = validateListSetExists(id);
        if (ObjectUtil.equal(listSet.getStatus(), status)) {
            return;
        }
        ListSetDO updateObj = new ListSetDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        updateObj.setSyncStatus(RiskListSyncStatusEnum.PENDING.getType());
        listSetMapper.updateById(updateObj);
        auditLogService.createAuditLog(listSet.getSceneCode(), BIZ_TYPE_LIST, buildListSetBizCode(listSet), ACTION_UPDATE_STATUS,
                listSet, listSetMapper.selectById(id), "更新名单集状态为 " + status + "：" + listSet.getListCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteListSet(Long id) {
        ListSetDO listSet = validateListSetExists(id);
        listItemMapper.delete(new LambdaQueryWrapperX<ListItemDO>()
                .eq(ListItemDO::getSceneCode, listSet.getSceneCode())
                .eq(ListItemDO::getListCode, listSet.getListCode()));
        listSetMapper.deleteById(id);
        auditLogService.createAuditLog(listSet.getSceneCode(), BIZ_TYPE_LIST, buildListSetBizCode(listSet), ACTION_DELETE,
                listSet, null, "删除名单集 " + listSet.getListCode());
        try {
            purgeRedisData(listSet);
        } catch (Exception ignored) {
        }
    }

    @Override
    public ListSetDO getListSet(Long id) {
        return listSetMapper.selectById(id);
    }

    @Override
    public PageResult<ListSetDO> getListSetPage(ListSetPageReqVO pageReqVO) {
        return listSetMapper.selectPage(pageReqVO);
    }

    @Override
    public ListSyncRespVO syncListSet(Long id) {
        ListSetDO listSet = validateListSetExists(id);
        String redisKeyPrefix = buildRedisKeyPrefix(listSet);
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> beforePayload = buildListSyncAuditPayload(listSet, null, null);
        try {
            purgeRedisData(listSet);
            int syncedCount = 0;
            if (CommonStatusEnum.isEnable(listSet.getStatus())) {
                List<ListItemDO> syncItems = listItemMapper.selectSyncItems(listSet.getSceneCode(), listSet.getListCode(), now);
                if (RiskListStorageTypeEnum.REDIS_HASH.getType().equals(listSet.getStorageType())) {
                    syncedCount = syncHashItems(redisKeyPrefix, syncItems);
                } else {
                    syncedCount = syncKeyItems(listSet, redisKeyPrefix, syncItems, now);
                }
            }
            ListSetDO updateObj = new ListSetDO();
            updateObj.setId(listSet.getId());
            updateObj.setSyncStatus(RiskListSyncStatusEnum.SUCCESS.getType());
            updateObj.setLastSyncTime(now);
            listSetMapper.updateById(updateObj);

            ListSyncRespVO respVO = new ListSyncRespVO();
            respVO.setListCode(listSet.getListCode());
            respVO.setRedisKeyPrefix(redisKeyPrefix);
            respVO.setSyncedItemCount(syncedCount);
            respVO.setStorageType(listSet.getStorageType());
            auditLogService.createAuditLog(listSet.getSceneCode(), BIZ_TYPE_LIST, buildListSetBizCode(listSet), ACTION_SYNC,
                    beforePayload, buildListSyncAuditPayload(listSetMapper.selectById(listSet.getId()), respVO, null),
                    "同步名单到 Redis：" + listSet.getListCode());
            return respVO;
        } catch (Exception exception) {
            ListSetDO updateObj = new ListSetDO();
            updateObj.setId(listSet.getId());
            updateObj.setSyncStatus(RiskListSyncStatusEnum.FAILED.getType());
            listSetMapper.updateById(updateObj);
            auditLogService.createAuditLog(listSet.getSceneCode(), BIZ_TYPE_LIST, buildListSetBizCode(listSet), ACTION_SYNC,
                    beforePayload, buildListSyncAuditPayload(listSetMapper.selectById(listSet.getId()), null, exception.getMessage()),
                    "同步名单到 Redis 失败：" + listSet.getListCode());
            throw ServiceExceptionUtil.exception(LIST_SYNC_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createListItem(ListItemSaveReqVO createReqVO) {
        validateListSetExists(createReqVO.getSceneCode(), createReqVO.getListCode());
        validateListItemValueUnique(createReqVO.getSceneCode(), createReqVO.getListCode(), createReqVO.getMatchValue(), null);
        ListItemDO listItem = BeanUtils.toBean(createReqVO, ListItemDO.class);
        listItemMapper.insert(listItem);
        markSetSyncPending(createReqVO.getSceneCode(), createReqVO.getListCode());
        auditLogService.createAuditLog(listItem.getSceneCode(), BIZ_TYPE_LIST, buildListItemBizCode(listItem), ACTION_CREATE,
                null, listItemMapper.selectById(listItem.getId()), "新增名单项 " + listItem.getMatchValue());
        return listItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateListItem(ListItemSaveReqVO updateReqVO) {
        ListItemDO listItem = validateListItemExists(updateReqVO.getId());
        validateListItemValueUnique(listItem.getSceneCode(), listItem.getListCode(), updateReqVO.getMatchValue(), updateReqVO.getId());
        ListItemDO updateObj = BeanUtils.toBean(updateReqVO, ListItemDO.class);
        updateObj.setSceneCode(listItem.getSceneCode());
        updateObj.setListCode(listItem.getListCode());
        listItemMapper.updateById(updateObj);
        markSetSyncPending(listItem.getSceneCode(), listItem.getListCode());
        auditLogService.createAuditLog(listItem.getSceneCode(), BIZ_TYPE_LIST, buildListItemBizCode(listItem), ACTION_UPDATE,
                listItem, listItemMapper.selectById(listItem.getId()), "修改名单项 " + listItem.getMatchValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateListItemStatus(Long id, Integer status) {
        ListItemDO listItem = validateListItemExists(id);
        if (ObjectUtil.equal(listItem.getStatus(), status)) {
            return;
        }
        ListItemDO updateObj = new ListItemDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        listItemMapper.updateById(updateObj);
        markSetSyncPending(listItem.getSceneCode(), listItem.getListCode());
        auditLogService.createAuditLog(listItem.getSceneCode(), BIZ_TYPE_LIST, buildListItemBizCode(listItem), ACTION_UPDATE_STATUS,
                listItem, listItemMapper.selectById(id), "更新名单项状态为 " + status + "：" + listItem.getMatchValue());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteListItem(Long id) {
        ListItemDO listItem = validateListItemExists(id);
        listItemMapper.deleteById(id);
        markSetSyncPending(listItem.getSceneCode(), listItem.getListCode());
        auditLogService.createAuditLog(listItem.getSceneCode(), BIZ_TYPE_LIST, buildListItemBizCode(listItem), ACTION_DELETE,
                listItem, null, "删除名单项 " + listItem.getMatchValue());
    }

    @Override
    public ListItemDO getListItem(Long id) {
        return listItemMapper.selectById(id);
    }

    @Override
    public PageResult<ListItemDO> getListItemPage(ListItemPageReqVO pageReqVO) {
        return listItemMapper.selectPage(pageReqVO);
    }

    private SceneDO validateSceneExists(String sceneCode) {
        SceneDO scene = sceneMapper.selectOne(SceneDO::getSceneCode, sceneCode);
        if (scene == null) {
            throw ServiceExceptionUtil.exception(SCENE_NOT_EXISTS);
        }
        return scene;
    }

    private ListSetDO validateListSetExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(LIST_SET_NOT_EXISTS);
        }
        ListSetDO listSet = listSetMapper.selectById(id);
        if (listSet == null) {
            throw ServiceExceptionUtil.exception(LIST_SET_NOT_EXISTS);
        }
        return listSet;
    }

    private ListSetDO validateListSetExists(String sceneCode, String listCode) {
        ListSetDO listSet = listSetMapper.selectOne(ListSetDO::getSceneCode, sceneCode, ListSetDO::getListCode, listCode);
        if (listSet == null) {
            throw ServiceExceptionUtil.exception(LIST_SET_NOT_EXISTS);
        }
        return listSet;
    }

    private void validateListSetCodeUnique(String sceneCode, String listCode, Long id) {
        ListSetDO listSet = listSetMapper.selectOne(ListSetDO::getSceneCode, sceneCode, ListSetDO::getListCode, listCode);
        if (listSet == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(listSet.getId(), id)) {
            throw ServiceExceptionUtil.exception(LIST_SET_CODE_DUPLICATE);
        }
    }

    private ListItemDO validateListItemExists(Long id) {
        if (id == null) {
            throw ServiceExceptionUtil.exception(LIST_ITEM_NOT_EXISTS);
        }
        ListItemDO listItem = listItemMapper.selectById(id);
        if (listItem == null) {
            throw ServiceExceptionUtil.exception(LIST_ITEM_NOT_EXISTS);
        }
        return listItem;
    }

    private void validateListItemValueUnique(String sceneCode, String listCode, String matchValue, Long id) {
        ListItemDO listItem = listItemMapper.selectOne(ListItemDO::getSceneCode, sceneCode,
                ListItemDO::getListCode, listCode, ListItemDO::getMatchValue, matchValue);
        if (listItem == null) {
            return;
        }
        if (id == null || !ObjectUtil.equal(listItem.getId(), id)) {
            throw ServiceExceptionUtil.exception(LIST_ITEM_VALUE_DUPLICATE);
        }
    }

    private void markSetSyncPending(String sceneCode, String listCode) {
        ListSetDO listSet = validateListSetExists(sceneCode, listCode);
        if (RiskListSyncStatusEnum.PENDING.getType().equals(listSet.getSyncStatus())) {
            return;
        }
        ListSetDO updateObj = new ListSetDO();
        updateObj.setId(listSet.getId());
        updateObj.setSyncStatus(RiskListSyncStatusEnum.PENDING.getType());
        listSetMapper.updateById(updateObj);
    }

    private String buildRedisKeyPrefix(ListSetDO listSet) {
        return RiskListRedisUtils.buildRedisKeyPrefix(listSet.getListCode(), listSet.getListType(), listSet.getMatchType());
    }

    private void purgeRedisData(ListSetDO listSet) {
        String redisKeyPrefix = buildRedisKeyPrefix(listSet);
        Set<String> keys = stringRedisTemplate.keys(redisKeyPrefix + ":*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
        stringRedisTemplate.delete(redisKeyPrefix);
    }

    private int syncHashItems(String redisKeyPrefix, List<ListItemDO> syncItems) {
        if (syncItems.isEmpty()) {
            return 0;
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (ListItemDO syncItem : syncItems) {
            values.put(syncItem.getMatchValue(), RiskListRedisUtils.buildHashValue(syncItem.getRemark(), syncItem.getExtJson()));
        }
        stringRedisTemplate.opsForHash().putAll(redisKeyPrefix, values);
        return values.size();
    }

    private int syncKeyItems(ListSetDO listSet, String redisKeyPrefix, List<ListItemDO> syncItems, LocalDateTime now) {
        int syncedCount = 0;
        for (ListItemDO syncItem : syncItems) {
            String redisKey = RiskListRedisUtils.buildRedisItemKey(listSet.getListCode(), listSet.getListType(), listSet.getMatchType(),
                    syncItem.getMatchValue());
            String redisValue = RiskListRedisUtils.buildHashValue(syncItem.getRemark(), syncItem.getExtJson());
            if (syncItem.getExpireAt() != null) {
                Duration duration = Duration.between(now, syncItem.getExpireAt());
                if (!duration.isNegative() && !duration.isZero()) {
                    stringRedisTemplate.opsForValue().set(redisKey, redisValue, duration);
                    syncedCount++;
                }
                continue;
            }
            stringRedisTemplate.opsForValue().set(redisKey, redisValue);
            syncedCount++;
        }
        return syncedCount;
    }

    private String buildListSetBizCode(ListSetDO listSet) {
        return listSet.getListCode();
    }

    private String buildListItemBizCode(ListItemDO listItem) {
        return listItem.getListCode() + ':' + listItem.getMatchValue();
    }

    private Map<String, Object> buildListSyncAuditPayload(ListSetDO listSet, ListSyncRespVO syncRespVO, String syncError) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("listSet", listSet);
        if (syncRespVO != null) {
            payload.put("syncResult", syncRespVO);
        }
        if (syncError != null) {
            payload.put("syncError", syncError);
        }
        return payload;
    }

}
