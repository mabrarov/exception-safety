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

import java.util.ArrayList;
import java.util.List;

/**
 * {@link NestedGuard#close()} method uses recursion which nesting size is 2 * (n - 1), where {@code
 * n} is the number of guarded instances of {@link AutoCloseable}.
 */
public class NestedGuard implements AutoCloseable {

  private static class AddGuard implements AutoCloseable {

    private AutoCloseable resource;

    public void set(final AutoCloseable resource) {
      this.resource = resource;
    }

    @Override
    public void close() throws Exception {
      if (resource == null) {
        return;
      }
      final AutoCloseable tmp = resource;
      resource = null;
      tmp.close();
    }
  }

  private static class CleanGuard implements AutoCloseable {

    private ArrayList<PairGuard> resource;

    public void set(final ArrayList<PairGuard> resource) {
      this.resource = resource;
    }

    @Override
    public void close() {
      if (resource == null) {
        return;
      }
      final ArrayList<PairGuard> items = resource;
      resource = null;
      int size = items.size();
      PairGuard lastAlive = null;
      int i = 0;
      while (i < size) {
        final PairGuard item = items.get(i);
        if (item.getSecond() == null) {
          // Assuming that java.util.ArrayList#remove(int index) provides no-throw guarantee
          // if index is correct, because ArrayList#remove(int) is implemented using
          // java.lang.System#arraycopy and System#arraycopy is assumed to provide
          // no-throw guarantee like any reference copying / assignment
          items.remove(i);
          --size;
        } else {
          item.setFirst(lastAlive);
          lastAlive = item;
          ++i;
        }
      }
    }
  }

  private final AddGuard addGuard = new AddGuard();
  private final CleanGuard cleanGuard = new CleanGuard();
  private ArrayList<PairGuard> items;

  /**
   * Provides strong exception safety. If throws exception then {@code resource} is closed by
   * invocation of its {@link AutoCloseable#close()} method. If this method throws exception then it
   * is returned as suppressed exception of initial exception.
   */
  public void add(final AutoCloseable resource) {
    try (final AddGuard guard = addGuard) {
      guard.set(resource);
      if (items == null) {
        items = new ArrayList<>();
      }
      final PairGuard newItem = new PairGuard();
      if (!items.isEmpty()) {
        final PairGuard lastItem = items.get(items.size() - 1);
        newItem.setFirst(lastItem);
      }
      newItem.setSecond(resource);
      addItem(items, newItem);
      guard.set(null);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new AssertionError("Should never come here", e);
    }
  }

  /**
   * Retrieves number of guarded instances of {@link AutoCloseable}. Provides no-throw guarantee.
   *
   * @return number of of guarded instances of {@link AutoCloseable}.
   */
  public int size() {
    return items == null ? 0 : items.size();
  }

  /**
   * Sets existing item with given {@code index} to guard another instance of {@link AutoCloseable}.
   * Provides no-throw guarantee if {@code index} is correct, i.e. &gt;= 0 and &lt; {@link
   * NestedGuard#size()}, otherwise provides strong exception safety. Resource which was guarded
   * before is not impacted and is forgotten.
   *
   * @param index index of existing guarded instance of {@link AutoCloseable}. Should be &gt;= 0 and
   * &lt; {@link NestedGuard#size()}.
   * @param resource new instance of {@link AutoCloseable} to guard. {@code null} is allowed and
   * means that item with given {@code index} guards nothing (equivalent to {@link
   * NestedGuard#release(int)}). If {@code null} is given then size ({@link NestedGuard#size()})
   * remains the same, i.e. is not impacted.
   * @throws IndexOutOfBoundsException if {@code index} is &lt; 0 or &gt;= {@link
   * NestedGuard#size()}.
   */
  public void set(final int index, final AutoCloseable resource) {
    ensureItemsNotNull(index);
    items.get(index).setSecond(resource);
  }

  /**
   * Retrieves existing guarded instance of {@link AutoCloseable} with given {@code index}. Provides
   * no-throw guarantee if {@code index} is correct, i.e. &gt;= 0 and &lt; {@link
   * NestedGuard#size()}, otherwise provides strong exception safety.
   *
   * @param index index of existing guarded instance of {@link AutoCloseable}. Should be &gt;= 0 and
   * &lt; {@link NestedGuard#size()}.
   * @return existing guarded instance of {@link AutoCloseable} with given {@code index}, maybe
   * {@code null}.
   * @throws IndexOutOfBoundsException if {@code index} is &lt; 0 or &gt;= {@link
   * NestedGuard#size()}.
   */
  public AutoCloseable get(final int index) {
    ensureItemsNotNull(index);
    return items.get(index).getSecond();
  }

  /**
   * Resets this instance to guard nothing. Provides no-throw guarantee. Resource which was guarded
   * before is not impacted and is forgotten.
   */
  public void release() {
    items = null;
  }

  /**
   * Sets existing item with given {@code index} to guard nothing. Provides no-throw guarantee if
   * {@code index} is correct, i.e. &gt;= 0 and &lt; {@link NestedGuard#size()}, otherwise provides
   * strong exception safety. Resource which was guarded before is not impacted and is forgotten.
   * {@link NestedGuard#size()} remains the same, i.e. is not impacted. Equivalent to {@link
   * NestedGuard#set(int, AutoCloseable)} where {@code resource} is {@code null}.
   *
   * @param index index of existing guarded instance of {@link AutoCloseable}. Should be &gt;= 0 and
   * &lt; {@link NestedGuard#size()}.
   * @return instance of {@link AutoCloseable} which was guarded by item with given {@code index},
   * may be {@code null}.
   * @throws IndexOutOfBoundsException if {@code index} is &lt; 0 or &gt;= {@link
   * NestedGuard#size()}.
   */
  public AutoCloseable release(final int index) {
    ensureItemsNotNull(index);
    final PairGuard item = items.get(index);
    final AutoCloseable tmp = item.getSecond();
    item.setSecond(null);
    return tmp;
  }

