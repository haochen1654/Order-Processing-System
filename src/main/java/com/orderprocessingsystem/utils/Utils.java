package com.orderprocessingsystem.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils {
  public static String toPrettyJson(Object obj) throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    // Convert object to pretty JSON string
    return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
  }

  public static Long currentTimestampMicros() {
    return System.currentTimeMillis() * 1_000L;
  }
}
