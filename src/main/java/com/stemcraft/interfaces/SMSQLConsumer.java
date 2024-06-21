package com.stemcraft.interfaces;

import java.sql.SQLException;

@FunctionalInterface
public interface SMSQLConsumer {
    void accept() throws SQLException;
}
