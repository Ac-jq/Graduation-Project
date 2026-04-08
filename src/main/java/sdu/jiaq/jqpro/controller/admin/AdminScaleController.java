package sdu.jiaq.jqpro.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.adminscale.AdminScaleResponse;
import sdu.jiaq.jqpro.dto.adminscale.UpsertAdminScaleRequest;
import sdu.jiaq.jqpro.service.AdminScaleService;

import java.util.List;

/**
 * Admin scale management controller.
 */
@RestController
@RequestMapping("/api/admin/scales")
@SaCheckRole(RoleConstants.ADMIN)
public class AdminScaleController {

    private final AdminScaleService adminScaleService;

    public AdminScaleController(AdminScaleService adminScaleService) {
        this.adminScaleService = adminScaleService;
    }

    @GetMapping
    public Result<List<AdminScaleResponse>> listScales() {
        return Result.success(adminScaleService.listScales());
    }

    @GetMapping("/{scaleId}")
    public Result<AdminScaleResponse> getScale(@PathVariable Long scaleId) {
        return Result.success(adminScaleService.getScale(scaleId));
    }

    @PostMapping
    public Result<AdminScaleResponse> createScale(@Valid @RequestBody UpsertAdminScaleRequest request) {
        return Result.success("Scale created", adminScaleService.createScale(request));
    }

    @PutMapping("/{scaleId}")
    public Result<AdminScaleResponse> updateScale(@PathVariable Long scaleId,
                                                  @Valid @RequestBody UpsertAdminScaleRequest request) {
        return Result.success("Scale updated", adminScaleService.updateScale(scaleId, request));
    }

    @PostMapping("/{scaleId}/activate")
    public Result<AdminScaleResponse> activateScale(@PathVariable Long scaleId) {
        return Result.success("Scale activated", adminScaleService.activateScale(scaleId));
    }

    @PostMapping("/{scaleId}/deactivate")
    public Result<AdminScaleResponse> deactivateScale(@PathVariable Long scaleId) {
        return Result.success("Scale deactivated", adminScaleService.deactivateScale(scaleId));
    }
}
