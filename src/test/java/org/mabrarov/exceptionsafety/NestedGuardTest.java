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

import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.Is.is;
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

import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;

public class NestedGuardTest {

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_getEmpty_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.get(0);
    }
  }

  @Test
  public void test_get_returnsResource() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      final AutoCloseable resource1 = mock(AutoCloseable.class);
      final AutoCloseable addedResource1 = guard.add(resource1);
      assertThat(addedResource1, is(sameInstance(resource1)));
      assertThat(guard.get(0), is(sameInstance(resource1)));

      final AutoCloseable resource2 = mock(AutoCloseable.class);
      final AutoCloseable addedResource2 = guard.add(resource2);
      assertThat(addedResource2, is(sameInstance(resource2)));
      assertThat(guard.get(0), is(sameInstance(resource1)));
      assertThat(guard.get(1), is(sameInstance(resource2)));
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_getSmallIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      guard.get(-1);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_getLargeIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      guard.get(2);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_setEmpty_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      final AutoCloseable resource = mock(AutoCloseable.class);
      guard.set(0, resource);
    }
  }

  @Test
  public void test_set_changesGuardedInstance() throws Exception {
    final AutoCloseable resource2 = mock(AutoCloseable.class);
    final AutoCloseable resource1;
    try (final NestedGuard guard = new NestedGuard()) {
      resource1 = guard.add(mock(AutoCloseable.class));
      assertThat(guard.get(0), is(sameInstance(resource1)));
      guard.set(0, resource2);
      assertThat(guard.get(0), is(sameInstance(resource2)));
    }
    verify(resource1, never()).close();
    verify(resource2).close();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_setSmallIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      final AutoCloseable resource3 = mock(AutoCloseable.class);
      guard.set(-1, resource3);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_setLargeIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      final AutoCloseable resource3 = mock(AutoCloseable.class);
      guard.set(2, resource3);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_releaseByIndexEmpty_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.release(0);
    }
  }

  @Test
  public void test_releaseByIndex_resourceIsNotClosed() throws Exception {
    final AutoCloseable resource;
    try (final NestedGuard guard = new NestedGuard()) {
      resource = guard.add(mock(AutoCloseable.class));
      assertThat(guard.size(), is(1));
      assertThat(guard.release(0), is(sameInstance(resource)));
      assertThat(guard.size(), is(1));
    }
    verify(resource, never()).close();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_releaseSmallIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      guard.release(-1);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_releaseLargeIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      guard.release(2);
    }
  }

  @Test
  public void test_releaseEmpty_doesNotThrowException() {
    final NestedGuard guard = new NestedGuard();
    guard.release();
  }

  @Test
  public void test_release_resourceIsNotClosed() throws Exception {
    final AutoCloseable resource;
    try (final NestedGuard guard = new NestedGuard()) {
      resource = guard.add(mock(AutoCloseable.class));
      assertThat(guard.size(), is(1));
      guard.release();
      assertThat(guard.size(), is(0));
    }
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
    final AutoCloseable resource = guard.add(mock(AutoCloseable.class));
    guard.close();
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_closeMultipleResources_allResourcesAreClosed() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource2 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource3 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource4 = guard.add(mock(AutoCloseable.class));
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
      final Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed, is(arrayWithSize(3)));
      assertThat(suppressed[0], is(sameInstance((Throwable) closeException3)));
      assertThat(suppressed[1], is(sameInstance((Throwable) closeException2)));
      assertThat(suppressed[2], is(sameInstance((Throwable) closeException1)));
    }
    final InOrder inOrder = inOrder(resource1, resource2, resource3, resource4);
    inOrder.verify(resource4).close();
    inOrder.verify(resource3).close();
    inOrder.verify(resource2).close();
    inOrder.verify(resource1).close();
    assertThat(guard.size(), is(4));
  }

  @Test
  public void test_closeAndResourceCloseThrowsExceptionAndAddSuppressedThrowsError_resourcesAreClosed()
      throws Exception {
    final TestException closeException1 = new TestException(1);
    final TestException closeException2 = new TestException(2);
    final TestException closeException3 = new TestException(3);
    final TestException closeException4 = spy(new TestException(4));
    final TestError addSuppressedError = new TestError(3);
    doThrow(addSuppressedError).when(closeException4).addSuppressed(closeException3);
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

      fail("Expected TestError");
    } catch (final TestError e) {
      assertThat(e, is(sameInstance(addSuppressedError)));
      final Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed, is(arrayWithSize(2)));
      assertThat(suppressed[0], is(sameInstance((Throwable) closeException2)));
      assertThat(suppressed[1], is(sameInstance((Throwable) closeException1)));
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
      final Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed, is(arrayWithSize(3)));
      assertThat(suppressed[0], is(sameInstance((Throwable) closeError3)));
      assertThat(suppressed[1], is(sameInstance((Throwable) closeError2)));
      assertThat(suppressed[2], is(sameInstance((Throwable) closeError1)));
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

      resource2 = guard.add(mock(AutoCloseable.class));
      resource3 = guard.add(mock(AutoCloseable.class));
      resource4 = guard.add(mock(AutoCloseable.class));

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
      resource1 = guard.add(mock(AutoCloseable.class));
      resource2 = guard.add(mock(AutoCloseable.class));
      resource3 = guard.add(mock(AutoCloseable.class));
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
      resource1 = guard.add(mock(AutoCloseable.class));

      resource2 = mock(AutoCloseable.class);
      doThrow(closeException2).when(resource2).close();
      guard.add(resource2);

      resource3 = guard.add(mock(AutoCloseable.class));
      resource4 = guard.add(mock(AutoCloseable.class));

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
      resource1 = guard.add(mock(AutoCloseable.class));

      resource2 = mock(AutoCloseable.class);
      doThrow(closeException2).when(resource2).close();
      guard.add(resource2);

      resource3 = mock(AutoCloseable.class);
      doThrow(closeException3).when(resource3).close();
      guard.add(resource3);

      resource4 = guard.add(mock(AutoCloseable.class));

      guard.close();

      fail("Expected TestException");
    } catch (final TestException e) {
      assertThat(e, is(sameInstance(closeException3)));
      final Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed, is(arrayWithSize(1)));
      assertThat(suppressed[0], is(sameInstance((Throwable) closeException2)));
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
    doNothing().when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
    final AutoCloseable resource = guard.add(mock(AutoCloseable.class));
    assertThat(guard.size(), is(0));

    guard.close();
    verify(resource, never()).close();
  }

  @Test
  public void test_addNull_doesNotThrowException() throws Exception {
    final NestedGuard guard = new NestedGuard();
    guard.add(null);

    assertThat(guard.size(), is(1));
    guard.close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_addNullThrowsException_addExceptionIsThrown() {
    final TestRuntimeException addException = new TestRuntimeException();
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addException).when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
    try {
      guard.add(null);
      fail("Expected TestRuntimeException");
    } catch (final TestRuntimeException e) {
      assertThat(e, is(sameInstance(addException)));
    }
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_addThrowsException_resourceIsClosed() throws Exception {
    final TestRuntimeException addException = new TestRuntimeException();
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addException).when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
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
    doThrow(addError).when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
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
    doThrow(addException).when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    doThrow(closeException).when(resource).close();
    try {
      guard.add(resource);
      fail("Expected TestRuntimeException");
    } catch (final TestRuntimeException e) {
      assertThat(e, is(sameInstance(addException)));
      final Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed, is(arrayWithSize(1)));
      assertThat(suppressed[0], is(sameInstance((Throwable) closeException)));
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
    doThrow(addError).when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    doThrow(closeError).when(resource).close();
    try {
      guard.add(resource);
      fail("Expected TestError");
    } catch (final TestError e) {
      assertThat(e, is(sameInstance(addError)));
      final Throwable[] suppressed = e.getSuppressed();
      assertThat(suppressed, is(arrayWithSize(1)));
      assertThat(suppressed[0], is(sameInstance((Throwable) closeError)));
    }
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_addThrowsErrorAndResourceCloseThrowsErrorAndAddSuppressedThrowsRuntimeException_resourceIsClosedWithError()
      throws Exception {
    final TestRuntimeException addSuppressedException = new TestRuntimeException();
    final TestError addError = spy(new TestError(1));
    doThrow(addSuppressedException).when(addError).addSuppressed(ArgumentMatchers.<Throwable>any());
    final TestError closeError = new TestError(2);
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addError).when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    doThrow(closeError).when(resource).close();
    try {
      guard.add(resource);
      fail("Expected TestRuntimeException");
    } catch (final TestRuntimeException e) {
      assertThat(e, is(sameInstance(addSuppressedException)));
    }
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test
  public void test_addThrowsErrorAndResourceCloseThrowsErrorAndAddSuppressedThrowsError_resourceIsClosedWithError()
      throws Exception {
    final TestError addSuppressedError = new TestError(3);
    final TestError addError = spy(new TestError(1));
    doThrow(addSuppressedError).when(addError).addSuppressed(ArgumentMatchers.<Throwable>any());
    final TestError closeError = new TestError(2);
    final NestedGuard guard = spy(new NestedGuard());
    doThrow(addError).when(guard).addItem(ArgumentMatchers.<List<AutoCloseable>>any(),
        ArgumentMatchers.<AutoCloseable>any());
    final AutoCloseable resource = mock(AutoCloseable.class);
    doThrow(closeError).when(resource).close();
    try {
      guard.add(resource);
      fail("Expected TestError");
    } catch (final TestError e) {
      assertThat(e, is(sameInstance(addSuppressedError)));
    }
    verify(resource).close();
    assertThat(guard.size(), is(0));
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeEmpty_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.remove(0);
    }
  }

  @Test
  public void test_removeSingleItem_becomesEmpty() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource = guard.add(mock(AutoCloseable.class));
    assertThat(guard.size(), is(not(0)));
    assertThat(guard.remove(0), is(sameInstance(resource)));
    guard.close();
    assertThat(guard.size(), is(0));
    verify(resource, never()).close();
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeSingleItemInvalidIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      assertThat(guard.size(), is(not(0)));
      guard.remove(1);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeSmallIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      guard.remove(-1);
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void test_removeLargeIndex_indexOutOfBoundException() throws Exception {
    try (final NestedGuard guard = new NestedGuard()) {
      guard.add(mock(AutoCloseable.class));
      guard.add(mock(AutoCloseable.class));
      guard.remove(2);
    }
  }

  @Test
  public void test_removeFirstItem_removedItemNotClosedAndSizeReduced() throws Exception {
    final NestedGuard guard = new NestedGuard();
    final AutoCloseable resource1 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource2 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource3 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource4 = guard.add(mock(AutoCloseable.class));

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
    final AutoCloseable resource1 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource2 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource3 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource4 = guard.add(mock(AutoCloseable.class));

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
    final AutoCloseable resource1 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource2 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource3 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource4 = guard.add(mock(AutoCloseable.class));

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
    final AutoCloseable resource1 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource2 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource3 = guard.add(mock(AutoCloseable.class));
    final AutoCloseable resource4 = guard.add(mock(AutoCloseable.class));

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
    final AutoCloseable resource = guard.add(mock(AutoCloseable.class));
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
    final AutoCloseable resource = guard.add(mock(AutoCloseable.class));
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
    final AutoCloseable resource1 = guard1.add(mock(AutoCloseable.class));
    assertThat(guard1.get(0), is(resource1));

    final NestedGuard guard2 = new NestedGuard();
    final AutoCloseable resource2 = guard2.add(mock(AutoCloseable.class));
    assertThat(guard2.get(0), is(resource2));

    guard1.swap(guard2);

    assertThat(guard1.get(0), is(resource2));
    assertThat(guard2.get(0), is(resource1));

    guard1.close();
    verify(resource2).close();

    guard2.close();
    verify(resource1).close();
  }

}
