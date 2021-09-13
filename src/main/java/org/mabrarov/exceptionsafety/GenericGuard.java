/*
 * Copyright (c) 2021 Marat Abrarov (abrarov@gmail.com)
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

public class GenericGuard<T> implements AutoCloseable {

  private CloseFunction<? super T> closeFunction;
  private T resource;

  public GenericGuard(final CloseFunction<? super T> closeFunction) {
    this.closeFunction = closeFunction;
  }

  /**
   * Sets guarded instance of {@code T}. Provides no-throw guarantee. Resource which was guarded
   * before is not impacted and is forgotten.
   *
   * @param resource instance of {@code T} to guard. {@code null} is allowed and means no resource
   * is guarded (equivalent to {@link GenericGuard#release()}).
   * @return instance of {@code T} passed as {@code resource} parameter.
   */
  public <R extends T> T set(final R resource) {
    this.resource = resource;
    return resource;
  }

  /**
   * Retrieves guarded instance of {@code T}. Provides no-throw guarantee.
   *
   * @return guarded instance of {@code T} or {@code null} if nothing is guarded.
   */
  public T get() {
    return resource;
  }

  /**
   * Resets this instance to guard nothing. Provides no-throw guarantee. Equivalent to {@link
   * GenericGuard#set} with {@code null} as {@code resource}.
   *
   * @return instance of {@code T} which was guarded, may be {@code null}.
   */
  public T release() {
    final T tmp = resource;
    resource = null;
    return tmp;
  }

  /**
   * Swaps this instance with another instance. Provides no-throw guarantee.
   *
   * @param other another instance to swap with.
   */
  public void swap(final GenericGuard<T> other) {
    final CloseFunction<? super T> theCloseFunction = this.closeFunction;
    this.closeFunction = other.closeFunction;
    other.closeFunction = theCloseFunction;
    final T thisResource = this.resource;
    this.resource = other.resource;
    other.resource = thisResource;
  }

  /**
   * Closes guarded instance of {@code T} by invocation of {@link CloseFunction#apply} method and
   * passing guarded instance into this method. Provides basic exception safety. If no exception is
   * thrown by {@link CloseFunction#apply} then this method provides no-throw guarantee. If nothing
   * ({@code null}) is guarded then does nothing. If {@link CloseFunction#apply} method for the
   * guarded instance throws exception then this guard instance remains guarding the same instance
   * of {@code T} and subsequent calls of {@code GenericGuard#close()} method work the same way
   * (calling {@link CloseFunction#apply} method and passing guarded instance of {@code T} into that
   * method). If closing of guarded instance completes without throwing exception then this guard
   * instance is reset to guard nothing (i.e. {@link GenericGuard#get()} returns {@code null}), so
   * subsequent calls of this method do nothing.
   *
   * @throws Exception if {@link CloseFunction#apply} method for the guarded instance of {@code T}
   * throws {@link Exception}, i.e. this method throws the same exceptions as {@link
   * CloseFunction#apply} method.
   */
  @Override
  public void close() throws Exception {
    if (resource == null) {
      return;
    }
    closeFunction.apply(resource);
    resource = null;
  }
}
