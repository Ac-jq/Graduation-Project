package sdu.jiaq.jqpro.dto.adminai;

import java.util.List;
import lombok.Data;

/**
 * Request payload for selectively confirming administrator AI task items.
 */
@Data
public class ConfirmAdminAiTaskRequest {

    /**
     * Empty or null means execute all generated task items.
     */
    private List<Long> selectedItemIds;
}
