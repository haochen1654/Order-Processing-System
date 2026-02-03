package com.orderprocessingsystem.ledger;

import com.orderprocessingsystem.constants.Constants.ActionType;
import com.orderprocessingsystem.constants.Constants.StorageType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Action {
  private final long timestamp;
  private final String id;
  private final ActionType action;
  private final StorageType target;
}
