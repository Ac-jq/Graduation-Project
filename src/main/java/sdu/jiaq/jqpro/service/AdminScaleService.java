package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.adminscale.AdminScaleResponse;
import sdu.jiaq.jqpro.dto.adminscale.UpsertAdminScaleRequest;

import java.util.List;

/**
 * Admin scale service.
 */
public interface AdminScaleService {

    List<AdminScaleResponse> listScales();

    AdminScaleResponse getScale(Long scaleId);

    AdminScaleResponse createScale(UpsertAdminScaleRequest request);

    AdminScaleResponse updateScale(Long scaleId, UpsertAdminScaleRequest request);

    AdminScaleResponse activateScale(Long scaleId);

    AdminScaleResponse deactivateScale(Long scaleId);
}
