package com.orderprocessingsystem.storage;

import static com.orderprocessingsystem.utils.Utils.currentTimestampMicros;

import com.orderprocessingsystem.constants.Constants.StorageType;
import com.orderprocessingsystem.constants.Constants.Temperature;
import com.orderprocessingsystem.models.StoredOrder;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;


public class Shelf implements Storage {
  private static final int CAPACITY = 12;

  private final Map<String, StoredOrder> orders = new HashMap<>();
  private final PriorityQueue<StoredOrder> orderQueue =
      new PriorityQueue<>(Comparator.comparingLong(
          o -> o.remainingFreshness(currentTimestampMicros())));

  @Override
  public boolean hasRoom() {
    return orders.size() < CAPACITY;
  }

  @Override
  public void add(StoredOrder storedOrder) {
    storedOrder.setStorageType(StorageType.SHELF);
    orders.put(storedOrder.getOrder().getId(), storedOrder);
    orderQueue.offer(storedOrder);
  }

  @Override
  public StoredOrder remove(String orderId) {
    StoredOrder storedOrder = orders.remove(orderId);
    if (storedOrder != null) {
      orderQueue.remove(storedOrder);
    }
    return storedOrder;
  }

  public StoredOrder discardWorst() {
    StoredOrder worstOrder = orderQueue.poll();
    if (worstOrder != null) {
      orders.remove(worstOrder.getOrder().getId());
    }
    return worstOrder;
  }

  public Optional<StoredOrder> findMovableForIdeal(Temperature temp) {
    return orderQueue.stream()
        .filter(o -> o.getOrder().getTemp() == temp)
        .sorted(Comparator.comparingLong(
            o -> o.remainingFreshness(currentTimestampMicros())))
        .findFirst();
  }
}
