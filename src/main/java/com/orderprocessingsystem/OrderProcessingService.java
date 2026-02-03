package com.orderprocessingsystem;

import static com.orderprocessingsystem.utils.Utils.currentTimestampMicros;

import com.orderprocessingsystem.ledger.Action;
import com.orderprocessingsystem.ledger.ActionLedger;
import com.orderprocessingsystem.models.Order;
import com.orderprocessingsystem.models.StoredOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OrderProcessingService {
  private final StorageManager storageManager = new StorageManager();
  private final ActionLedger ledger = new ActionLedger();
  private final ConcurrentMap<String, StoredOrder> index =
      new ConcurrentHashMap<>();

  public void placeOrder(Order order) throws Exception {
    StoredOrder storedOrder = StoredOrder.builder()
                         .order(order)
                         .storedAtMicros(currentTimestampMicros())
                         .build();

    storageManager.place(storedOrder, ledger);
    index.put(order.getId(), storedOrder);
  }

  public void pickupOrder(String orderId) {
    StoredOrder so = index.remove(orderId);
    if (so == null)
      return;

    if (so.isExpired(TimeUtil.nowMicros())) {
      storageManager.remove(so);
      ledger.record(Action.discard(so));
    } else {
      storageManager.remove(so);
      ledger.record(Action.pickup(so));
    }
  }
}
