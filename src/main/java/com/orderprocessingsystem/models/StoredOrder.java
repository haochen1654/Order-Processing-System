package com.orderprocessingsystem.models;

import com.orderprocessingsystem.constants.Constants.StorageType;
import com.orderprocessingsystem.constants.Constants.Temperature;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
public class StoredOrder {
  @Getter private final Order order;
  @Setter @Getter private long storedAtMicros;
  @Setter @Getter private StorageType storageType;

  public boolean isExpired(long nowMicros) {
    return getEffectiveAgeSeconds(nowMicros) > order.getFreshnessSeconds();
  }

  public int remainingFreshness(long nowMicros) {
    return order.getFreshnessSeconds() - getEffectiveAgeSeconds(nowMicros);
  }

  private boolean isIdealStorage() {
    return (order.getTemp() == Temperature.HOT &&
            storageType == StorageType.HEATER) ||
        (order.getTemp() == Temperature.COLD &&
         storageType == StorageType.COOLER) ||
        (order.getTemp() == Temperature.ROOM &&
         storageType == StorageType.SHELF);
  }

  private int getEffectiveAgeSeconds(long nowMicros) {
    int ageSeconds = (int)((nowMicros - storedAtMicros) / 1_000_000L);
    return isIdealStorage() ? ageSeconds : ageSeconds * 2;
  }
}