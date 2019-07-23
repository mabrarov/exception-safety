package org.mabrarov.exceptionsafety;

public class TestException extends Exception {

  private final int id;

  public TestException() {
    this(0);
  }

  public TestException(final int id) {
    super("Test exception");
    this.id = id;
  }

  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "TestException{" + "id=" + id + '}';
  }
}