  /**
   * Removes existing item with given {@code index} without closing it. Provides no-throw guarantee
   * if {@code index} is correct, i.e. &gt;= 0 and &lt; {@link NestedGuard#size()}, otherwise
   * provides strong exception safety. Resource which was guarded before is not impacted and is
   * forgotten.
   *
   * @param index index of existing guarded instance of {@link AutoCloseable}. Should be &gt;= 0 and
   * &lt; {@link NestedGuard#size()}.
   * @return instance of {@link AutoCloseable} which was guarded by item with given {@code index},
   * may be {@code null}.
   * @throws IndexOutOfBoundsException if {@code index} is &lt; 0 or &gt;= {@link
   * NestedGuard#size()}.
   */
  public AutoCloseable remove(final int index) {
    ensureItemsNotNull(index);
    final PairGuard removedItem = items.get(index);
    final int size = items.size();
    if (size == 1) {
      items = null;
      return removedItem.getSecond();
    }
    // If not last item, then exclude removed item from chain
    if (index < size - 1) {
      final PairGuard nextItem = items.get(index + 1);
      nextItem.setFirst(removedItem.getFirst());
    }
    // Assuming that java.util.ArrayList#remove(int index) provides no-throw guarantee
    // if index is correct
    items.remove(index);
    return removedItem.getSecond();
  }

  /**
   * Swaps this instance with another instance. Provides no-throw guarantee.
   *
   * @param other another instance to swap with.
   */
  public void swap(final NestedGuard other) {
    final ArrayList<PairGuard> thisItems = items;
    items = other.items;
    other.items = thisItems;
  }

  /**
   * Closes all guarded instances of {@link AutoCloseable} by invocation of their {@link
   * AutoCloseable#close()} method. Provides basic exception safety. If no exception is thrown by
   * guarded instances of {@link AutoCloseable} then this method provides no-throw guarantee. If
   * nothing is guarded then does nothing. If {@link AutoCloseable#close()} method of particular
   * guarded instance of {@link AutoCloseable} throws exception then this guard instance remains
   * guarding that instance of {@link AutoCloseable} and subsequent calls of {@link
   * NestedGuard#close()} work the same way (calling {@link AutoCloseable#close()} method of that
   * instance of {@link AutoCloseable}). If {@link AutoCloseable#close()} method of particular
   * guarded instance of {@link AutoCloseable} completes successfully (without throwing exception)
   * then this guard instance removes that instance of {@link AutoCloseable} from internal list of
   * guarded resources and subsequent calls of {@link NestedGuard#close()} don't impact that
   * instance of {@link AutoCloseable}. {@link NestedGuard#size()} is reduces by number of resources
   * which {@link AutoCloseable#close()} method completed successfully.<br/> Resources are closed in
   * the order opposite to order of adding of resources, i.e. in LIFO order.<br/> If {@link
   * AutoCloseable#close()} method of multiple guarded resources throws exception (exception 1 is
   * thrown, then exception 2 is thrown, then exception n is thrown) then exceptions are "nested" as
   * suppressed - refer to {@link Throwable#getSuppressed()} method. Nesting of multiple exceptions
   * is done in a different way comparing to "try-with-resources" statement.<br/> Nesting of
   * exceptions by "try-with-resources" statement:<br/>
   * <pre>
   * exception 1, {@link Throwable#getSuppressed()} returns { exception 2, exception 3, ..., exception n}
   * </pre>
   * Nesting of exceptions by this method:
   * <pre>
   * exception 1, {@link Throwable#getSuppressed()} returns { exception 2 }
   * exception 2, {@link Throwable#getSuppressed()} returns { exception 3 }
   * ...
   * exception n-1, {@link Throwable#getSuppressed()} returns { exception n }
   * </pre>
   *
   * @throws Exception if {@link AutoCloseable#close()} method of one of the guarded resources
   * throws exception, i.e. the same instance of exception is thrown by this method. If multiple
   * instances of {@link AutoCloseable} are guarded and multiple exceptions are thrown then
   * exceptions are nested in the way described above.
   */
  @Override
  public void close() throws Exception {
    // All operations except org.mabrarov.exceptionsafety.PairGuard#close are assumed to provide
    // no-throw guarantee
    if (items == null) {
      return;
    }
    if (items.isEmpty()) {
      items = null;
      return;
    }
    try (final CleanGuard guard = cleanGuard) {
      guard.set(items);
      final PairGuard lastItem = items.get(items.size() - 1);
      lastItem.close();
      guard.set(null);
      items = null;
    }
  }

  /**
   * Marked as "protected" for testing purposes only. Should provide strong exception safety.
   *
   * @param items list to add new item to the end.
   * @param item new item to be added at the end of given {@code items} list.
   */
  protected void addItem(final List<PairGuard> items, final PairGuard item) {
    items.add(item);
  }

  private void ensureItemsNotNull(final int index) {
    if (items == null) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }
  }

}
