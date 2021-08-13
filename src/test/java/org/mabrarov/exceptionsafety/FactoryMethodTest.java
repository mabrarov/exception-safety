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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;

public class FactoryMethodTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final TestResourceConfigurationException resourceConfigurationException = spy(
      new TestResourceConfigurationException());
  private final IOException closeException = new TestResourceCloseException();
  private final AtomicBoolean resourceClosed = new AtomicBoolean();

  @Before
  public void setUp() {
    doThrow(new TestSuppressionError()).when(resourceConfigurationException)
        .addSuppressed(ArgumentMatchers.<Throwable>any());
  }

  @Test
  public void test_factoryMethod() {
    try (final OutputStream resource = createConfiguredResource();
        final PrintStream printStream = new PrintStream(resource)) {
      Assert.assertFalse("Resource should be opened", resourceClosed.get());
      printStream.println("Test");
    } catch (final Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue("Resource should be closed", resourceClosed.get());
  }

  private OutputStream createConfiguredResource()
      throws IOException, TestResourceConfigurationException {
    try (final Guard guard = new Guard()) {
      final OutputStream resource = guard.set(createResource());
      configureResource(resource);
      guard.release();
      return resource;
    } catch (final IOException | TestResourceConfigurationException | RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new AssertionError("Should never come here", e);
    }
  }

  private OutputStream createResource() throws IOException {
    final File file = temporaryFolder.newFile();
    return new FileOutputStream(file) {
      @Override
      public void close() throws IOException {
        super.close();
        resourceClosed.set(true);
        throw closeException;
      }
    };
  }

  private void configureResource(@SuppressWarnings("unused") final OutputStream resource)
      throws TestResourceConfigurationException {
    if (Math.random() > 0.5) {
      throw resourceConfigurationException;
    }
  }

}
