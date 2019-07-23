package org.mabrarov.exceptionsafety;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class GuardTest {

  /**
   * Ensure that no exception / error happens when empty guard is closed
   */
  @Test
  public void test_emptyClose_doesNotThrow() throws Exception {
    final Guard guard = new Guard();
    guard.close();
  }

  @Test
  public void test_close_resourceIsClosed() throws Exception {
    final AutoCloseable guarded;
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(resource);
      guarded = resource;
    }
    verify(guarded).close();
  }

  @Test
  public void test_closeTwice_resourceIsClosedOnce() throws Exception {
    final AutoCloseable guarded;
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(resource);
      guarded = resource;
      guard.close();
    }
    verify(guarded).close();
  }

  @Test(expected = TestException.class)
  public void test_resourceCloseThrows_exceptionIsPropagated() throws Exception {
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(resource);
      doThrow(new TestException()).when(resource).close();
    }
  }

  @Test
  public void test_resourceCloseThrows_secondCallStillClosesResource() throws Exception {
    final Guard guard = new Guard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.set(resource);
    doThrow(new TestException()).when(resource).close();
    closeSuppressingTestException(guard);
    closeSuppressingTestException(guard);
    verify(resource, times(2)).close();
  }

  @Test
  public void test_release_resourceIsNotClosed() throws Exception {
    final AutoCloseable guarded;
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(resource);
      guarded = resource;
      assertThat(guard.release(), is(resource));
    }
    verify(guarded, never()).close();
  }

  @Test
  public void test_secondRelease_returnsNull() throws Exception {
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(resource);
      assertThat(guard.release(), is(resource));
      assertThat(guard.release(), is(nullValue()));
    }
  }

  private void closeSuppressingTestException(final Guard guard) throws Exception {
    try {
      guard.close();
    } catch (final TestException ignored) {
      // Expected exception
    }
  }

}
