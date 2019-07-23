package org.mabrarov.exceptionsafety;

import java.util.ArrayList;
import java.util.List;

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
          // Assuming that java.util.ArrayList#remove(int) never throws exceptions
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

  public int size() {
    return items == null ? 0 : items.size();
  }

  public void set(final int index, final AutoCloseable resource) {
    if (items == null) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }
    items.get(index).setSecond(resource);
  }

  public AutoCloseable get(final int index) {
    if (items == null) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: 0");
    }
    return items.get(index).getSecond();
  }

  public void release() {
    items = null;
  }

  public void release(final int index) {
    set(index, null);
  }

  public void swap(final NestedGuard other) {
    final ArrayList<PairGuard> thisItems = items;
    items = other.items;
    other.items = thisItems;
  }

  @Override
  public void close() throws Exception {
    // All operations except java.lang.AutoCloseable#close are assumed to never throw exceptions
    if (items == null || items.isEmpty()) {
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
   * Marked as "protected" for testing purposes only
   */
  protected void addItem(final List<PairGuard> items, final PairGuard item) {
    items.add(item);
  }

}
