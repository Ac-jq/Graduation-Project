package sdu.jiaq.jqpro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import sdu.jiaq.jqpro.dto.adminuser.AdminUserSummaryResponse;
import sdu.jiaq.jqpro.entity.SysUser;

/**
 * 用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * Admin-side joined query over sys_user and student_profile.
     */
    @Select("""
            <script>
            SELECT
              u.id AS userId,
              u.account AS account,
              u.role_code AS roleCode,
              u.real_name AS realName,
              u.display_name AS displayName,
              u.student_no AS studentNo,
              u.counselor_no AS counselorNo,
              u.status AS status,
              sp.college AS college,
              sp.grade AS grade,
              sp.phone AS phone,
              u.created_at AS createdAt
            FROM sys_user u
            LEFT JOIN student_profile sp ON u.id = sp.user_id
            <where>
              <if test="roleCode != null and roleCode != ''">
                AND u.role_code = #{roleCode}
              </if>
              <if test="status != null and status != ''">
                AND u.status = #{status}
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                  LOWER(u.account) LIKE CONCAT('%', LOWER(#{keyword}), '%')
                  OR LOWER(u.real_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
                  OR LOWER(u.display_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
                  OR LOWER(u.student_no) LIKE CONCAT('%', LOWER(#{keyword}), '%')
                )
              </if>
              <if test="grade != null and grade != ''">
                AND LOWER(sp.grade) LIKE CONCAT('%', LOWER(#{grade}), '%')
              </if>
              <if test="college != null and college != ''">
                AND LOWER(sp.college) LIKE CONCAT('%', LOWER(#{college}), '%')
              </if>
            </where>
            ORDER BY u.created_at DESC, u.id DESC
            </script>
            """)
    List<AdminUserSummaryResponse> selectAdminUserSummaries(@Param("roleCode") String roleCode,
                                                            @Param("status") String status,
                                                            @Param("keyword") String keyword,
                                                            @Param("grade") String grade,
                                                            @Param("college") String college);
}
