package main.models;

import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by ekkert on 18.10.16.
 */
public class ThreadModel {
    private int id;
    private Object forum;
    private String title;
    private boolean isClosed;
    private Object user;
    private String date;
    private String message;
    private String slug;
    private boolean isDeleted;
    private int likes;
    private int dislikes;
    private int points;
    private int posts;

    public ThreadModel(int id, Object forum, String title, boolean isClosed, Object user, String date,
                       String message, String slug, boolean isDeleted, int likes, int dislikes, int points, int posts) {
        this.id = id;
        this.forum = forum;
        this.title = title;
        this.isClosed = isClosed;
        this.user = user;
        this.date = date;
        this.message = message;
        this.slug = slug;
        this.isDeleted = isDeleted;
        this.likes = likes;
        this.dislikes = dislikes;
        this.points = points;
        this.posts = posts;
    }

    public ThreadModel(Object forum, String title, boolean isClosed, Object user, String date,
                       String message, String slug, boolean isDeleted) {
        this (-1, forum, title, isClosed, user, date, message, slug, isDeleted, 0, 0, 0, 0);
    }

    public ThreadModel(JsonObject jsonObject) {
        this (
                jsonObject.get("forum").getAsString(),
                jsonObject.get("title").getAsString(),
                jsonObject.get("isClosed").getAsBoolean(),
                jsonObject.get("user").getAsString(),
                jsonObject.get("date").getAsString(),
                jsonObject.get("message").getAsString(),
                jsonObject.get("slug").getAsString(),
                jsonObject.get("isDeleted").getAsBoolean()
        );
    }

    public ThreadModel(ResultSet resultSet) throws SQLException {
        this (
                resultSet.getInt("id"),
                resultSet.getString("forum"),
                resultSet.getString("title"),
                resultSet.getBoolean("isClosed"),
                resultSet.getString("user"),
                resultSet.getString("date"),
                resultSet.getString("message"),
                resultSet.getString("slug"),
                resultSet.getBoolean("isDeleted"),
                resultSet.getInt("likes"),
                resultSet.getInt("dislikes"),
                resultSet.getInt("likes") - resultSet.getInt("dislikes"),
                resultSet.getInt("posts")
        );
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Object getForum() {
        return forum;
    }

    public void setForum(Object forum) {
        this.forum = forum;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean getIsClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getPosts() {
        return posts;
    }

    public void setPosts(int posts) {
        this.posts = posts;
    }
}
