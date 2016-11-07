package main.database.dao.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import main.database.dao.ThreadDAO;
import main.database.executor.TExecutor;
import main.models.PostModel;
import main.models.Response;
import main.models.ThreadModel;
import main.models.UserModel;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by ekkert on 19.10.16.
 */
public class ThreadDAOImpl implements ThreadDAO {
    DataSource dataSource;

    public ThreadDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int getCount() {
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(*) FROM Thread;";
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
            TExecutor.execQuery(connection, "TRUNCATE TABLE Thread;");
            TExecutor.execQuery(connection, "TRUNCATE TABLE Subscribe;");
            TExecutor.execQuery(connection, "SET FOREIGN_KEY_CHECKS = 1;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Response close(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE Thread SET isClosed = 1 WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, jsonObject.get("thread").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return new Response(new Gson().fromJson(jsonObject, Object.class));
    }

    @Override
    public Response create(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        ThreadModel threadModel;
        try {
            threadModel = new ThreadModel(jsonObject);
        } catch (Exception e) {
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "INSERT INTO Thread (forum, title, isClosed, user, date, message, slug, isDeleted)" +
                    " VALUES (?,?,?,?,?,?,?,?);";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, (String) threadModel.getForum());
                preparedStatement.setString(2, threadModel.getTitle());
                preparedStatement.setBoolean(3, threadModel.getIsClosed());
                preparedStatement.setString(4, (String) threadModel.getUser());
                preparedStatement.setString(5, threadModel.getDate());
                preparedStatement.setString(6, threadModel.getMessage());
                preparedStatement.setString(7, threadModel.getSlug());
                preparedStatement.setBoolean(8, threadModel.getIsDeleted());
                preparedStatement.execute();
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        threadModel.setId(resultSet.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.UNKNOWN_ERROR);
        }
        return new Response(threadModel);
    }

