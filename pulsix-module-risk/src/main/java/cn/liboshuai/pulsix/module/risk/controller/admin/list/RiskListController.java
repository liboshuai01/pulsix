package cn.liboshuai.pulsix.module.risk.controller.admin.list;

import cn.liboshuai.pulsix.framework.common.pojo.CommonResult;
import cn.liboshuai.pulsix.framework.common.pojo.PageResult;
import cn.liboshuai.pulsix.framework.common.util.object.BeanUtils;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListItemSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetPageReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSetSaveReqVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListSyncRespVO;
import cn.liboshuai.pulsix.module.risk.controller.admin.list.vo.ListUpdateStatusReqVO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListItemDO;
import cn.liboshuai.pulsix.module.risk.dal.dataobject.list.ListSetDO;
import cn.liboshuai.pulsix.module.risk.service.list.RiskListService;
import cn.liboshuai.pulsix.module.risk.util.RiskListRedisUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.liboshuai.pulsix.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 名单中心")
@RestController
@RequestMapping("/risk/list")
@Validated
public class RiskListController {

    @Resource
    private RiskListService riskListService;

    @PostMapping("/set/create")
    @Operation(summary = "创建名单集合")
    @PreAuthorize("@ss.hasPermission('risk:list:create')")
    public CommonResult<Long> createListSet(@Valid @RequestBody ListSetSaveReqVO createReqVO) {
        return success(riskListService.createListSet(createReqVO));
    }

    @PutMapping("/set/update")
    @Operation(summary = "修改名单集合")
    @PreAuthorize("@ss.hasPermission('risk:list:update')")
    public CommonResult<Boolean> updateListSet(@Valid @RequestBody ListSetSaveReqVO updateReqVO) {
        riskListService.updateListSet(updateReqVO);
        return success(true);
    }

    @PutMapping("/set/update-status")
    @Operation(summary = "修改名单集合状态")
    @PreAuthorize("@ss.hasPermission('risk:list:update')")
    public CommonResult<Boolean> updateListSetStatus(@Valid @RequestBody ListUpdateStatusReqVO reqVO) {
        riskListService.updateListSetStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/set/delete")
    @Operation(summary = "删除名单集合")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:list:delete')")
    public CommonResult<Boolean> deleteListSet(@RequestParam("id") Long id) {
        riskListService.deleteListSet(id);
        return success(true);
    }

    @GetMapping("/set/get")
    @Operation(summary = "获得名单集合详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:list:query')")
    public CommonResult<ListSetRespVO> getListSet(@RequestParam("id") Long id) {
        return success(toListSetResp(riskListService.getListSet(id)));
    }

    @GetMapping("/set/page")
    @Operation(summary = "获得名单集合分页")
    @PreAuthorize("@ss.hasPermission('risk:list:query')")
    public CommonResult<PageResult<ListSetRespVO>> getListSetPage(@Valid ListSetPageReqVO pageReqVO) {
        PageResult<ListSetDO> pageResult = riskListService.getListSetPage(pageReqVO);
        PageResult<ListSetRespVO> respPage = BeanUtils.toBean(pageResult, ListSetRespVO.class);
        List<ListSetRespVO> list = pageResult.getList().stream().map(this::toListSetResp).toList();
        respPage.setList(list);
        return success(respPage);
    }

    @PostMapping("/set/sync")
    @Operation(summary = "手动同步名单到 Redis")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:list:sync')")
    public CommonResult<ListSyncRespVO> syncListSet(@RequestParam("id") Long id) {
        return success(riskListService.syncListSet(id));
    }

    @PostMapping("/item/create")
    @Operation(summary = "创建名单条目")
    @PreAuthorize("@ss.hasPermission('risk:list:create')")
    public CommonResult<Long> createListItem(@Valid @RequestBody ListItemSaveReqVO createReqVO) {
        return success(riskListService.createListItem(createReqVO));
    }

    @PutMapping("/item/update")
    @Operation(summary = "修改名单条目")
    @PreAuthorize("@ss.hasPermission('risk:list:update')")
    public CommonResult<Boolean> updateListItem(@Valid @RequestBody ListItemSaveReqVO updateReqVO) {
        riskListService.updateListItem(updateReqVO);
        return success(true);
    }

    @PutMapping("/item/update-status")
    @Operation(summary = "修改名单条目状态")
    @PreAuthorize("@ss.hasPermission('risk:list:update')")
    public CommonResult<Boolean> updateListItemStatus(@Valid @RequestBody ListUpdateStatusReqVO reqVO) {
        riskListService.updateListItemStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @DeleteMapping("/item/delete")
    @Operation(summary = "删除名单条目")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:list:delete')")
    public CommonResult<Boolean> deleteListItem(@RequestParam("id") Long id) {
        riskListService.deleteListItem(id);
        return success(true);
    }

    @GetMapping("/item/get")
    @Operation(summary = "获得名单条目详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PreAuthorize("@ss.hasPermission('risk:list:query')")
    public CommonResult<ListItemRespVO> getListItem(@RequestParam("id") Long id) {
        ListItemDO listItem = riskListService.getListItem(id);
        return success(BeanUtils.toBean(listItem, ListItemRespVO.class));
    }

    @GetMapping("/item/page")
    @Operation(summary = "获得名单条目分页")
    @PreAuthorize("@ss.hasPermission('risk:list:query')")
    public CommonResult<PageResult<ListItemRespVO>> getListItemPage(@Valid ListItemPageReqVO pageReqVO) {
        PageResult<ListItemDO> pageResult = riskListService.getListItemPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ListItemRespVO.class));
    }

    private ListSetRespVO toListSetResp(ListSetDO listSet) {
        if (listSet == null) {
            return null;
        }
        ListSetRespVO respVO = BeanUtils.toBean(listSet, ListSetRespVO.class);
        respVO.setRedisKeyPrefix(RiskListRedisUtils.buildRedisKeyPrefix(listSet.getListCode(), listSet.getListType(), listSet.getMatchType()));
        return respVO;
    }

}
