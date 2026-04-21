package sdu.jiaq.jqpro.service;

import sdu.jiaq.jqpro.dto.counselor.CounselorProfileResponse;
import sdu.jiaq.jqpro.dto.counselor.UpdateCounselorProfileRequest;

/**
 * 咨询师个人资料服务。
 */
public interface CounselorProfileService {

    CounselorProfileResponse getCurrentCounselorProfile();

    CounselorProfileResponse updateCurrentCounselorProfile(UpdateCounselorProfileRequest request);
}
