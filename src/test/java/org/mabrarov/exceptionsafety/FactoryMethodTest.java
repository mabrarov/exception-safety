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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FactoryMethodTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final AtomicBoolean resourceClosed = new AtomicBoolean();
  private ResourceCreator resourceCreator;
  private ResourceConfigurator resourceConfigurator;

  private OutputStream createConfiguredResource()
      throws IOException, TestResourceConfigurationException {
    final OutputStream resource = createResource();
    try {
      configureResource(resource);
    } catch (final TestResourceConfigurationException e) {
      try (@SuppressWarnings("unused") final OutputStream closer = resource) {
        throw e;
      }
    }
    return resource;
  }

  @Test
  public void test_noExceptions() {
    resourceCreator = resourceCreatorNoException;
    resourceConfigurator = resourceConfiguratorNoException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationCheckedException() {
    resourceCreator = resourceCreatorNoException;
    resourceConfigurator = resourceConfiguratorCheckedException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationRuntimeException() {
    resourceCreator = resourceCreatorNoException;
    resourceConfigurator = resourceConfiguratorRuntimeException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_closeCheckedException() {
    resourceCreator = resourceCreatorCheckedException;
    resourceConfigurator = resourceConfiguratorNoException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_closeRuntimeException() {
    resourceCreator = resourceCreatorRuntimeException;
    resourceConfigurator = resourceConfiguratorNoException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationCheckedException_closeCheckedException() {
    resourceCreator = resourceCreatorCheckedException;
    resourceConfigurator = resourceConfiguratorCheckedException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationCheckedException_closeRuntimeException() {
    resourceCreator = resourceCreatorRuntimeException;
    resourceConfigurator = resourceConfiguratorCheckedException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationRuntimeException_closeCheckedException() {
    resourceCreator = resourceCreatorRuntimeException;
    resourceConfigurator = resourceConfiguratorCheckedException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationRuntimeException_closeRuntimeException() {
    resourceCreator = resourceCreatorRuntimeException;
    resourceConfigurator = resourceConfiguratorCheckedException;
    verifyResourceCreationAndClosing();
  }

  private void verifyResourceCreationAndClosing() {
    try (final OutputStream resource = createConfiguredResource();
        final PrintStream printStream = new PrintStream(resource)) {
      Assert.assertFalse("Resource should be opened", resourceClosed.get());
      printStream.println("Test");
    } catch (final Exception e) {
      e.printStackTrace();
    }
    Assert.assertTrue("Resource should be closed", resourceClosed.get());
  }

  private interface ResourceCreator {

    OutputStream createResource(final File file) throws IOException;

  }

  private interface ResourceConfigurator {

    void configureResource(final OutputStream resource) throws TestResourceConfigurationException;

  }

  private final ResourceCreator resourceCreatorNoException = new ResourceCreator() {
    @Override
    public OutputStream createResource(final File file) throws IOException {
      return new FileOutputStream(file) {
        @Override
        public void close() throws IOException {
          super.close();
          resourceClosed.set(true);
        }
      };
    }
  };

  private final ResourceCreator resourceCreatorCheckedException = new ResourceCreator() {
    @Override
    public OutputStream createResource(final File file) throws IOException {
      return new FileOutputStream(file) {
        @Override
        public void close() throws IOException {
          super.close();
          resourceClosed.set(true);
          throw new TestResourceCloseException();
        }
      };
    }
  };

  private final ResourceCreator resourceCreatorRuntimeException = new ResourceCreator() {
    @Override
    public OutputStream createResource(final File file) throws IOException {
      return new FileOutputStream(file) {
        @Override
        public void close() throws IOException {
          super.close();
          resourceClosed.set(true);
          throw new TestResourceCloseRuntimeException();
        }
      };
    }
  };

  private final ResourceConfigurator resourceConfiguratorNoException = new ResourceConfigurator() {
    @Override
    public void configureResource(final OutputStream resource) {
    }
  };

  private final ResourceConfigurator resourceConfiguratorCheckedException = new ResourceConfigurator() {
    @Override
    public void configureResource(final OutputStream resource)
        throws TestResourceConfigurationException {
      throw new TestResourceConfigurationException();
    }
  };

  private final ResourceConfigurator resourceConfiguratorRuntimeException = new ResourceConfigurator() {
    @Override
    public void configureResource(final OutputStream resource) {
      throw new TestRuntimeException();
    }
  };

  private OutputStream createResource() throws IOException {
    return resourceCreator.createResource(temporaryFolder.newFile());
  }

  private void configureResource(final OutputStream resource)
      throws TestResourceConfigurationException {
    resourceConfigurator.configureResource(resource);
  }

}
