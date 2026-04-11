package sdu.jiaq.jqpro.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import sdu.jiaq.jqpro.entity.SysAuditLog;

/**
 * 审计日志 Mapper。
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
