package com.orderprocessingsystem.models;

import com.orderprocessingsystem.constants.Constants.Temperature;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class Order {
  private final String id;
  private final String name;
  private final Temperature temp;
  private final int price;
  @Setter private int freshnessSeconds;
}
