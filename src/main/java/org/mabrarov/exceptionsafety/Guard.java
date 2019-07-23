package org.mabrarov.exceptionsafety;

public class Guard implements AutoCloseable {

  private AutoCloseable resource;

  public void set(final AutoCloseable resource) {
    this.resource = resource;
  }

  public AutoCloseable release() {
    AutoCloseable releasedResource = resource;
    resource = null;
    return releasedResource;
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
