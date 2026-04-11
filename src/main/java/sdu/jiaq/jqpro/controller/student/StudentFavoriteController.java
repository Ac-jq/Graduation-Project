package sdu.jiaq.jqpro.controller.student;

import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sdu.jiaq.jqpro.common.constant.RoleConstants;
import sdu.jiaq.jqpro.common.result.Result;
import sdu.jiaq.jqpro.dto.resource.ResourceSummaryResponse;
import sdu.jiaq.jqpro.service.ResourceService;

import java.util.List;

/**
 * Student favorite controller.
 */
@RestController
@RequestMapping("/api/student/favorites")
@SaCheckRole(RoleConstants.STUDENT)
public class StudentFavoriteController {

    private final ResourceService resourceService;

    public StudentFavoriteController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public Result<List<ResourceSummaryResponse>> listFavorites() {
        return Result.success(resourceService.listCurrentStudentFavorites());
    }

    @PostMapping("/{resourceId}")
    public Result<Void> addFavorite(@PathVariable Long resourceId) {
        resourceService.addFavorite(resourceId);
        return Result.success("资源收藏成功", null);
    }

    @DeleteMapping("/{resourceId}")
    public Result<Void> removeFavorite(@PathVariable Long resourceId) {
        resourceService.removeFavorite(resourceId);
        return Result.success("已取消收藏", null);
    }
}
