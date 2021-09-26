package org.mabrarov.exceptionsafety.OR;

public abstract class ORTransactionContext {


  public abstract boolean isBegun();


  public abstract void begin() throws Exception;


  public abstract void commit() throws Exception;


  public abstract void rollback() throws Exception;


  public abstract void returnAll() throws Exception;


}

