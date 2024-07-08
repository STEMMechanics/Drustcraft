package com.stemcraft.migrations;

import com.stemcraft.SMBook;
import com.stemcraft.SMDatabase;
import com.stemcraft.SMDatabaseMigration;
import com.stemcraft.STEMCraft;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

public class _240703_MigrateBooksTable extends SMDatabaseMigration {

    /**
     * Perform the database migration
     */
    @Override
    public void migrate() {
        try {
            /* Copy DB data and save to Books configuration */
            PreparedStatement statement = SMDatabase.prepareStatement(
                    "SELECT * FROM books");

            assert statement != null;
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                SMBook book = SMBook.create(resultSet.getString("name"));
                book.setAuthor(resultSet.getString("author"));
                book.setTitle(resultSet.getString("title"));

                List<String> contents = Arrays.stream(resultSet.getString("content").split("<n>")).toList();
                book.setContent(contents);
                book.save();
            }

            resultSet.close();
            statement.close();

            /* Drops the books table */
            String sql = "DROP TABLE books";
            statement = SMDatabase.prepareStatement("DROP TABLE books");
            assert statement != null;
            statement.execute();
            statement.close();
        } catch (Exception e) {
            STEMCraft.error(e);
        }
    }
}
