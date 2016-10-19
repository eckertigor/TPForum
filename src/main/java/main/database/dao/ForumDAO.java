package main.database.dao;

import main.models.Response;
/**
 * Created by ekkert on 16.10.16.
 */
public interface ForumDAO {
    int getCount();

    void truncateTable();

    Response create(String jsonString);

    Response details(String forum, String[] related);

    Response listPosts(String forum, String since, Integer limit, String order, String[] related);

    Response listThreads(String forum, String since, Integer limit, String order, String[] related);

    Response listUsers(String forum, Integer sinceId, Integer limit, String order);


}
