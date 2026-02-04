package com.orderprocessingsystem.ledger;

import static com.orderprocessingsystem.utils.Utils.toPrettyJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ActionLedger {
  private final Queue<Action> actions = new ConcurrentLinkedQueue<>();

  public void record(Action action) throws Exception {
    actions.add(action);
    System.out.println(toPrettyJson(action) + ",");
  }

  public List<Action> snapshot() { return new ArrayList<>(actions); }
}
