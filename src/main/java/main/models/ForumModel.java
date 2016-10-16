package main.models;

import com.google.gson.JsonObject;
import org.omg.CORBA.Object;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by ekkert on 16.10.16.
 */
public class ForumModel
{
    private int id;
    private String name;
    private String short_name;
    private Object user;

    public ForumModel(int id, String name, String short_name, Object user) {
        this.id = id;
        this.name = name;
        this.short_name = name;
        this.user = user;
    }

    public ForumModel(String name, String short_name, Object user) {
        this.id = -1;
        this.name = name;
        this.short_name = name;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getShort_name() {
        return short_name;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }


}