    @Override
    public Response details(Integer thread, String[] related) {
        ThreadModel threadModel;

        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT * FROM Thread WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, thread);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        threadModel = new ThreadModel(resultSet);
                    } else {
                        return new Response(Response.Codes.NOT_FOUND);
                    }
                }
                if (related != null) {
                    if (Arrays.asList(related).contains("user")) {
                        threadModel.setUser(new UserDAOImpl(dataSource)
                                .details((String) threadModel.getUser()).getResponse());
                    }
                    if (Arrays.asList(related).contains("forum")) {
                        threadModel.setForum(new ForumDAOImpl(dataSource)
                                .details((String) threadModel.getForum(), null).getResponse());
                    }
                    else {
                        return new Response(Response.Codes.INCORRECT_QUERY);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.UNKNOWN_ERROR);
        }
        return new Response(threadModel);
    }

    @Override
    public Response listPosts(Integer thread, String since, Integer limit, String sort, String order) {
        List<PostModel> array = new ArrayList<>();

        if (order == null) {
            order = "desc";
        }

        if (sort == null) {
            sort = "flat";
        }

        StringBuilder queryBuilder = new StringBuilder();

        if (Objects.equals(sort, "flat")) {
            queryBuilder.append("SELECT * FROM Post ");
            queryBuilder.append("WHERE thread = ? ");
            if (since != null) {
                queryBuilder.append("AND date >= ? ");
            }
            queryBuilder.append("ORDER BY date ");
            switch (order) {
                case "asc":
                    queryBuilder.append("ASC");
                    break;
                case "desc":
                    queryBuilder.append("DESC");
                    break;
                default:
                    return new Response(Response.Codes.INCORRECT_QUERY);
            }
            if (limit != null) {
                queryBuilder.append(" LIMIT ?");
            }
            queryBuilder.append(';');
        } else {
            queryBuilder.append("SELECT * FROM Post ");
            queryBuilder.append("WHERE thread = ? ");
            if (since != null) {
                queryBuilder.append("AND date >= ? ");
            }
            queryBuilder.append("ORDER BY root_mpath ");
            switch (order) {
                case "asc":
                    queryBuilder.append("ASC");
                    break;
                case "desc":
                    queryBuilder.append("DESC");
                    break;
                default:
                    return new Response(Response.Codes.INCORRECT_QUERY);
            }
            queryBuilder.append(", mpath ASC ");
            if (limit != null && Objects.equals(sort, "tree")) {
                queryBuilder.append("LIMIT ?");
            }
            queryBuilder.append(';');
        }

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int parameterIndex = 0;
                preparedStatement.setInt(++parameterIndex, thread);
                if (since != null) {
                    preparedStatement.setString(++parameterIndex, since);
                }
                if (Objects.equals(sort, "flat") || Objects.equals(sort, "tree")) {
                    if (limit != null) {
                        preparedStatement.setInt(++parameterIndex, limit);
                    }
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        PostModel postModel = new PostModel(resultSet);
                        if (Objects.equals(sort, "parent_tree") && limit != null && postModel.getParent() == null) {
                            if (limit > 0) {
                                --limit;
                            } else {
                                break;
                            }
                        }
                        array.add(postModel);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return new Response(array);
    }

    @Override
    public Response listUserThreads(String user, String since, Integer limit, String order) {
        List<ThreadModel> array = new ArrayList<>();

        if (order == null) {
            order = "desc";
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM Thread ");
        queryBuilder.append("WHERE user = ? ");
        if (since != null) {
            queryBuilder.append("AND date >= ? ");
        }
        queryBuilder.append("ORDER BY date ");
        switch (order) {
            case "asc":
                queryBuilder.append("ASC");
                break;
            case "desc":
                queryBuilder.append("DESC");
                break;
            default:
                return new Response(Response.Codes.INCORRECT_QUERY);
        }
        if (limit != null) {
            queryBuilder.append(" LIMIT ?");
        }
        queryBuilder.append(';');

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int parameterIndex = 0;
                preparedStatement.setString(++parameterIndex, user);
                if (since != null) {
                    preparedStatement.setString(++parameterIndex, since);
                }
                if (limit != null) {
                    preparedStatement.setInt(++parameterIndex, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        ThreadModel threadModel = new ThreadModel(resultSet);
                        array.add(threadModel);
                    }
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(array);
    }

    @Override
    public Response listForumThreads(String forum, String since, Integer limit, String order) {
        List<ThreadModel> array = new ArrayList<>();

        if (order == null) {
            order = "desc";
        }

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM Thread ");
        queryBuilder.append("WHERE forum = ? ");
        if (since != null) {
            queryBuilder.append("AND date >= ? ");
        }
        queryBuilder.append("ORDER BY date ");
        switch (order) {
            case "asc":
                queryBuilder.append("ASC");
                break;
            case "desc":
                queryBuilder.append("DESC");
                break;
            default:
                return new Response(Response.Codes.INCORRECT_QUERY);
        }
        if (limit != null) {
            queryBuilder.append(" LIMIT ?");
        }
        queryBuilder.append(';');

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int parameterIndex = 0;
                preparedStatement.setString(++parameterIndex, forum);
                if (since != null) {
                    preparedStatement.setString(++parameterIndex, since);
                }
                if (limit != null) {
                    preparedStatement.setInt(++parameterIndex, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        ThreadModel thread = new ThreadModel(resultSet);
                        array.add(thread);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(array);
    }

    @Override
    public Response remove(String jsonString) {
        JsonObject object;
        try {
            object = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE Thread SET isDeleted = 1 WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, object.get("thread").getAsInt());
                preparedStatement.execute();
            }

            String updatePostsQuery = "UPDATE Post SET isDeleted = 1 WHERE thread = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updatePostsQuery)) {
                preparedStatement.setInt(1, object.get("thread").getAsInt());
                preparedStatement.execute();
            }

            String deleteSubsQuery = "DELETE FROM Subscribe WHERE thread = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteSubsQuery)) {
                preparedStatement.setInt(1, object.get("thread").getAsInt());
                preparedStatement.execute();
            }

        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(new Gson().fromJson(object, Object.class));
    }

    @Override
    public Response restore(String jsonString) {
        JsonObject object;
        try {
            object = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            int postsCount;
            String updatePostsQuery = "UPDATE Post SET isDeleted = 0 WHERE thread = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updatePostsQuery)) {
                preparedStatement.setInt(1, object.get("thread").getAsInt());
                postsCount = preparedStatement.executeUpdate();
            }

            String query = "UPDATE Thread SET isDeleted = 0, posts = ? WHERE id = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, postsCount);
                preparedStatement.setInt(2, object.get("thread").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(new Gson().fromJson(object, Object.class));
    }

    @Override
    public Response update(String jsonString) {
        JsonObject object;
        try {
            object = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE Thread SET slug = ?, message = ? WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, object.get("slug").getAsString());
                preparedStatement.setString(2, object.get("message").getAsString());
                preparedStatement.setInt(3, object.get("thread").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return details(object.get("thread").getAsInt(), null);
    }

    @Override
    public Response vote(String jsonString) {
        JsonObject object;
        try {
            object = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        String likeQuery = "UPDATE Thread SET likes = likes + 1 WHERE id = ?";
        String dislikeQuery = "UPDATE Thread SET dislikes = dislikes + 1 WHERE id = ?";

        String query = object.get("vote").getAsInt() > 0 ? likeQuery : dislikeQuery;

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, object.get("thread").getAsInt());
                preparedStatement.execute();
            }

        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return details(object.get("thread").getAsInt(), null);
    }

    @Override
    public Response subscribe(String jsonString) {
        JsonObject object;
        try {
            object = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "INSERT IGNORE INTO Subscribe (user, thread) VALUES (?,?);";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, object.get("user").getAsString());
                preparedStatement.setInt(2, object.get("thread").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(new Gson().fromJson(object, Object.class));
    }

    @Override
    public Response unsubscribe(String jsonString) {
        JsonObject object;
        try {
            object = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "DELETE FROM Subscribe WHERE user = ? AND thread = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, object.get("user").getAsString());
                preparedStatement.setInt(2, object.get("thread").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(new Gson().fromJson(object, Object.class));
    }

    @Override
    public Response open(String jsonString) {
        JsonObject object;
        try {
            object = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE Thread SET isClosed = 0 WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, object.get("thread").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(new Gson().fromJson(object, Object.class));
    }

}
