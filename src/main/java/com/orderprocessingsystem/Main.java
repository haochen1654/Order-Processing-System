package com.orderprocessingsystem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.orderprocessingsystem.ledger.ActionLog;
import com.orderprocessingsystem.models.Options;
import com.orderprocessingsystem.models.Order;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


public class Main {
  public static void main(String[] args) throws IOException {
    if (args.length < 4) {
      throw new IllegalArgumentException("Missing arguments");
    }

    int rateMicro = Integer.parseInt(args[0]);
    int minPickupOffsetMicro = Integer.parseInt(args[1]);
    int maxPickupOffsetMicro = Integer.parseInt(args[2]);
    // Read JSON input from file or direct string
    String jsonInput;
    if (!args[3].isEmpty() && args[3].endsWith(".json")) {
      jsonInput = new String(Files.readAllBytes(Paths.get(args[3])));
    } else {
      jsonInput = args[3];
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
    // Use CountDownLatch to wait for all orders to be processed if needed
    CountDownLatch latch = new CountDownLatch(orders.size());
    for (int i = 0; i < orders.size(); i++) {
      Order order = orders.get(i);
      // Calculate a unique delay for each order to create a staggered rate
      long staggeredDelay = (long)(i + 1) * rateMicro;

      placer.schedule(() -> {
        try {
          service.placeOrder(order);

          // Now schedule the pickup relative to placement time
          int pickupDelay = ThreadLocalRandom.current().nextInt(
              minPickupOffsetMicro, maxPickupOffsetMicro);

          pickupper.schedule(() -> {
            try {
              service.pickupOrder(order.getId());
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
              // Countdown the latch when pickup is done
              latch.countDown();
            }
          }, pickupDelay, TimeUnit.MICROSECONDS);

        } catch (Exception e) {
          e.printStackTrace();
          // If placement fails, still countdown the latch otherwise it will
          // hang
          latch.countDown();
        }
      }, staggeredDelay, TimeUnit.MICROSECONDS);
    }

    // Block the main thread here until the count reaches zero
    System.out.println("Waiting for all orders to complete...");

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // This runs ONLY after all latch.countDown() calls are finished
    System.out.println("All orders processed. Generating report...");
    ActionLog actionLog =
        service.generateLogReport(Options.builder()
                                      .rate(rateMicro)
                                      .min(minPickupOffsetMicro)
                                      .max(maxPickupOffsetMicro)
                                      .build());

    // Make the JSON look "pretty" (readable)
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    try {
      mapper.writeValue(new File("data.json"), actionLog);
      System.out.println("JSON successfully written to data.json");
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Clean up
    placer.shutdown();
    pickupper.shutdown();
  }
}