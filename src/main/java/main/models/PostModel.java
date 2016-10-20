package main.models;

import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by ekkert on 18.10.16.
 */
public class PostModel {
    private int id;
    private Object thread;
    private Object user;
    private Object forum;
    private String message;
    private Integer parent;
    private int dislikes;
    private int likes;
    private boolean isApproved;
    private boolean isHighlighted;
    private boolean isEdited;
    private boolean isSpam;
    private boolean isDeleted;
    private String date;

    public PostModel(int id, Object thread, Object user, Object forum, String message, Integer parent,
                     int dislikes, int likes, boolean isApproved, boolean isHighlighted,
                     boolean isEdited, boolean isSpam, boolean isDeleted, String date) {
        this.id = id;
        this.thread = thread;
        this.user = user;
        this.forum = forum;
        this.message = message;
        this.parent = parent;
        this.dislikes = dislikes;
        this.likes = likes;
        this.isApproved = isApproved;
        this.isHighlighted = isHighlighted;
        this.isEdited = isEdited;
        this.isSpam = isSpam;
        this.isDeleted = isDeleted;
        this.date = date;
    }

    public PostModel(Object thread, Object user, Object forum, String message, Integer parent,
                     boolean isApproved, boolean isHighlighted,
                     boolean isEdited, boolean isSpam, boolean isDeleted, String date) {
        this(
                -1, thread, user, forum, message, parent, 0, 0, isApproved, isHighlighted, isEdited,
                isSpam, isDeleted, date
        );
    }

    public PostModel(JsonObject jsonObject) throws Exception{
        this (
                jsonObject.get("thread").getAsInt(),
                jsonObject.get("user").getAsString(),
                jsonObject.get("forum").getAsString(),
                jsonObject.get("message").getAsString(),
                !jsonObject.has("parent") || jsonObject.get("parent").isJsonNull() ? null : jsonObject.get("parent").getAsInt(),
                jsonObject.get("isApproved").getAsBoolean(),
                jsonObject.get("isHighlighted").getAsBoolean(),
                jsonObject.get("isEdited").getAsBoolean(),
                jsonObject.get("isSpam").getAsBoolean(),
                jsonObject.get("isDeleted").getAsBoolean(),
                jsonObject.get("date").getAsString()
        );
    }

    public PostModel(ResultSet resultSet) throws SQLException {
        this (
                resultSet.getInt("id"),
                resultSet.getInt("thread"),
                resultSet.getString("user"),
                resultSet.getString("forum"),
                resultSet.getString("message"),
                resultSet.getInt("parent"),
                resultSet.getInt("dislikes"),
                resultSet.getInt("likes"),
                resultSet.getBoolean("isApproved"),
                resultSet.getBoolean("isHighlighted"),
                resultSet.getBoolean("isEdited"),
                resultSet.getBoolean("isSpam"),
                resultSet.getBoolean("isDeleted"),
                resultSet.getString("date")
        );
        date = date.substring(0, date.length() -2);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Object getThread() {
        return thread;
    }

    public void setThread(Object thread) {
        this.thread = thread;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }

    public Object getForum() {
        return forum;
    }

    public void setForum(Object forum) {
        this.forum = forum;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean highlighted) {
        isHighlighted = highlighted;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public boolean isSpam() {
        return isSpam;
    }

    public void setSpam(boolean spam) {
        isSpam = spam;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
