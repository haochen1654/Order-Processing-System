package com.orderprocessingsystem.storage;

import com.orderprocessingsystem.constants.Constants.StorageType;
import com.orderprocessingsystem.models.StoredOrder;
import java.util.HashMap;
import java.util.Map;

public class Heater implements Storage  {
  private static final int CAPACITY = 6;
  private final Map<String, StoredOrder> orders = new HashMap<>();

  @Override
  public boolean hasRoom() { return orders.size() < CAPACITY; }

  @Override
  public void add(StoredOrder storedOrder) {
    storedOrder.setStorageType(StorageType.HEATER);
    orders.put(storedOrder.getOrder().getId(), storedOrder);
  }

  @Override
  public StoredOrder remove(String orderId) { return orders.remove(orderId); }
}
