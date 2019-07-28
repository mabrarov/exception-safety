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

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;

public class NestedGuardTest {

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_getEmpty_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    guard.get(0);
  }

  @Test
  public void test_get_returnsResource() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    assertThat(guard.get(0), is(sameInstance(resource1)));

    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);
    assertThat(guard.get(0), is(sameInstance(resource1)));
    assertThat(guard.get(1), is(sameInstance(resource2)));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_getSmallIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    guard.get(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_getLargeIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    guard.get(2);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_setEmpty_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);

    guard.set(0, resource);
  }

  @Test
  public void test_set_changesGuardedInstance() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    assertThat(guard.get(0), is(sameInstance(resource1)));

    guard.set(0, resource2);

    assertThat(guard.get(0), is(sameInstance(resource2)));

    guard.close();
    verify(resource1, never()).close();
    verify(resource2).close();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_setSmallIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);
    final AutoCloseable resource3 = mock(AutoCloseable.class);

    guard.set(-1, resource3);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_setLargeIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);
    final AutoCloseable resource3 = mock(AutoCloseable.class);

    guard.set(2, resource3);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_releaseByIndexEmpty_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    guard.release(0);
  }

  @Test
  public void test_releaseByIndex_resourceIsNotClosed() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);
    assertThat(guard.size(), is(1));

    assertThat(guard.release(0), is(sameInstance(resource)));
    assertThat(guard.size(), is(1));

    guard.close();
    verify(resource, never()).close();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_releaseSmallIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    guard.release(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_releaseLargeIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    guard.release(2);
  }


  @Test
  public void test_releaseEmpty_doesNotThrowException() {
    final NestedGuard guard = new NestedGuard();
    guard.release();
  }

  @Test
  public void test_release_resourceIsNotClosed() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);
    assertThat(guard.size(), is(1));

    guard.release();
    assertThat(guard.size(), is(0));

    guard.close();
    verify(resource, never()).close();
  }

  /**
   * Ensure that no exception / error happens when empty guard is closed
   */
  @Test
  public void test_closeEmpty_doesNotThrowException() throws Exception {
    final NestedGuard guard = new NestedGuard();
    assertThat(guard.size(), is(0));
    guard.close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_closeSingleResource_resourceIsClosed() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);

    guard.close();

    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_closeMultipleResources_allResourcesAreClosed() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);

    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    final AutoCloseable resource3 = mock(AutoCloseable.class);
    guard.add(resource3);

    final AutoCloseable resource4 = mock(AutoCloseable.class);
    guard.add(resource4);

    guard.close();

    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_closeAndResourceCloseThrowsException_exceptionIsPropagatedWithSuppressedExceptions()
      throws Exception {
    final TestException closeException1 = new TestException(1);
    final TestException closeException2 = new TestException(2);
    final TestException closeException3 = new TestException(3);
    final TestException closeException4 = new TestException(4);
    final NestedGuard guard = new NestedGuard();
    AutoCloseable resource1 = null;
    AutoCloseable resource2 = null;
    AutoCloseable resource3 = null;
    AutoCloseable resource4 = null;
    try {
      resource1 = mock(AutoCloseable.class);
      doThrow(closeException1).when(resource1).close();
      guard.add(resource1);

      resource2 = mock(AutoCloseable.class);
      doThrow(closeException2).when(resource2).close();
      guard.add(resource2);

      resource3 = mock(AutoCloseable.class);
      doThrow(closeException3).when(resource3).close();
      guard.add(resource3);

      resource4 = mock(AutoCloseable.class);
      doThrow(closeException4).when(resource4).close();
      guard.add(resource4);

      guard.close();

      fail("Expected TestException");
    } catch (final TestException e) {
      assertThat(e, is(sameInstance(closeException4)));
      final List<Throwable> suppressed = getAllSuppressed(e);
      assertThat(suppressed, hasSize(3));
      assertSameInstance(suppressed.get(0), TestException.class, closeException3);
      assertSameInstance(suppressed.get(1), TestException.class, closeException2);
      assertSameInstance(suppressed.get(2), TestException.class, closeException1);
    }
    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(4));
  }

  @Test
  public void test_closeAndResourceCloseThrowsError_errorIsPropagatedWithSuppressedErrors()
      throws Exception {
    final TestError closeError1 = new TestError(1);
    final TestError closeError2 = new TestError(2);
    final TestError closeError3 = new TestError(3);
    final TestError closeError4 = new TestError(4);
    final NestedGuard guard = new NestedGuard();
    AutoCloseable resource1 = null;
    AutoCloseable resource2 = null;
    AutoCloseable resource3 = null;
    AutoCloseable resource4 = null;
    try {
      resource1 = mock(AutoCloseable.class);
      doThrow(closeError1).when(resource1).close();
      guard.add(resource1);

      resource2 = mock(AutoCloseable.class);
      doThrow(closeError2).when(resource2).close();
      guard.add(resource2);

      resource3 = mock(AutoCloseable.class);
      doThrow(closeError3).when(resource3).close();
      guard.add(resource3);

      resource4 = mock(AutoCloseable.class);
      doThrow(closeError4).when(resource4).close();
      guard.add(resource4);

      guard.close();

      fail("Expected TestError");
    } catch (final TestError e) {
      assertThat(e, is(sameInstance(closeError4)));
      final List<Throwable> suppressed = getAllSuppressed(e);
      assertThat(suppressed, hasSize(3));
      assertSameInstance(suppressed.get(0), TestError.class, closeError3);
      assertSameInstance(suppressed.get(1), TestError.class, closeError2);
      assertSameInstance(suppressed.get(2), TestError.class, closeError1);
    }
    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(4));
  }

  @Test
  public void test_closeAndFirstResourceCloseThrowsException_exceptionIsPropagatedAndSizeIsReduced()
      throws Exception {
    final TestException closeException1 = new TestException(1);
    final NestedGuard guard = new NestedGuard();
    AutoCloseable resource1 = null;
    AutoCloseable resource2 = null;
    AutoCloseable resource3 = null;
    AutoCloseable resource4 = null;
    try {
      resource1 = mock(AutoCloseable.class);
      doThrow(closeException1).when(resource1).close();
      guard.add(resource1);

      resource2 = mock(AutoCloseable.class);
      guard.add(resource2);

      resource3 = mock(AutoCloseable.class);
      guard.add(resource3);

      resource4 = mock(AutoCloseable.class);
      guard.add(resource4);

      guard.close();

      fail("Expected TestException");
    } catch (final TestException e) {
      assertThat(e, is(sameInstance(closeException1)));
    }
    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(1));
  }

  @Test
  public void test_closeAndLastResourceCloseThrowsException_exceptionIsPropagatedAndSizeIsReduced()
      throws Exception {
    final TestException closeException4 = new TestException(4);
    final NestedGuard guard = new NestedGuard();
    AutoCloseable resource1 = null;
    AutoCloseable resource2 = null;
    AutoCloseable resource3 = null;
    AutoCloseable resource4 = null;
    try {
      resource1 = mock(AutoCloseable.class);
      guard.add(resource1);

      resource2 = mock(AutoCloseable.class);
      guard.add(resource2);

      resource3 = mock(AutoCloseable.class);
      guard.add(resource3);

      resource4 = mock(AutoCloseable.class);
      doThrow(closeException4).when(resource4).close();
      guard.add(resource4);

      guard.close();

      fail("Expected TestException");
    } catch (final TestException e) {
      assertThat(e, is(sameInstance(closeException4)));
    }
    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(1));
  }

  @Test
  public void test_closeAndSingleMidResourceCloseThrowsException_exceptionIsPropagatedAndSizeIsReduced()
      throws Exception {
    final TestException closeException2 = new TestException(2);
    final NestedGuard guard = new NestedGuard();
    AutoCloseable resource1 = null;
    AutoCloseable resource2 = null;
    AutoCloseable resource3 = null;
    AutoCloseable resource4 = null;
    try {
      resource1 = mock(AutoCloseable.class);
      guard.add(resource1);

      resource2 = mock(AutoCloseable.class);
      doThrow(closeException2).when(resource2).close();
      guard.add(resource2);

      resource3 = mock(AutoCloseable.class);
      guard.add(resource3);

      resource4 = mock(AutoCloseable.class);
      guard.add(resource4);

      guard.close();

      fail("Expected TestException");
    } catch (final TestException e) {
      assertThat(e, is(sameInstance(closeException2)));
    }
    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(1));
  }

  @Test
  public void test_closeAnd2MidResourcesCloseThrowException_exceptionIsPropagatedAndSizeIsReduced()
      throws Exception {
    final TestException closeException2 = new TestException(2);
    final TestException closeException3 = new TestException(3);
    final NestedGuard guard = new NestedGuard();
    AutoCloseable resource1 = null;
    AutoCloseable resource2 = null;
    AutoCloseable resource3 = null;
    AutoCloseable resource4 = null;
    try {
      resource1 = mock(AutoCloseable.class);
      guard.add(resource1);

      resource2 = mock(AutoCloseable.class);
      doThrow(closeException2).when(resource2).close();
      guard.add(resource2);

      resource3 = mock(AutoCloseable.class);
      doThrow(closeException3).when(resource3).close();
      guard.add(resource3);

      resource4 = mock(AutoCloseable.class);
      guard.add(resource4);

      guard.close();

      fail("Expected TestException");
    } catch (final TestException e) {
      assertThat(e, is(sameInstance(closeException3)));
      final List<Throwable> suppressed = getAllSuppressed(e);
      assertThat(suppressed, hasSize(1));
      assertSameInstance(suppressed.get(0), TestException.class, closeException2);
    }
    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(2));
  }

  @Test
  public void test_closeEmptyItems_doesNotThrowException() throws Exception {
    final NestedGuard guard = spy(new NestedGuard());
    doNothing().when(guard).addItem(Matchers.<List<PairGuard>>any(), Matchers.<PairGuard>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);
    assertThat(guard.size(), is(0));

    guard.close();
    verify(resource, never()).close();
  }

  @Test
  public void test_addThrowsException_resourceIsClosed() throws Exception {
    final TestRuntimeException addException = new TestRuntimeException();
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addException).when(guard)
        .addItem(Matchers.<List<PairGuard>>any(), Matchers.<PairGuard>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    try {
      guard.add(resource);
      fail("Expected TestRuntimeException");
    } catch (final TestRuntimeException e) {
      assertThat(e, is(sameInstance(addException)));
    }
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_addThrowsError_resourceIsClosed() throws Exception {
    final TestError addError = new TestError();
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addError).when(guard)
        .addItem(Matchers.<List<PairGuard>>any(), Matchers.<PairGuard>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    try {
      guard.add(resource);
      fail("Expected TestRuntimeException");
    } catch (final TestError e) {
      assertThat(e, is(sameInstance(addError)));
    }
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_addThrowsExceptionAndResourceCloseThrowsException_resourceIsClosedWithException()
      throws Exception {
    final TestRuntimeException addException = new TestRuntimeException();
    final TestException closeException = new TestException();
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addException).when(guard)
        .addItem(Matchers.<List<PairGuard>>any(), Matchers.<PairGuard>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    doThrow(closeException).when(resource).close();
    try {
      guard.add(resource);
      fail("Expected TestRuntimeException");
    } catch (final TestRuntimeException e) {
      assertThat(e, is(sameInstance(addException)));
      final List<Throwable> suppressed = getAllSuppressed(e);
      assertThat(suppressed, hasSize(1));
      assertSameInstance(suppressed.get(0), TestException.class, closeException);
    }
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_addThrowsErrorAndResourceCloseThrowsError_resourceIsClosedWithError()
      throws Exception {
    final TestError addError = new TestError(1);
    final TestError closeError = new TestError(2);
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addError).when(guard)
        .addItem(Matchers.<List<PairGuard>>any(), Matchers.<PairGuard>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    doThrow(closeError).when(resource).close();
    try {
      guard.add(resource);
      fail("Expected TestError");
    } catch (final TestError e) {
      assertThat(e, is(sameInstance(addError)));
      final List<Throwable> suppressed = getAllSuppressed(e);
      assertThat(suppressed, hasSize(1));
      assertSameInstance(suppressed.get(0), TestError.class, closeError);
    }
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeEmpty_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    guard.remove(0);
  }

  @Test
  public void test_removeSingleItem_becomesEmpty() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);
    assertThat(guard.size(), is(not(0)));
    assertThat(guard.remove(0), is(sameInstance(resource)));
    guard.close();
    assertThat(guard.size(), is(0));
    verify(resource, never()).close();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeSingleItemInvalidIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);
    assertThat(guard.size(), is(not(0)));
    guard.remove(1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeSmallIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    guard.remove(-1);
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeLargeIndex_indexOutOfBoundException() {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    guard.remove(2);
  }

  @Test
  public void test_removeFirstItem_removedItemNotClosedAndSizeReduced() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);

    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    final AutoCloseable resource3 = mock(AutoCloseable.class);
    guard.add(resource3);

    final AutoCloseable resource4 = mock(AutoCloseable.class);
    guard.add(resource4);

    assertThat(guard.remove(0), is(sameInstance(resource1)));
    assertThat(guard.size(), is(3));

    guard.close();

    final InOrder inOrder = inOrder(resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    verify(resource1, never()).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_removeLastItem_removedItemNotClosedAndSizeReduced() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);

    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    final AutoCloseable resource3 = mock(AutoCloseable.class);
    guard.add(resource3);

    final AutoCloseable resource4 = mock(AutoCloseable.class);
    guard.add(resource4);

    assertThat(guard.remove(3), is(sameInstance(resource4)));
    assertThat(guard.size(), is(3));

    guard.close();

    final InOrder inOrder = inOrder(resource1, resource2, resource3);
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    verify(resource4, never()).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_removeMidItem_removedItemNotClosedAndSizeReduced() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);

    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    final AutoCloseable resource3 = mock(AutoCloseable.class);
    guard.add(resource3);

    final AutoCloseable resource4 = mock(AutoCloseable.class);
    guard.add(resource4);

    assertThat(guard.remove(1), is(sameInstance(resource2)));
    assertThat(guard.size(), is(3));

    guard.close();

    final InOrder inOrder = inOrder(resource1, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource1).close();
    verify(resource2, never()).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_removeItemBeforeLast_removedItemNotClosedAndSizeReduced() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard.add(resource1);

    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard.add(resource2);

    final AutoCloseable resource3 = mock(AutoCloseable.class);
    guard.add(resource3);

    final AutoCloseable resource4 = mock(AutoCloseable.class);
    guard.add(resource4);

    assertThat(guard.remove(2), is(sameInstance(resource3)));
    assertThat(guard.size(), is(3));

    guard.close();

    final InOrder inOrder = inOrder(resource1, resource2, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    verify(resource3, never()).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_swapNonEmptyWithEmpty_becomesEmpty() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);
    assertThat(guard.get(0), is(resource));

    final NestedGuard empty = new NestedGuard();
    assertThat(empty.size(), is(0));

    guard.swap(empty);

    assertThat(guard.size(), is(0));
    assertThat(empty.get(0), is(resource));

    guard.close();
    verify(resource, never()).close();

    empty.close();
    verify(resource).close();
  }

  @Test
  public void test_swapEmptyWithNonEmpty_becomesNonEmpty() throws Exception {
    final NestedGuard empty = new NestedGuard();
    assertThat(empty.size(), is(0));

    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = mock(AutoCloseable.class);
    guard.add(resource);
    assertThat(guard.get(0), is(resource));

    empty.swap(guard);

    assertThat(guard.size(), is(0));
    assertThat(empty.get(0), is(resource));

    guard.close();
    verify(resource, never()).close();

    empty.close();
    verify(resource).close();
  }

  @Test
  public void test_swap_resourcesAreSwapped() throws Exception {
    final NestedGuard guard1 = new NestedGuard();
    final AutoCloseable resource1 = mock(AutoCloseable.class);
    guard1.add(resource1);
    assertThat(guard1.get(0), is(resource1));

    final NestedGuard guard2 = new NestedGuard();
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    guard2.add(resource2);
    assertThat(guard2.get(0), is(resource2));

    guard1.swap(guard2);

    assertThat(guard1.get(0), is(resource2));
    assertThat(guard2.get(0), is(resource1));

    guard1.close();
    verify(resource2).close();

    guard2.close();
    verify(resource1).close();
  }

  private static List<Throwable> getAllSuppressed(final Throwable throwable) {
    final Throwable[] suppressed = throwable.getSuppressed();
    if (suppressed == null || suppressed.length == 0) {
      return Collections.emptyList();
    }
    final List<Throwable> throwables = new ArrayList<>();
    for (final Throwable t : suppressed) {
      throwables.add(t);
      throwables.addAll(getAllSuppressed(t));
    }
    return throwables;
  }

  private static <T> void assertSameInstance(final Throwable actual, final Class<T> expectedType,
      final T expected) {
    assertThat(actual, is(instanceOf(expectedType)));
    assertThat(expectedType.cast(actual), is(sameInstance(expected)));
  }

}
