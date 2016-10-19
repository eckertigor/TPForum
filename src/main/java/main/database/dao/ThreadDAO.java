package main.database.dao;

import main.models.Response;

/**
 * Created by ekkert on 18.10.16.
 */
public interface ThreadDAO {
    int getCount();

    void truncateTable();

    Response close(String jsonString);

    Response create(String jsonString);

    Response details(Integer thread, String[] related);

    Response listPosts(Integer thread, String since, Integer limit, String sort, String order);

    Response open(String jsonString);

    Response remove(String jsonString);

    Response restore(String jsonString);

    Response subscribe(String jsonString);

    Response unsubscribe(String jsonString);

    Response update(String jsonString);

    Response vote(String jsonString);

    Response listUserThreads(String user, String since, Integer limit, String order);

    Response listForumThreads(String forum, String since, Integer limit, String order);



}
