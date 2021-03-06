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

public class TestError extends Error {

  private final int id;

  public TestError() {
    this(0);
  }

  public TestError(final int id) {
    super("Test exception");
    this.id = id;
  }

  public int getId() {
    return id;
  }

  @Override
  public String toString() {
    return "TestError{" + "id=" + id + '}';
  }
}
