package com.orderprocessingsystem.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class Constants {
  public enum Temperature {
    HOT,
    COLD,
    ROOM;
    @JsonCreator
    public static Temperature fromString(String value) {
      return switch (value.toLowerCase()) {
            case "hot" -> HOT;
            case "cold" -> COLD;
            case "room" -> ROOM;
            default -> throw new IllegalArgumentException("Unknown temp: " + value);
        };
    }
  }

  public enum StorageType {
    HEATER("heater"),
    COOLER("cooler"),
    SHELF("shelf");
    private final String storage;
    StorageType(String storage) { this.storage = storage; }
    @JsonValue
    public String getStorage() {
      return storage;
    }
  }
  public enum ActionType {
    PLACE("place"),
    MOVE("move"),
    PICKUP("pickup"),
    DISCARD("discard");
    private final String action;
    ActionType(String action) { this.action = action; }
    @JsonValue
    public String getAction() {
      return action;
    }
  }

  public static final int LOCK_TIMEOUT = 500;                // milliseconds
  public static final int RATE_MICRO = 500000;               // 0.5 seconds
  public static final int MIN_PICKUP_OFFSET_MICRO = 4000000; // 4 seconds
  public static final int MAX_PICKUP_OFFSET_MICRO = 8000000; // 8 seconds
}
