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

public class Guard implements AutoCloseable {

  private AutoCloseable resource;

  /**
   * Sets guarded instance of {@link AutoCloseable}. Provides no-throw guarantee. Resource which was
   * guarded before is not impacted and is forgotten.
   *
   * @param resource instance of {@link AutoCloseable} to guard. {@code null} is allowed and means
   * no resource is guarded (equivalent to {@link Guard#release()}).
   */
  public void set(final AutoCloseable resource) {
    this.resource = resource;
  }

  /**
   * Retrieves guarded instance of {@link AutoCloseable}. Provides no-throw guarantee.
   *
   * @return guarded instance of {@link AutoCloseable} or {@code null} if nothing is guarded.
   */
  public AutoCloseable get() {
    return resource;
  }

  /**
   * Resets this instance to guard nothing. Provides no-throw guarantee. Equivalent to {@link
   * Guard#set(java.lang.AutoCloseable)} with {@code null} as {@code resource}.
   *
   * @return instance of {@link AutoCloseable} which was guarded, may be {@code null}.
   */
  public AutoCloseable release() {
    final AutoCloseable tmp = resource;
    resource = null;
    return tmp;
  }

  /**
   * Swaps this instance with another instance. Provides no-throw guarantee.
   *
   * @param other another instance to swap with.
   */
  public void swap(final Guard other) {
    final AutoCloseable thisResource = this.resource;
    this.resource = other.resource;
    other.resource = thisResource;
  }

  /**
   * Closes guarded instance of {@link AutoCloseable} by invocation of its {@link
   * AutoCloseable#close()} method. Provides basic exception safety. If no exception is thrown by
   * guarded instance of {@link AutoCloseable} then this method provides no-throw guarantee. If
   * nothing ({@code null}) is guarded then does nothing. If {@link AutoCloseable#close()} method of
   * guarded instance of {@link AutoCloseable} throws exception then this guard instance remains
   * guarding instance of {@link AutoCloseable} and subsequent calls of {@link Guard#close()} work
   * the same way (calling {@link AutoCloseable#close()} method of guarded instance of {@link
   * AutoCloseable}). If guarded instance of {@link AutoCloseable} completes without throwing
   * exception then this guard instance is reset to guard nothing (i.e. {@link Guard#get()} returns
   * {@code null}), so subsequent calls of this method do nothing.
   *
   * @throws Exception if {@link AutoCloseable#close()} method of guarded instance of {@link
   * AutoCloseable} throws {@link Exception}, i.e. this method throws the same exceptions as {@link
   * AutoCloseable#close()} method of guarded instance of {@link AutoCloseable}.
   */
  @Override
  public void close() throws Exception {
    if (resource == null) {
      return;
    }
    resource.close();
    resource = null;
  }
}
