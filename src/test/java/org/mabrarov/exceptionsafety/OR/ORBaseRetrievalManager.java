package org.mabrarov.exceptionsafety.OR;

import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;

public abstract class ORBaseRetrievalManager {
  protected Logger _log = Logger.getLogger(ORBaseRetrievalManager.class);

  public Object retrieve(final Map<String, Object> parameters, final ORTransactionContext context)
      throws SQLException {
    if (!context.isBegun()) {
      return retrieveWhereTransactionIsClose(parameters, context);
    }
    return retrieveWhereTransactionIsOpen(parameters, context);
  }


  private void rollbackAfterException(final ORTransactionContext context, Throwable e) throws SQLException {
    try {
      context.rollback();
    } catch (final Throwable ex) {
      e.addSuppressed(ex);
    }
  }

  private void commit(final ORTransactionContext context) throws SQLException {
    try {
      context.commit();
    } catch (final Throwable ex) {
      rollbackAfterException(context, ex);
      throwSQLExceptionOrRuntimeOrError(ex);
    }
  }

  private void throwSQLExceptionOrRuntimeOrError(Throwable e) throws SQLException {
    if (e == null) {
      return;
    }
    if (e instanceof SQLException) {
      throw (SQLException) e;
    }
    if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    }
    throw (Error) e;
  }

  private Object retrieveWhereTransactionIsOpen(final Map<String, Object> parameters,
                                                final ORTransactionContext context) throws SQLException {
    try {
      return retrieveExecute(parameters, context);
    } catch (Throwable e) {
      _log.error(e);
      throw e;
    }
  }

  private Object retrieveWhereTransactionIsClose(final Map<String, Object> parameters,
                                                 final ORTransactionContext context) throws SQLException {
    Object o;
    try {
      context.begin();
    } catch (Throwable e) {
      _log.error(e);
      throwSQLExceptionOrRuntimeOrError(e);
    }
    try {
       o = retrieveExecute(parameters, context);
       commit(context);
       return o;
    } catch (Throwable e) {
      _log.error(e);
      rollbackAfterException(context, e);
      throw e;
    }
  }

  private Object retrieveWhereTransactionIsCloseWithoutCommit(final Map<String, Object> parameters,
                                                              final ORTransactionContext context) throws SQLException {
    try {
      context.begin();
    } catch (Throwable e) {
      _log.error(e);
      throwSQLExceptionOrRuntimeOrError(e);
    }
    try {
      return retrieveExecute(parameters, context);
    } catch (Throwable e) {
      _log.error(e);
      rollbackAfterException(context, e);
      throw e;
    }
  }

  public Object retrieveWithoutCommit(final Map<String, Object> parameters,
                                      final ORTransactionContext context) throws SQLException {
    if (!context.isBegun()) {
      return retrieveWhereTransactionIsCloseWithoutCommit(parameters, context);
    }
    return retrieveWhereTransactionIsOpen(parameters, context);
  }

  protected abstract Object retrieveExecute(Map<String, Object> parameters,
                                            ORTransactionContext context) throws SQLException;
}
