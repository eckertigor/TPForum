package main.database.dao;

import main.models.Response;

/**
 * Created by ekkert on 18.10.16.
 */
public interface UserDAO {
    int getCount();

    Response create(String jsonString);

    Response details(String email);

    void truncateTable();

    Response follow(String jsonString);

    Response listFollowers(String user, Integer limit, String order, Integer sinceId);

    Response listFollowing(String user, Integer limit, String order, Integer sinceId);

    Response listPosts(String user, String since, Integer limit, String order);

    Response unfollow(String jsonString);

    Response updateProfile(String jsonString);


}
