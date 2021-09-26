package org.mabrarov.exceptionsafety.OR;

import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;

public abstract class ORBaseRetrievalManager {
  protected Logger _log = Logger.getLogger(ORBaseRetrievalManager.class);

  public Object retrieve(final Map<String, Object> parameters, final ORTransactionContext context)
      throws SQLException {
    boolean startTrans = false;
    boolean error = false;
    Object obj = null;

    try {
      if (!context.isBegun()) {
        startTrans = true;
        try {
          context.begin();
        } catch (final Exception e) {
          e.printStackTrace();
          throw new SQLException("Error starting transaction: " + e.getMessage());
        }
      }
      obj = retrieveExecute(parameters, context);
    } catch (final SQLException e) {
      error = true;
      _log.error("Got an error " + e.getErrorCode());
      throw e;
    } finally {
      if (startTrans) {
        if (error) {
          try {
            context.rollback();
          } catch (final Exception e) {
            throw new SQLException("Error rolling back transaction: " + e.getMessage());
          }
        } else {
          try {
            context.commit();
          } catch (final Exception e) {
            throw new SQLException("Error committing transaction: " + e.getMessage());
          }
        }
      }
    }
    return obj;
  }


  public Object retrieveWithoutCommit(final Map<String, Object> parameters,
                                      final ORTransactionContext context) throws SQLException {
    boolean startTrans = false;
    boolean error = false;
    Object obj = null;
    try {
      if (!context.isBegun()) {
        try {
          context.begin();
          startTrans = true;
        } catch (final Exception e) {
          _log.error("Error starting transaction", e);
          throw new SQLException("Error starting transaction: " + e.getMessage());
        }
      }
      obj = retrieveExecute(parameters, context);
    } catch (final SQLException e) {
      error = true;
      _log.error("Got an error " + e.getErrorCode());
      throw e;
    } finally {
      if (startTrans && error) {
        try {
          context.rollback();
        } catch (final Exception e) {
          throw new SQLException("Error rolling back transaction: " + e.getMessage(), e);
        }
      }
    }
    return obj;
  }

  protected abstract Object retrieveExecute(Map<String, Object> parameters,
                                            ORTransactionContext context) throws SQLException;


}