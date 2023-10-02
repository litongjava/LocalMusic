package com.litongjava.localmusic.model;

import java.util.Map;

public class MessageWrap {

  public final String instructions;
  public final Map<String, Object> args;

  public static MessageWrap getInstance(String instructions, Map<String, Object> args) {
    return new MessageWrap(instructions, args);
  }

  private MessageWrap(String instructions, Map<String, Object> args) {
    this.instructions = instructions;
    this.args = args;
  }
}