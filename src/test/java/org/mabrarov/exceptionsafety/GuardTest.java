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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class GuardTest {

  @Test
  public void test_getEmpty_returnsNull() throws Exception {
    try (final Guard guard = new Guard()) {
      assertThat(guard.get(), is(nullValue()));
    }
  }

  @Test
  public void test_get_returnsResource() throws Exception {
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      final AutoCloseable guardedResource = guard.set(resource);
      assertThat(guardedResource, is(sameInstance(resource)));
      assertThat(guard.get(), is(sameInstance(resource)));
    }
  }

  @Test
  public void test_releaseEmpty_returnsNull() throws Exception {
    try (final Guard guard = new Guard()) {
      assertThat(guard.release(), is(nullValue()));
    }
  }

  @Test
  public void test_release_resourceIsNotClosed() throws Exception {
    final AutoCloseable resource;
    try (final Guard guard = new Guard()) {
      resource = guard.set(mock(AutoCloseable.class));
      assertThat(guard.release(), is(sameInstance(resource)));
    }
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
    final AutoCloseable resource;
    try (final Guard guard = new Guard()) {
      resource = guard.set(mock(AutoCloseable.class));
    }
    verify(resource).close();
  }

  @Test
  public void test_closeTwice_resourceIsClosedOnce() throws Exception {
    final AutoCloseable resource;
    try (final Guard guard = new Guard()) {
      resource = guard.set(mock(AutoCloseable.class));
      //noinspection RedundantExplicitClose
      guard.close();
    }
    verify(resource).close();
  }

  @Test(expected = TestException.class)
  public void test_resourceCloseThrowsException_exceptionIsPropagated() throws Exception {
    try (final Guard guard = new Guard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      doThrow(new TestException()).when(resource).close();
      guard.set(resource);
    }
  }

  @Test
  public void test_resourceCloseThrowsException_subsequentCloseStillClosesResource()
      throws Exception {
    final Guard guard = new Guard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    doThrow(new TestException()).when(resource).close();
    guard.set(resource);

    closeSuppressingTestException(guard);
    closeSuppressingTestException(guard);

    verify(resource, times(2)).close();
  }

  @Test
  public void test_swapNonEmptyWithEmpty_becomesEmpty() throws Exception {
    final AutoCloseable resource;
    try (final Guard empty = new Guard()) {
      try (final Guard guard = new Guard()) {
        resource = guard.set(mock(AutoCloseable.class));
        assertThat(guard.get(), is(resource));
        assertThat(empty.get(), is(nullValue()));
        guard.swap(empty);
        assertThat(guard.get(), is(nullValue()));
        assertThat(empty.get(), is(resource));
      }
      verify(resource, never()).close();
    }
    verify(resource).close();
  }

  @Test
  public void test_swapEmptyWithNonEmpty_becomesNonEmpty() throws Exception {
    final AutoCloseable resource;
    try (final Guard empty = new Guard()) {
      try (final Guard guard = new Guard()) {
        resource = guard.set(mock(AutoCloseable.class));
        assertThat(guard.get(), is(resource));
        assertThat(empty.get(), is(nullValue()));
        empty.swap(guard);
        assertThat(guard.get(), is(nullValue()));
        assertThat(empty.get(), is(resource));
      }
      verify(resource, never()).close();
    }
    verify(resource).close();
  }

  @Test
  public void test_swap_resourcesAreSwapped() throws Exception {
    final AutoCloseable resource2;
    try (final Guard guard1 = new Guard()) {
      final AutoCloseable resource1 = guard1.set(mock(AutoCloseable.class));
      assertThat(guard1.get(), is(resource1));
      try (final Guard guard2 = new Guard()) {
        resource2 = guard2.set(mock(AutoCloseable.class));
        assertThat(guard2.get(), is(resource2));
        guard1.swap(guard2);
        assertThat(guard1.get(), is(resource2));
        assertThat(guard2.get(), is(resource1));
      }
      verify(resource1).close();
    }
    verify(resource2).close();
  }

  private void closeSuppressingTestException(final Guard guard) throws Exception {
    try {
      guard.close();
    } catch (final TestException ignored) {
      // Expected exception
    }
  }

}
