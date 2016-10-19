package main.models;

import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ekkert on 18.10.16.
 */
public class UserModel {
    private int id;
    private String username;
    private String about;
    private String name;
    private String email;
    private boolean isAnonymous;
    private List<String> followers;
    private List<String> following;
    private List<Integer> subscriptions;

    public UserModel(int id, String username, String name, String email, boolean isAnonymous, List<String> followers,
                     List<String> following, List<Integer> subscriptions, String about) {

        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.isAnonymous = isAnonymous;
        this.followers = followers;
        this.following = following;
        this.subscriptions = subscriptions;
        this.about = about;


    }

    public UserModel(String username, String name, String email, boolean isAnonymous, String about) {
        this (-1, username, name, email, isAnonymous, new ArrayList<String>(),
                new ArrayList<String>(), new ArrayList<Integer>(), about);
    }

    public UserModel(JsonObject jsonObject) {
        this (
                jsonObject.get("username").getAsString(),
                jsonObject.get("name").getAsString(),
                jsonObject.get("email").getAsString(),
                jsonObject.get("isAnonymous").getAsBoolean(),
                jsonObject.get("about").getAsString()
        );
    }

    public UserModel(ResultSet resultSet) throws SQLException {
        this (
                resultSet.getString("username"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getBoolean("isAnonymous"),
                resultSet.getString("about")
        );
    }

    public List<Integer> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Integer> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public List<String> getFollowers() {
        return followers;
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;
    }

    public List<String> getFollowing() {
        return following;
    }

    public void setFollowing(List<String> following) {
        this.following = following;
    }

}
