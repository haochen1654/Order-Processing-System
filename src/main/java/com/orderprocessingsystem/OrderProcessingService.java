package com.orderprocessingsystem;

import static com.orderprocessingsystem.utils.Utils.buildAction;
import static com.orderprocessingsystem.utils.Utils.currentTimestampMicros;

import com.orderprocessingsystem.constants.Constants.ActionType;
import com.orderprocessingsystem.ledger.Action;
import com.orderprocessingsystem.ledger.ActionLedger;
import com.orderprocessingsystem.models.Order;
import com.orderprocessingsystem.models.StoredOrder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OrderProcessingService {
  private final StorageManager storageManager = new StorageManager();
  private final ActionLedger ledger = new ActionLedger();
  private final ConcurrentMap<String, StoredOrder> indexOfOrder =
      new ConcurrentHashMap<>();

  public void placeOrder(Order order) throws Exception {
    StoredOrder storedOrder = StoredOrder.builder()
                                  .order(order)
                                  .storedAtMicros(currentTimestampMicros())
                                  .build();

    storageManager.place(storedOrder, ledger);
    indexOfOrder.put(order.getId(), storedOrder);
  }

  public void pickupOrder(String orderId) throws Exception {
    StoredOrder storedOrder = indexOfOrder.remove(orderId);
    if (storedOrder == null)
      return;

    if (storedOrder.isExpired(currentTimestampMicros())) {
      storageManager.remove(storedOrder);
      ledger.record(buildAction(storedOrder,
                                /* target= */ storedOrder.getStorageType(),
                                /* action= */ ActionType.DISCARD));
      return;
    }

    storageManager.remove(storedOrder);
    ledger.record(buildAction(storedOrder,
                                /* target= */ storedOrder.getStorageType(),
                                /* action= */ ActionType.PICKUP));
  }

}
