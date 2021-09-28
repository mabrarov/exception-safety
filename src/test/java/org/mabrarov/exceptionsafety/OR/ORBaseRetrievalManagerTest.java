package org.mabrarov.exceptionsafety.OR;

import org.junit.Assert;
import org.junit.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ORBaseRetrievalManagerTest {

  private ORTransactionContext context;
  private final ORBaseRetrievalManager manager = new ORBaseRetrievalManager() {
    @Override
    protected Object retrieveExecute(Map<String, Object> parameters, ORTransactionContext context) throws SQLException {
      /*foo case for exception*/
      if(parameters.isEmpty()) {
        throw new RetrieveExecuteException("No parameters");
      }
      return null;
    }
  };

  private void retrieve() throws SQLException {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("string", "string");
    manager.retrieve(map, context);
  }
  private void retrieveWithBadParameters() throws SQLException {
    Map<String, Object> map = new HashMap<String, Object>();
    manager.retrieve(map, context);
  }

  private void retrieveWithoutCommit() throws SQLException {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("string", "string");
    manager.retrieveWithoutCommit(map, context);
  }
  private void retrieveWithoutCommitWithBadParameters() throws SQLException {
    Map<String, Object> map = new HashMap<String, Object>();
    manager.retrieveWithoutCommit(map, context);
  }

  /* case without exception and transaction had been opened before method*/
  @Test
  public void test_no_exceptions_without_opening_transaction_retrieve() throws SQLException {
    context = transactionContextNoExceptionTransactionIsOpen;
    retrieve();
  }

  /* case without exception and transaction had not been opened before method*/
  @Test
  public void test_no_exceptions_with_opening_transaction_retrieve() throws SQLException {
    context = transactionContextNoExceptionTransactionIsClose;
    retrieve();
  }

  /*If the commit throws an exception, the transaction should rollback.
   Otherwise, it leads to the opened transaction that could block tables/rows,
   take memory for transactions in db.*/
  @Test
  public void test_exception_in_commit_retrieve() {
    context = transactionContextExceptionInCommitTransactionIsClose;
    try {
      retrieve();
    } catch (Throwable e) {
      Assert.assertFalse("Transaction is opened", context.isBegun());
    }
  }

  /*startTrans becomes true before the transaction opens.
  If the opening of a transaction throws an exception, the not existing transaction is tried to roll back,
  which throws a new exception that suppressed the origin exception.
  So, rollback is called when it can not work correctly, that is spent system resources /db resources. */
  @Test
  public void test_exception_in_begin_of_transaction_retrieve() {
    context = transactionContextExceptionInBeginTransactionIsClose;
    try {
      retrieve();
    } catch (Throwable e) {
      //First Assert is not needed, if test_exception_in_try_blocke_is_supprested_by_finally_blocke_retrieve is passed
      Assert.assertTrue(e instanceof BeginException);
      for (Throwable throwable : e.getSuppressed()) {
        Assert.assertFalse(throwable instanceof RollBackException);
      }
    }
  }

  /*Any exception originally thrown in the try block is suppressed by SQLException()in the finally block,
  if transaction had not been opened before and could not roll back.*/
  @Test
  public void test_exception_in_try_blocke_is_supprested_by_finally_blocke_retrieve() {
    context = transactionContextExceptionInRollBackTransactionIsClose;
    try {
      retrieveWithBadParameters();
    } catch (Throwable e) {
      Assert.assertTrue(e instanceof RetrieveExecuteException);
    }
  }

  /*using two place for same logs e.printStackTrace() and _log.error("Got an error " + e.getErrorCode()).
  Maybe, that is not a mistake*/
//  @Test
//  public void test_same_logs_in_two_different_place_retrieve() throws Exception {
//    /* no test, look at logs nad console to see it */
//    context = transactionContextExceptionInBeginTransactionIsClose;
//    retrieve();
//    if(true){
//      throw new Exception();
//    }
//  }

  /*Exception originally thrown in the try block by retrieveExecute(parameters, context) is  suppressed
  by  SQLException("Error rolling back transaction: " + e.getMessage())in the finally block, if rolling back is fail.
  So, the original exception was lost, the reason for occurring exception is harder to find.*/
  @Test
  public void test_exception_in_try_blocke_is_supprested_by_finally_blocke_retrieveWithoutCommit() {
    context = transactionContextExceptionInRollBackTransactionIsClose;
    try {
      retrieveWithoutCommitWithBadParameters();
    } catch (Throwable e) {
      Assert.assertTrue("Other Exception" + e.getMessage(),e instanceof RetrieveExecuteException);
    }
  }

  /*The SQLException is thrown by context.begin()  is put in log twice.
  Duplication takes memory for logs, reading of logs becomes harder.*/
//  @Test
//  public void test_duplication_of_logs() throws Exception {
//    /* no test, look at logs to see it */
//    context = transactionContextExceptionInBeginTransactionIsClose;
//    retrieveWithoutCommit();
//    if(true){
//      throw new Exception();
//    }
//  }

  private class ORTransactionContextEx extends ORTransactionContext {
    boolean isBegun;

    ORTransactionContextEx(boolean isBegun) {
      this.isBegun = isBegun;

    }

    @Override
    public boolean isBegun() {
      return isBegun;
    }

    @Override
    public void begin() throws Exception {
      isBegun = true;

    }

    @Override
    public void commit() throws Exception {
      if (isBegun) {
        isBegun = false;
      } else {
        throw new SQLException("No Transaction");
      }
    }

    @Override
    public void rollback() throws Exception {
      if (isBegun) {
        isBegun = false;
      } else {
        throw new RollBackException("No Transaction");
      }
    }

    @Override
    public void returnAll() throws Exception {
    }
  }

  private final ORTransactionContext transactionContextNoExceptionTransactionIsOpen =
      new ORTransactionContextEx(true);

  private final ORTransactionContext transactionContextNoExceptionTransactionIsClose =
      new ORTransactionContextEx(false);

  private final ORTransactionContext transactionContextExceptionInCommitTransactionIsClose =
      new ORTransactionContextEx(false) {
        @Override
        public void commit() throws Exception {
          if (true) {
            throw new SQLException("Exception in commit");
          }
          if (isBegun) {
            isBegun = false;
          } else {
            throw new SQLException("No Transaction");
          }
        }
      };

  private final ORTransactionContext transactionContextExceptionInBeginTransactionIsClose =
      new ORTransactionContextEx(false) {
        @Override
        public void begin() throws Exception {
          if (true) {
            throw new BeginException("Exception in begin");
          }
          isBegun = true;
        }
      };

  private final ORTransactionContext transactionContextExceptionInRollBackTransactionIsClose =
      new ORTransactionContextEx(false) {
        @Override
        public void rollback() throws Exception {
          if (true) {
            throw new RollBackException("Exception in roll back");
          }
          if (isBegun) {
            isBegun = false;
          } else {
            throw new RollBackException("No Transaction");
          }
        }
      };


  private class RollBackException extends SQLException {
    private RollBackException(String message) {
      super(message);
    }
  }

  private class BeginException extends SQLException {
    private BeginException(String message) {
      super(message);
    }
  }

  private class RetrieveExecuteException extends SQLException {
    private RetrieveExecuteException(String message) {
      super(message);
    }
  }

}
