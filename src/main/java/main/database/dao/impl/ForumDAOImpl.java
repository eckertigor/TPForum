package main.database.dao.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import main.database.dao.ForumDAO;
import main.database.executor.TExecutor;
import main.models.*;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ekkert on 16.10.16.
 */
public class ForumDAOImpl implements ForumDAO {

    private static final int MYSQL_DUPLICATE_PK = 1062;

    private final DataSource dataSource;

    public ForumDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int getCount() {
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(*) FROM Forum;";
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
            TExecutor.execQuery(connection, "TRUNCATE TABLE Forum;");
            TExecutor.execQuery(connection, "TRUNCATE TABLE forum_user;");
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

        ForumModel forumModel;
        try {
            forumModel = new ForumModel(jsonObject);
        } catch (Exception e) {
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "INSERT INTO Forum (name, short_name, user) VALUES (?,?,?);";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, forumModel.getName());
                preparedStatement.setString(2, forumModel.getShort_name());
                preparedStatement.setString(3, (String) forumModel.getUser());
                preparedStatement.execute();
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        forumModel.setId(resultSet.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (e.getErrorCode() == MYSQL_DUPLICATE_PK) {
                return details(jsonObject.get("short_name").getAsString(), null);
            } else {
                return new Response(Response.Codes.UNKNOWN_ERROR);
            }
        }

        return new Response(forumModel);
    }

    @Override
    public Response details(String forum, String[] related) {
        ForumModel forumModel;

        if (related != null && (Arrays.asList(related).contains("forum")
                || Arrays.asList(related).contains("thread"))) {
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT * FROM Forum WHERE short_name = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, forum);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        forumModel = new ForumModel(resultSet);
                    } else {
                        return new Response(Response.Codes.NOT_FOUND);
                    }
                }
            }
            if (related != null) {
                if (Arrays.asList(related).contains("user")) {
                    forumModel.setUser(new UserDAOImpl(dataSource).details((String)forumModel.getUser()).getResponse());
                } else {
                    return new Response(Response.Codes.INCORRECT_QUERY);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.UNKNOWN_ERROR);
        }

        return new Response(forumModel);
    }


    @Override
    public Response listPosts(String forum, String since, Integer limit, String order, String[] related) {
        List<PostModel> array = new ArrayList<>();
        if (order == null) {
            order = "desc";
        }
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SElECT * FROM Post ");
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
        queryBuilder.append(";");

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int index = 0;
                preparedStatement.setString(++index, forum);
                if (since != null) {
                    preparedStatement.setString(++index, since);
                }
                if (limit != null) {
                    preparedStatement.setInt(++index, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        PostModel post = new PostModel(resultSet);
                        if (related != null) {
                            if (Arrays.asList(related).contains("user")) {
                                post.setUser(new UserDAOImpl(dataSource).details((String) post.getUser()).getResponse());
                            }
                            if (Arrays.asList(related).contains("forum")) {
                                post.setForum(details((String) post.getForum(), null).getResponse());
                            }
                            if (Arrays.asList(related).contains("thread")) {
                                post.setThread(new ThreadDAOImpl(dataSource).details((Integer)
                                        post.getThread(), null).getResponse());
                            }
                        }
                        array.add(post);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return new Response(Response.Codes.INCORRECT_QUERY);
            }

            return new Response(array);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Response(array);
    }

    @Override
    public Response listThreads(String forum, String since, Integer limit, String order, String[] related) {
        List<ThreadModel> array = new ArrayList<>();
        if (order == null) {
            order = "desc";
        }
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SElECT * FROM Thread ");
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
        queryBuilder.append(";");

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int index = 0;
                preparedStatement.setString(++index, forum);
                if (since != null) {
                    preparedStatement.setString(++index, since);
                }
                if (limit != null) {
                    preparedStatement.setInt(++index, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        ThreadModel threadModel = new ThreadModel(resultSet);
                        if (Arrays.asList(related).contains("user")) {
                            threadModel.setUser(new UserDAOImpl(dataSource).details((String) threadModel.getUser()).getResponse());
                        }
                        if (Arrays.asList(related).contains("forum")) {
                            threadModel.setForum(details((String) threadModel.getForum(), null).getResponse());
                        }
                        array.add(threadModel);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return new Response(Response.Codes.INCORRECT_QUERY);
            }

            return new Response(array);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new Response(array);
    }

    @Override
    public Response listUsers(String forum, Integer sinceId, Integer limit, String order) {
        List<UserModel> array = new ArrayList<>();

        order = order == null ? "desc" : order;

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT u.* FROM User u ");
        queryBuilder.append("INNER JOIN forum_user fu ");
        queryBuilder.append("ON u.email = fu.user ");
        queryBuilder.append("WHERE fu.forum = ? ");
        if (sinceId != null) {
            queryBuilder.append("AND u.id >= ? ");
        }
        queryBuilder.append("ORDER BY u.name ");
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
                if (sinceId != null) {
                    preparedStatement.setInt(++parameterIndex, sinceId);
                }
                if (limit != null) {
                    preparedStatement.setInt(++parameterIndex, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        UserModel user = new UserModel(resultSet);
                        UserDAOImpl userDAO = new UserDAOImpl(dataSource);
                        user.setFollowers(userDAO.getFollowers(connection, user.getEmail()));
                        user.setFollowing(userDAO.getFollowing(connection, user.getEmail()));
                        user.setSubscriptions(userDAO.getSubscriptions(connection, user.getEmail()));
                        array.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        return new Response(array);
    }
}

