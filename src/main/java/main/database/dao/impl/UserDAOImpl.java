package main.database.dao.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import main.database.dao.UserDAO;
import main.database.executor.TExecutor;
import main.models.PostModel;
import main.models.Response;
import main.models.UserModel;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ekkert on 19.10.16.
 */
public class UserDAOImpl implements UserDAO {

    DataSource dataSource;

    public UserDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int getCount() {
        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT COUNT(*) FROM User;";
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
    public Response create(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        UserModel userModel;
        try {
            userModel = new UserModel(jsonObject);
        } catch (Exception e) {
            return new Response(Response.Codes.INCORRECT_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "INSERT INTO User (username, about, name, email, isAnonymous) VALUES (?,?,?,?,?);";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, userModel.getUsername());
                preparedStatement.setString(2, userModel.getAbout());
                preparedStatement.setString(3, userModel.getName());
                preparedStatement.setString(4, userModel.getEmail());
                preparedStatement.setBoolean(5, userModel.isAnonymous());
                preparedStatement.execute();
                try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        userModel.setId(resultSet.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.UNKNOWN_ERROR);
        }
        return new Response(userModel);
    }

    @Override
    public Response details(String email) {
        UserModel userModel;

        try (Connection connection = dataSource.getConnection()) {
            String query = "SELECT * FROM User WHERE email = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setString(1, email);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        userModel = new UserModel(resultSet);
                    } else {
                        return new Response(Response.Codes.NOT_FOUND);
                    }
                }
            }
            userModel.setFollowers(getFollowers(connection, email));
            userModel.setFollowing(getFollowing(connection, email));
            userModel.setSubscriptions(getSubscriptions(connection, email));
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.UNKNOWN_ERROR);
        }
        return new Response(userModel);
    }

    public List<String> getFollowers(Connection connection, String email) throws SQLException {
        List<String> array = new ArrayList<>();

        String query = "SELECT follower FROM Follow WHERE followee = ?;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    array.add(resultSet.getString("follower"));
                }
            }
        }
        return array;
    }

    public List<String> getFollowing(Connection connection, String email) throws SQLException {
        List<String> array = new ArrayList<>();

        String query = "SELECT followee FROM Follow WHERE follower = ?;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    array.add(resultSet.getString("followee"));
                }
            }
        }
        return array;
    }


    public List<Integer> getSubscriptions(Connection connection, String email) throws SQLException {
        List<Integer> array = new ArrayList<>();

        String query = "SELECT thread FROM Subscribe WHERE user = ?;";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    array.add(resultSet.getInt("thread"));
                }
            }
        }
        return array;
    }

    @Override
    public Response follow(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "INSERT IGNOR INTO Follow (follower, followee) VALUES(?, ?);";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, jsonObject.get("follower").getAsInt());
                preparedStatement.setInt(2, jsonObject.get("followee").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return details(jsonObject.get("follower").getAsString());
    }

    @Override
    public Response unfollow(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "DELETE FROM Foloow WHERE follower = ? AND followee = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, jsonObject.get("follower").getAsInt());
                preparedStatement.setInt(2, jsonObject.get("followee").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return details(jsonObject.get("follower").getAsString());
    }

    @Override
    public Response listFollowers(String user, Integer limit, String order, Integer sinceId) {
        List<UserModel> array = new ArrayList<>();
        if (order == null) {
            order = "desc";
        }
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SElECT u.* FROM User as u ");
        queryBuilder.append("JOIN Follow as f ON u.email = f.follower ");
        queryBuilder.append("WHERE followee = ? ");
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
        queryBuilder.append(";");

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int index = 0;
                preparedStatement.setString(++index, user);
                if (sinceId != null) {
                    preparedStatement.setInt(++index, sinceId);
                }
                if (limit != null) {
                    preparedStatement.setInt(++index, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        UserModel follower = new UserModel(resultSet);
                        follower.setFollowers(getFollowers(connection, follower.getEmail()));
                        follower.setFollowing(getFollowing(connection, follower.getEmail()));
                        follower.setSubscriptions(getSubscriptions(connection, follower.getEmail()));
                        array.add(follower);
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
    public Response listFollowing(String user, Integer limit, String order, Integer sinceId) {
        List<UserModel> array = new ArrayList<>();
        if (order == null) {
            order = "desc";
        }
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SElECT u.* FROM User as u ");
        queryBuilder.append("JOIN Follow as f ON u.email = f.followee ");
        queryBuilder.append("WHERE follower = ? ");
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
        queryBuilder.append(";");

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int index = 0;
                preparedStatement.setString(++index, user);
                if (sinceId != null) {
                    preparedStatement.setInt(++index, sinceId);
                }
                if (limit != null) {
                    preparedStatement.setInt(++index, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        UserModel follower = new UserModel(resultSet);
                        follower.setFollowers(getFollowers(connection, follower.getEmail()));
                        follower.setFollowing(getFollowing(connection, follower.getEmail()));
                        follower.setSubscriptions(getSubscriptions(connection, follower.getEmail()));
                        array.add(follower);
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
    public Response listPosts(String user, String since, Integer limit, String order) {
        List<PostModel> array = new ArrayList<>();
        if (order == null) {
            order = "desc";
        }
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SElECT * FROM Post ");
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
        queryBuilder.append(";");

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(queryBuilder.toString())) {
                int index = 0;
                preparedStatement.setString(++index, user);
                if (since != null) {
                    preparedStatement.setString(++index, since);
                }
                if (limit != null) {
                    preparedStatement.setInt(++index, limit);
                }
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        PostModel postModel = new PostModel(resultSet);
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
    public Response updateProfile(String jsonString) {
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return new Response(Response.Codes.INVALID_QUERY);
        }

        try (Connection connection = dataSource.getConnection()) {
            String query = "UPDATE User SET about = ?, name = ? WHERE email = ?;";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, jsonObject.get("about").getAsInt());
                preparedStatement.setInt(2, jsonObject.get("name").getAsInt());
                preparedStatement.setInt(3, jsonObject.get("user").getAsInt());
                preparedStatement.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.Codes.INCORRECT_QUERY);
        }
        return new Response(details(jsonObject.get("user").getAsString()));
    }
}
