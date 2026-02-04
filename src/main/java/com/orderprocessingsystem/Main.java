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

  private static final ObjectMapper mapper =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  public static void main(String[] args) {
    try {
      validateArgs(args);

      // Setup Configuration
      int rate = Integer.parseInt(args[0]);
      int min = Integer.parseInt(args[1]);
      int max = Integer.parseInt(args[2]);
      List<Order> orders = loadOrders(args[3]);

      // Execute Simulation
      OrderProcessingService service = new OrderProcessingService();
      runSimulation(orders, service, rate, min, max);

      // Generate and Save Report
      saveReport(service, rate, min, max);

    } catch (Exception e) {
      System.err.println("Simulation failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private static void runSimulation(List<Order> orders,
                                    OrderProcessingService service, int rate,
                                    int min, int max)
      throws InterruptedException {
    ScheduledExecutorService placer =
        Executors.newSingleThreadScheduledExecutor();
    ScheduledExecutorService pickupper = Executors.newScheduledThreadPool(4);
    CountDownLatch latch = new CountDownLatch(orders.size());

    System.out.println("Starting simulation for " + orders.size() +
                       " orders...");

    for (int i = 0; i < orders.size(); i++) {
      Order order = orders.get(i);
      long startDelay = (long)(i + 1) * rate;

      placer.schedule(()
                          -> placeAndSchedulePickup(order, service, pickupper,
                                                    latch, min, max),
                      startDelay, TimeUnit.MICROSECONDS);
    }

    latch.await();
    System.out.println("All orders processed.");

    placer.shutdown();
    pickupper.shutdown();
  }

  private static void placeAndSchedulePickup(Order order,
                                             OrderProcessingService service,
                                             ScheduledExecutorService pickupper,
                                             CountDownLatch latch, int min,
                                             int max) {
    try {
      service.placeOrder(order);

      int pickupDelay = ThreadLocalRandom.current().nextInt(min, max);
      pickupper.schedule(() -> {
        try {
          service.pickupOrder(order.getId());
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      }, pickupDelay, TimeUnit.MICROSECONDS);

    } catch (Exception e) {
      e.printStackTrace();
      latch.countDown(); // Prevent hang on failure
    }
  }

  private static List<Order> loadOrders(String source) throws IOException {
    String json = source.endsWith(".json")
                      ? new String(Files.readAllBytes(Paths.get(source)))
                      : source;
    return mapper.readValue(json, new TypeReference<List<Order>>() {});
  }

  private static void saveReport(OrderProcessingService service, int rate,
                                 int min, int max) throws IOException {
    ActionLog actionLog = service.generateLogReport(
        Options.builder().rate(rate).min(min).max(max).build());

    mapper.writeValue(new File("data.json"), actionLog);
    System.out.println("Report successfully saved to data.json");
  }

  private static void validateArgs(String[] args) {
    if (args.length < 4) {
      throw new IllegalArgumentException(
          "Usage: java -jar app.jar <RATE> <MIN> <MAX> <JSON_OR_FILE>");
    }
  }
}