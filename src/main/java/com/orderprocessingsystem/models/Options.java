package com.orderprocessingsystem.models;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Options {
  private int rate;
  private int min;
  private int max;
}
