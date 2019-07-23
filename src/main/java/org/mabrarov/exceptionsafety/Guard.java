package org.mabrarov.exceptionsafety;

public class Guard implements AutoCloseable {

  private AutoCloseable resource;

  public void set(final AutoCloseable resource) {
    this.resource = resource;
  }

  public AutoCloseable get() {
    return resource;
  }

  public void release() {
    resource = null;
  }

  public void swap(final Guard other) {
    final AutoCloseable thisResource = this.resource;
    this.resource = other.resource;
    other.resource = thisResource;
  }

  @Override
  public void close() throws Exception {
    if (resource == null) {
      return;
    }
    resource.close();
    resource = null;
  }
}
