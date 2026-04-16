package sdu.jiaq.jqpro.dto.statistics;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户个体心理健康干预效果评估导出行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInterventionEffectExportRow {

    @ExcelProperty("用户姓名")
    private String userName;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("所属班级/组织")
    private String organizationName;

    @ExcelProperty("累计登录天数")
    private Long loginDayCount;

    @ExcelProperty("资源浏览总次数")
    private Long resourceViewCount;

    @ExcelProperty("AI会话总次数")
    private Long aiSessionCount;

    @ExcelProperty("首次测评时间")
    private String firstAssessmentTime;

    @ExcelProperty("首次测评量表名称")
    private String firstScaleName;

    @ExcelProperty("前测得分")
    private Integer firstScore;

    @ExcelProperty("初始心理风险等级")
    private String firstRiskLevel;

    @ExcelProperty("最新测评时间")
    private String latestAssessmentTime;

    @ExcelProperty("最新测评量表名称")
    private String latestScaleName;

    @ExcelProperty("最新测评得分")
    private Integer latestScore;

    @ExcelProperty("当前心理风险等级")
    private String latestRiskLevel;

    @ExcelProperty("得分差值")
    private String scoreDeltaText;

    @ExcelProperty("状态转化")
    private String statusTransition;

    @ExcelProperty("评估结果")
    private String evaluationLabel;
}
