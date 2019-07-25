package org.mabrarov.exceptionsafety;

public class Guard implements AutoCloseable {

  private AutoCloseable resource;

  /**
   * Sets guarded instance of {@link AutoCloseable}. Provides no-throw guarantee. Resource which was
   * guarded before is not impacted and is forgotten.
   *
   * @param resource instance of {@link AutoCloseable} to guard. {@code null} is allowed and means
   * no resource is guarded (equivalent to {@link Guard#release()}).
   */
  public void set(final AutoCloseable resource) {
    this.resource = resource;
  }

  /**
   * Retrieves guarded instance of {@link AutoCloseable}. Provides no-throw guarantee.
   *
   * @return guarded instance of {@link AutoCloseable} or {@code null} if nothing is guarded.
   */
  public AutoCloseable get() {
    return resource;
  }

  /**
   * Resets this instance to guard nothing. Provides no-throw guarantee. Equivalent to {@link
   * Guard#set(java.lang.AutoCloseable)} with {@code null} as {@code resource}.
   */
  public void release() {
    resource = null;
  }

  /**
   * Swaps this instance with another instance. Provides no-throw guarantee.
   *
   * @param other another instance to swap with.
   */
  public void swap(final Guard other) {
    final AutoCloseable thisResource = this.resource;
    this.resource = other.resource;
    other.resource = thisResource;
  }

  /**
   * Closes guarded instance of {@link AutoCloseable} by invocation of its {@link
   * AutoCloseable#close()} method. Provides basic exception safety. If no exception is thrown by
   * guarded instance of {@link AutoCloseable} then this method provides no-throw guarantee. If
   * nothing ({@code null}) is guarded then does nothing. If {@link AutoCloseable#close()} method of
   * guarded instance of {@link AutoCloseable} throws exception then this guard instance remains
   * guarding instance of {@link AutoCloseable} and subsequent calls of {@link Guard#close()} work
   * the same way (calling {@link AutoCloseable#close()} method of guarded instance of {@link
   * AutoCloseable}). If guarded instance of {@link AutoCloseable} completes without throwing
   * exception then this guard instance is reset to guard nothing (i.e. {@link Guard#get()} returns
   * {@code null}), so subsequent calls of this method do nothing.
   *
   * @throws Exception if {@link AutoCloseable#close()} method of guarded instance of {@link
   * AutoCloseable} throws {@link Exception}, i.e. this method throws the same exceptions as {@link
   * AutoCloseable#close()} method of guarded instance of {@link AutoCloseable}.
   */
  @Override
  public void close() throws Exception {
    if (resource == null) {
      return;
    }
    resource.close();
    resource = null;
  }
}
