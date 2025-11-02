package com.ccinfom.model.pack;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TODO[E-INFRA-T3-004] Complete PackBoxLine model with packed_qty and audit fields.
// Why: Represents rows in pack_box_line used by DAO/Service.
// Fields to include: boxLineId, boxId, pickingLineId, packedQty, createdAt, createdBy, updatedAt, updatedBy.
// Acceptance: PackService/DAO compile; ServiceTestRunner uses builder/helper to create instances.
// Owner: Mark
