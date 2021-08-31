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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

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
import org.mockito.ArgumentMatchers;

public class FactoryMethodTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final AtomicBoolean resourceClosed = new AtomicBoolean();
  private ResourceCreator resourceCreator;
  private ResourceConfigurator resourceConfigurator;

  private OutputStream createConfiguredResource()
      throws IOException, TestResourceConfigurationException {
    OutputStream resource = createResource();
    Throwable throwable = null;
    try {
      configureResource(resource);
      return resource;
    } catch (final TestResourceConfigurationException | RuntimeException e) {
      throwable = e;
      throw e;
    }
    catch(final Error e) {
      throwable = e;
      throw e;
    } finally {
      if (throwable != null) {
        try {
          resource.close();
        } catch (Throwable e) {
          throwable.addSuppressed(e);
        }
      }
    }
  }

  @Test
  public void test_noExceptions() {
    resourceConfigurator = resourceConfiguratorNoException;
    resourceCreator = resourceCreatorCloseNoException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationCheckedException() {
    resourceConfigurator = resourceConfiguratorCheckedException;
    resourceCreator = resourceCreatorCloseNoException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationRuntimeException() {
    resourceConfigurator = resourceConfiguratorRuntimeException;
    resourceCreator = resourceCreatorCloseNoException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_closeCheckedException() {
    resourceConfigurator = resourceConfiguratorNoException;
    resourceCreator = resourceCreatorCloseCheckedException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_closeRuntimeException() {
    resourceConfigurator = resourceConfiguratorNoException;
    resourceCreator = resourceCreatorCloseRuntimeException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationCheckedException_closeCheckedException() {
    resourceConfigurator = resourceConfiguratorCheckedException;
    resourceCreator = resourceCreatorCloseCheckedException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationCheckedException_closeRuntimeException() {
    resourceConfigurator = resourceConfiguratorCheckedException;
    resourceCreator = resourceCreatorCloseRuntimeException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationRuntimeException_closeCheckedException() {
    resourceConfigurator = resourceConfiguratorRuntimeException;
    resourceCreator = resourceCreatorCloseCheckedException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_configurationRuntimeException_closeRuntimeException() {
    resourceConfigurator = resourceConfiguratorRuntimeException;
    resourceCreator = resourceCreatorCloseRuntimeException;
    verifyResourceCreationAndClosing();
  }

  @Test
  public void test_exceptionSuppressionError() {
    resourceConfigurator = resourceConfiguratorRuntimeExceptionWithSuppressionFailure;
    resourceCreator = resourceCreatorCloseRuntimeException;
    verifyResourceCreationAndClosing();
  }

  private void verifyResourceCreationAndClosing() {
    try (final OutputStream resource = createConfiguredResource();
         final PrintStream printStream = new PrintStream(resource)) {
      Assert.assertFalse("Resource should be opened", resourceClosed.get());
      printStream.println("Test");
    } catch (final Exception | TestSuppressionError e) {
      e.printStackTrace();
    }
    Assert.assertTrue("Resource should be closed", resourceClosed.get());
  }

  private interface ResourceCreator {

    OutputStream createResource(final File file) throws IOException;

  }

  private interface ResourceConfigurator {

    void configureResource(@SuppressWarnings("unused") final OutputStream resource)
        throws TestResourceConfigurationException;

  }

  private final ResourceCreator resourceCreatorCloseNoException = new ResourceCreator() {
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

  private final ResourceCreator resourceCreatorCloseCheckedException = new ResourceCreator() {
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

  private final ResourceCreator resourceCreatorCloseRuntimeException = new ResourceCreator() {
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

  private final ResourceConfigurator resourceConfiguratorRuntimeExceptionWithSuppressionFailure = new ResourceConfigurator() {
    @Override
    public void configureResource(final OutputStream resource) {
      final TestRuntimeException configurationException = spy(new TestRuntimeException());
      doThrow(new TestSuppressionError()).when(configurationException)
          .addSuppressed(ArgumentMatchers.<Throwable>any());
      throw configurationException;
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
