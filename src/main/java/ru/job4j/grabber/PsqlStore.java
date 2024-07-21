package ru.job4j.grabber;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store, AutoCloseable {

    private Connection connection;

    public PsqlStore(Properties config) {
        try {
            Class.forName(config.getProperty("driver-class-name"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password"));
        } catch (SQLException s) {
            s.printStackTrace();
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO post (name, link, text, created)\n"
                        + "VALUES (?, ?, ?, ?)\n"
                        + "ON CONFLICT(link) \n"
                        + "DO UPDATE SET\n"
                        + "  name = EXCLUDED.name,\n"
                        + "  text = EXCLUDED.text,\n"
                        + "  created = EXCLUDED.created")) {
            preparedStatement.setString(1, post.getTitle());
            preparedStatement.setString(2, post.getLink());
            preparedStatement.setString(3, post.getDescription());
            preparedStatement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            preparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM post")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(createNewPostFromDatabaseData(resultSet));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    @Override
    public Post findById(int id) {
        Post result = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM post WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result = createNewPostFromDatabaseData(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    private Post createNewPostFromDatabaseData(ResultSet resultSet) throws SQLException {
       return new Post(
               resultSet.getInt("id"),
               resultSet.getString("name"),
               resultSet.getString("link"),
               resultSet.getString("text"),
               resultSet.getTimestamp("created").toLocalDateTime()
       );
    }
}