package main.database.dao.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import main.database.dao.PostDAO;
import main.database.executor.TExecutor;
import main.models.ForumModel;
import main.models.PostModel;
import main.models.Response;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Arrays;

/**
 * Created by ekkert on 18.10.16.
 */
public class PostDAOImpl implements PostDAO {

    private static final int MYSQL_DUPLICATE_PK = 1062;

    private final DataSource dataSource;

    public PostDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int getCount() {
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(*) FROM Post;";
            return TExecutor.execQuery(connection, query, resultSet -> {
                resultSet.next();
                return resultSet.getInt(1);
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void truncateTable() {
        try (Connection connection = dataSource.getConnection()) {
            TExecutor.execQuery(connection, "SET FOREIGN_KEY_CHECKS = 0;");
            TExecutor.execQuery(connection, "TRUNCATE TABLE Post;");
            TExecutor.execQuery(connection, "SET FOREIGN_KEY_CHECKS = 1;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response create(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        PostModel postModel;
        try {
            postModel = new PostModel(jsonObject);
        } catch (Exception e) {
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "INSERT INTO Post (parent, isApproved, isHighlighted, isEdited, isSpam," +
                    "isDeleted, date, thread, message, user, forum) VALUES (?,?,?,?,?,?,?,?,?,?,?);";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setInt(1, postModel.getParent());
                preparedStatement.setBoolean(2, postModel.isApproved());
                preparedStatement.setBoolean(3, postModel.isHighlighted());
                preparedStatement.setBoolean(4, postModel.isEdited());
                preparedStatement.setBoolean(5, postModel.isSpam());
                preparedStatement.setBoolean(6, postModel.isDeleted());
                preparedStatement.setString(7, postModel.getDate());
                preparedStatement.setString(8, (String) postModel.getThread());
                preparedStatement.setString(9, postModel.getMessage());
                preparedStatement.setString(10, (String) postModel.getUser());
                preparedStatement.setString(11, (String) postModel.getForum());
                preparedStatement.execute();
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        postModel.setId(resultSet.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
                return new Response(Response.Codes.UNKNOWN_ERROR);
        }
        return new Response(postModel);
    }


    @Override
    public Response details(Integer post, String[] related) {
        PostModel postModel;

        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT * FROM Post WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, post);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        postModel = new PostModel(resultSet);
                    } else {
                        return new Response(Response.Codes.NOT_FOUND);
                    }
                }
            }
            if (related != null) {
                if (Arrays.asList(related).contains("user")) {
                    postModel.setUser(new UserDAOImpl(dataSource).details((String) postModel.getUser()).getResponse());
                }
                if (Arrays.asList(related).contains("forum")) {
                    postModel.setUser(new ForumDAOImpl(dataSource).details((String) postModel.getForum(), null).getResponse());
                }
                if (Arrays.asList(related).contains("user")) {
                    postModel.setUser(new ThreadDAOImpl(dataSource).details((Integer) postModel.getThread(), null).getResponse());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.UNKNOWN_ERROR);
        }

        return new Response(postModel);
    }

    @Override
    public Response listThreadPosts(Integer thread, String since, Integer limit, String order) {
        return new ThreadDAOImpl(dataSource).listPosts(thread, since, limit, order, null);
    }

    @Override
    public Response listForumPosts(String forum, String since, Integer limit, String order) {
        return new ForumDAOImpl(dataSource).listPosts(forum, since, limit, order, null);
    }

    @Override
    public Response remove(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE Post SET isDeleted = 1 WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, jsonObject.get("post").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return new Response(new Gson().fromJson(jsonObject, Object.class));
    }

    @Override
    public Response restore(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE Post SET isDeleted = 0 WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, jsonObject.get("post").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return new Response(new Gson().fromJson(jsonObject, Object.class));
    }

    @Override
    public Response update(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE Post SET message = ? WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, jsonObject.get("message").getAsString());
                preparedStatement.setInt(2, jsonObject.get("post").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return details(jsonObject.get("post").getAsInt(), null);
    }

    @Override
    public Response vote(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String likeQuery = "UPDATE Post SET likes = likes + 1 WHERE id = ?;";
            String dislikeQuery = "UPDATE Post SET dislikes = dislikes + 1 WHERE id = ?;";
            String query;
            if (jsonObject.get("vote").getAsInt() > 0) {
                query = likeQuery;
            } else  {
                query = dislikeQuery;
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, jsonObject.get("post").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return details(jsonObject.get("post").getAsInt(), null);
    }
}
