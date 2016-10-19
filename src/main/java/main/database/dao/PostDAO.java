package main.database.dao;

import main.models.Response;
/**
 * Created by ekkert on 18.10.16.
 */
public interface PostDAO {
    int getCount();

    void truncateTable();

    Response create(String jsonString);

    Response details(Integer post, String[] related);

    Response listForumPosts(String forum, String since, Integer limit, String order);

    Response listThreadPosts(Integer thread, String since, Integer limit, String order);

    Response remove(String jsonString);

    Response restore(String jsonString);

    Response update(String jsonString);

    Response vote(String jsonString);
}
