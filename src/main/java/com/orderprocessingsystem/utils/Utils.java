package com.orderprocessingsystem.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderprocessingsystem.constants.Constants.ActionType;
import com.orderprocessingsystem.constants.Constants.StorageType;
import com.orderprocessingsystem.ledger.Action;
import com.orderprocessingsystem.models.StoredOrder;

public class Utils {
  public static String toPrettyJson(Object obj) throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    // Convert object to pretty JSON string
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  public static Long currentTimestampMicros() {
    return System.currentTimeMillis() * 1_000L;
  }

  public static Action buildAction(StoredOrder storedOrder, StorageType target,
                                   ActionType action) {
    return Action.builder()
        .timestamp(currentTimestampMicros())
        .action(action)
        .id(storedOrder.getOrder().getId())
        .target(target)
        .build();
  }
}
