package com.orderprocessingsystem;

import static com.orderprocessingsystem.constants.Constants.MAX_PICKUP_OFFSET_MICRO;
import static com.orderprocessingsystem.constants.Constants.MIN_PICKUP_OFFSET_MICRO;
import static com.orderprocessingsystem.constants.Constants.RATE_MICRO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderprocessingsystem.models.Order;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      throw new IllegalArgumentException("Missing JSON config argument");
    }

    String jsonInput;
    if (args[0].endsWith(".json")) {
      jsonInput = new String(Files.readAllBytes(Paths.get(args[0])));
    } else {
      jsonInput = args[0];
    }

    // Parse orders from JSON input
    ObjectMapper mapper = new ObjectMapper();
    List<Order> orders = new ArrayList<>();
    try {
      orders = mapper.readValue(jsonInput, new TypeReference<List<Order>>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse JSON input", e);
    }

    // Schedule order placements and pickups
    ScheduledExecutorService placer =
        Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService pickupper = Executors.newScheduledThreadPool(4);

    OrderProcessingService service = new OrderProcessingService();
    for (Order order : orders) {
      placer.schedule(() -> {
        try {
          service.placeOrder(order);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        int delay = ThreadLocalRandom.current().nextInt(
            MIN_PICKUP_OFFSET_MICRO, MAX_PICKUP_OFFSET_MICRO);
        pickupper.schedule(() -> {
          try {
            service.pickupOrder(order.getId());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }, delay, TimeUnit.MICROSECONDS);
      }, RATE_MICRO, TimeUnit.MICROSECONDS);
    }
  }
}