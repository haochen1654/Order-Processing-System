package com.orderprocessingsystem;

import com.orderprocessingsystem.constants.Constants;
import com.orderprocessingsystem.constants.Constants.ActionType;
import com.orderprocessingsystem.constants.Constants.StorageType;
import com.orderprocessingsystem.constants.Constants.Temperature;
import com.orderprocessingsystem.ledger.Action;
import com.orderprocessingsystem.ledger.ActionLedger;
import com.orderprocessingsystem.models.StoredOrder;
import com.orderprocessingsystem.storage.Cooler;
import com.orderprocessingsystem.storage.Heater;
import com.orderprocessingsystem.storage.Shelf;
import com.orderprocessingsystem.storage.Storage;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import static com.orderprocessingsystem.utils.Utils.currentTimestampMicros;

public class StorageManager {
  private final Heater heater = new Heater();
  private final Cooler cooler = new Cooler();
  private final Shelf shelf = new Shelf();

  private final ReentrantLock heaterLock = new ReentrantLock();
  private final ReentrantLock coolerLock = new ReentrantLock();
  private final ReentrantLock shelfLock = new ReentrantLock();

public void place(StoredOrder storedOrder, ActionLedger ledger) throws Exception {
    // Try placing the order in ideal storage first
    if (placeOrderToIdealStorage(storedOrder, ledger)) {
        return;
    }

    boolean acquired = false;
    try {
        acquired = shelfLock.tryLock(Constants.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        if (!acquired) return;

        if (shelf.hasRoom()) {
            addToShelf(storedOrder, ledger);
            return;
        }

        // Try moving an order from shelf to ideal storage to free up space
        if (tryMoveFromShelf(ledger) && shelf.hasRoom()) {
            addToShelf(storedOrder, ledger);
            return;
        }

        // If still no room, discard the worst order
        StoredOrder victim = shelf.discardWorst();
        if (victim != null) {
            ledger.record(buildAction(victim, StorageType.SHELF, ActionType.DISCARD));
        }
        addToShelf(storedOrder, ledger);

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (acquired) {
            shelfLock.unlock();
        }
    }
}

private void addToShelf(StoredOrder order, ActionLedger ledger) throws Exception {
    shelf.add(order);
    ledger.record(buildAction(order, StorageType.SHELF, ActionType.PLACE));
}

  private boolean placeOrderToIdealStorage(StoredOrder storedOrder,
                                           ActionLedger ledger)
      throws Exception {

    switch (storedOrder.getOrder().getTemp()) {
    case HOT:
      return tryPlace(storedOrder, heater, heaterLock, StorageType.HEATER,
                      ledger);

    case COLD:
      return tryPlace(storedOrder, cooler, coolerLock, StorageType.COOLER,
                      ledger);
    case ROOM:
      return tryPlace(storedOrder, shelf, shelfLock, StorageType.COOLER,
                      ledger);
    default:
      return false;
    }
  }

  private boolean tryPlace(StoredOrder storedOrder, Storage storage,
                           ReentrantLock lock, StorageType target,
                           ActionLedger ledger) throws Exception {

    boolean acquired = false;
    try {
      acquired = lock.tryLock(Constants.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);

      if (acquired && storage.hasRoom()) {
        storage.add(storedOrder);
        ledger.record(buildAction(storedOrder, target, ActionType.PLACE));
        return true;
      }
      return false;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;

    } finally {
      if (acquired) {
        lock.unlock();
      }
    }
  }

  private Action buildAction(StoredOrder storedOrder, StorageType target,
                             ActionType action) {
    return Action.builder()
        .timestamp(currentTimestampMicros())
        .action(action)
        .id(storedOrder.getOrder().getId())
        .target(target)
        .build();
  }

  private boolean tryMoveFromShelf(ActionLedger ledger) throws Exception {
    return tryMoveToStorage(coolerLock, cooler, Temperature.COLD,
                            StorageType.COOLER, ledger) ||
        tryMoveToStorage(heaterLock, heater, Temperature.HOT,
                         StorageType.HEATER, ledger);
  }

  private boolean tryMoveToStorage(ReentrantLock lock, Storage storage,
                                   Temperature temp, StorageType target,
                                   ActionLedger ledger) throws Exception {
    boolean acquired = false;
    try {
      acquired = lock.tryLock(Constants.LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
      if (acquired && storage.hasRoom()) {
        Optional<StoredOrder> candidateOpt = shelf.findMovableForIdeal(temp);
        if (candidateOpt.isPresent()) {
          StoredOrder candidate = candidateOpt.get();
          // Remove from shelf
          shelf.remove(candidate.getOrder().getId());
          // Add to ideal storage
          long currentTimeMicros = currentTimestampMicros();
          candidate.setStoredAtMicros(currentTimeMicros);
          candidate.getOrder().setFreshnessSeconds(
              candidate.remainingFreshness(currentTimeMicros));
          storage.add(candidate);
          ledger.record(buildAction(candidate, target, ActionType.MOVE));

          return true;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } finally {
      if (acquired) {
        lock.unlock();
      }
    }
    return false;
  }
}
