package org.mabrarov.exceptionsafety;

public class PairGuard implements AutoCloseable {

  private final Guard firstGuard = new Guard();
  private AutoCloseable second;

  public void setFirst(final AutoCloseable first) {
    this.firstGuard.set(first);
  }

  public void setSecond(final AutoCloseable second) {
    this.second = second;
  }

  public AutoCloseable getFirst() {
    return firstGuard.get();
  }

  public AutoCloseable getSecond() {
    return second;
  }

  public void releaseFirst() {
    firstGuard.release();
  }

  public void releaseSecond() {
    second = null;
  }

  public void release() {
    firstGuard.release();
    second = null;
  }

  /**
   * Swaps this instance with another instance. Provides no-throw guarantee.
   *
   * @param other another instance to swap with
   */
  public void swap(final PairGuard other) {
    this.firstGuard.swap(other.firstGuard);
    final AutoCloseable thisSecond = this.second;
    this.second = other.second;
    other.second = thisSecond;
  }

  @Override
  public void close() throws Exception {
    try (@SuppressWarnings("unused") final AutoCloseable guard = firstGuard) {
      if (second != null) {
        second.close();
        second = null;
      }
    }
  }
}
