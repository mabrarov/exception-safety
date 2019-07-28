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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.InOrder;

public class PairGuardTest {

  @Test
  public void test_closeEmpty_doesNotThrowException() throws Exception {
    final PairGuard guard = new PairGuard();
    guard.close();
  }

  @Test
  public void test_closeNullFirst_secondResourceIsClosed() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.setSecond(resource);
    assertThat(guard.getSecond(), is(sameInstance(resource)));

    guard.close();

    assertThat(guard.getSecond(), is(nullValue()));
    verify(resource).close();
  }

  @Test
  public void test_closeNullSecond_firstResourceIsClosed() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.setFirst(resource);
    assertThat(guard.getFirst(), is(sameInstance(resource)));

    guard.close();

    assertThat(guard.getFirst(), is(nullValue()));
    verify(resource).close();
  }

  @Test
  public void test_close_resourcesAreClosed() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.setFirst(resource1);
    assertThat(guard.getFirst(), is(sameInstance(resource1)));

    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.setSecond(resource2);
    assertThat(guard.getSecond(), is(sameInstance(resource2)));

    guard.close();

    assertThat(guard.getFirst(), is(nullValue()));
    assertThat(guard.getSecond(), is(nullValue()));

    final InOrder inOrder = inOrder(resource1, resource2);
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
  }

  @Test
  public void test_releaseEmpty_doesNotThrowException() throws Exception {
    final PairGuard guard = new PairGuard();
    guard.release();
  }

  @Test
  public void test_releaseNullFirst_secondIsNotClosed() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.setSecond(resource);
    assertThat(guard.getSecond(), is(sameInstance(resource)));

    guard.release();
    assertThat(guard.getSecond(), is(nullValue()));

    guard.close();
    verify(resource, never()).close();
  }

  @Test
  public void test_releaseNullSecond_firstIsNotClosed() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.setFirst(resource);
    assertThat(guard.getFirst(), is(sameInstance(resource)));

    guard.release();
    assertThat(guard.getFirst(), is(nullValue()));

    guard.close();
    verify(resource, never()).close();
  }

  @Test
  public void test_release_resourcesAreNotClosed() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.setFirst(resource1);
    assertThat(guard.getFirst(), is(sameInstance(resource1)));
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.setSecond(resource2);
    assertThat(guard.getSecond(), is(sameInstance(resource2)));

    guard.release();
    assertThat(guard.getFirst(), is(nullValue()));
    assertThat(guard.getSecond(), is(nullValue()));

    guard.close();
    verify(resource1, never()).close();
    verify(resource2, never()).close();
  }

  @Test
  public void test_swapNonEmptyWithEmpty_becomesEmpty() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.setFirst(resource1);
    assertThat(guard.getFirst(), is(sameInstance(resource1)));
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.setSecond(resource2);
    assertThat(guard.getSecond(), is(sameInstance(resource2)));

    final PairGuard empty = new PairGuard();
    assertThat(empty.getFirst(), is(nullValue()));
    assertThat(empty.getSecond(), is(nullValue()));

    guard.swap(empty);

    assertThat(guard.getFirst(), is(nullValue()));
    assertThat(guard.getSecond(), is(nullValue()));
    assertThat(empty.getFirst(), is(sameInstance(resource1)));
    assertThat(empty.getSecond(), is(sameInstance(resource2)));

    guard.close();
    verify(resource1, never()).close();
    verify(resource2, never()).close();

    empty.close();
    final InOrder inOrder = inOrder(resource1, resource2);
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
  }

  @Test
  public void test_swapEmptyWithNonEmpty_becomesNonEmpty() throws Exception {
    final PairGuard guard = new PairGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.setFirst(resource1);
    assertThat(guard.getFirst(), is(sameInstance(resource1)));
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.setSecond(resource2);
    assertThat(guard.getSecond(), is(sameInstance(resource2)));

    final PairGuard empty = new PairGuard();
    assertThat(empty.getFirst(), is(nullValue()));
    assertThat(empty.getSecond(), is(nullValue()));

    empty.swap(guard);

    assertThat(guard.getFirst(), is(nullValue()));
    assertThat(guard.getSecond(), is(nullValue()));
    assertThat(empty.getFirst(), is(sameInstance(resource1)));
    assertThat(empty.getSecond(), is(sameInstance(resource2)));

    guard.close();
    verify(resource1, never()).close();
    verify(resource2, never()).close();

    empty.close();
    final InOrder inOrder = inOrder(resource1, resource2);
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
  }

  @Test
  public void test_swap_resourcesAreSwapped() throws Exception {
    final PairGuard guard1 = new PairGuard();
    final AutoCloseable resource11 = mock(AutoCloseable.class);
    guard1.setFirst(resource11);
    assertThat(guard1.getFirst(), is(sameInstance(resource11)));
    final AutoCloseable resource12 = mock(AutoCloseable.class);
    guard1.setSecond(resource12);
    assertThat(guard1.getSecond(), is(sameInstance(resource12)));

    final PairGuard guard2 = new PairGuard();
    final AutoCloseable resource21 = mock(AutoCloseable.class);
    guard2.setFirst(resource21);
    assertThat(guard2.getFirst(), is(sameInstance(resource21)));
    final AutoCloseable resource22 = mock(AutoCloseable.class);
    guard2.setSecond(resource22);
    assertThat(guard2.getSecond(), is(sameInstance(resource22)));

    guard1.swap(guard2);

    assertThat(guard1.getFirst(), is(sameInstance(resource21)));
    assertThat(guard1.getSecond(), is(sameInstance(resource22)));
    assertThat(guard2.getFirst(), is(sameInstance(resource11)));
    assertThat(guard2.getSecond(), is(sameInstance(resource12)));

    guard1.close();
    final InOrder inOrder1 = inOrder(resource21, resource22);
    inOrder1.verify(resource22).close();
    inOrder1.verify(resource21).close();

    guard2.close();
    final InOrder inOrder2 = inOrder(resource11, resource12);
    inOrder2.verify(resource12).close();
    inOrder2.verify(resource11).close();
  }

}
