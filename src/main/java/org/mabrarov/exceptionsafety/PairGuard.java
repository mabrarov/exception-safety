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

public class PairGuard implements AutoCloseable {

  private final Guard firstGuard = new Guard();
  private AutoCloseable second;

  /**
   * Provides no-throw guarantee.
   *
   * @param first instance of {@link AutoCloseable} to guard. {@code null} is allowed and means no
   * resource is guarded (equivalent to {@link PairGuard#releaseFirst()}).
   * @return instance of {@link AutoCloseable} passed as {@code first} parameter.
   */
  public <T extends AutoCloseable> T setFirst(final T first) {
    this.firstGuard.set(first);
    return first;
  }

  /**
   * Provides no-throw guarantee.
   *
   * @param second instance of {@link AutoCloseable} to guard. {@code null} is allowed and means no
   * resource is guarded (equivalent to {@link PairGuard#releaseSecond()}).
   * @return instance of {@link AutoCloseable} passed as {@code second} parameter.
   */
  public <T extends AutoCloseable> T setSecond(final T second) {
    this.second = second;
    return second;
  }

  /**
   * Provides no-throw guarantee.
   */
  public AutoCloseable getFirst() {
    return firstGuard.get();
  }

  /**
   * Provides no-throw guarantee.
   */
  public AutoCloseable getSecond() {
    return second;
  }

  /**
   * Provides no-throw guarantee.
   */
  public AutoCloseable releaseFirst() {
    return firstGuard.release();
  }

  /**
   * Provides no-throw guarantee.
   */
  public AutoCloseable releaseSecond() {
    AutoCloseable tmp = second;
    second = null;
    return tmp;
  }

  /**
   * Provides no-throw guarantee.
   */
  public void release() {
    firstGuard.release();
    second = null;
  }

  /**
   * Swaps this instance with another instance. Provides no-throw guarantee.
   *
   * @param other another instance to swap with
   */
  public void swap(final PairGuard other) {
    this.firstGuard.swap(other.firstGuard);
    final AutoCloseable thisSecond = this.second;
    this.second = other.second;
    other.second = thisSecond;
  }

  @Override
  public void close() throws Exception {
    try (@SuppressWarnings("unused") final AutoCloseable guard = firstGuard) {
      if (second != null) {
        second.close();
        second = null;
      }
    }
  }
}
