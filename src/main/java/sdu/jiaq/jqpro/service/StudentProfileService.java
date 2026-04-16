package sdu.jiaq.jqpro.service;

import org.springframework.web.multipart.MultipartFile;
import sdu.jiaq.jqpro.dto.student.AvatarUploadResponse;
import sdu.jiaq.jqpro.dto.student.StudentProfileResponse;
import sdu.jiaq.jqpro.dto.student.UpdateStudentProfileRequest;

/**
 * 学生档案服务。
 */
public interface StudentProfileService {

    StudentProfileResponse getCurrentStudentProfile();

    StudentProfileResponse updateCurrentStudentProfile(UpdateStudentProfileRequest request);

    AvatarUploadResponse uploadCurrentStudentAvatar(MultipartFile file);
}
