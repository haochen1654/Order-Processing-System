package com.orderprocessingsystem.storage;

import com.orderprocessingsystem.models.StoredOrder;

public interface Storage {

  public boolean hasRoom();

  public void add(StoredOrder storedOrder);

  public StoredOrder remove(String orderId);
}
