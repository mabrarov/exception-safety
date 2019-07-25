/*
 * Copyright (c) 2019 Marat Abrarov (abrarov@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
  public void test_closeEmpty_doesNotThrowException() throws Exception {
    final Guard guard = new Guard();
    guard.close();
  }

  @Test
  public void test_close_resourceIsClosed() throws Exception {
    final Guard guard = new Guard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.set(resource);

    guard.close();

    verify(resource).close();
  }

  @Test
  public void test_closeTwice_resourceIsClosedOnce() throws Exception {
    final Guard guard = new Guard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.set(resource);

    guard.close();
    guard.close();

    verify(resource).close();
  }

  @Test(expected = TestException.class)
  public void test_resourceCloseThrowsException_exceptionIsPropagated() throws Exception {
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(resource);
      doThrow(new TestException()).when(resource).close();
    }
  }

  @Test
  public void test_resourceCloseThrowsException_subsequentCloseStillClosesResource()
      throws Exception {
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
    final Guard guard = new Guard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.set(resource);

    guard.release();
    guard.close();

    verify(resource, never()).close();
  }

  @Test
  public void test_release_getReturnsNull() throws Exception {
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(resource);
      assertThat(guard.get(), is(resource));

      guard.release();

      assertThat(guard.get(), is(nullValue()));
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
