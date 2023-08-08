package com.mycompany.gainz.Database.Model;

public class Bodypart {
    int _id;
    String title;

    public Bodypart(String title) {
        this.title = title;
    }

    // SETTER

    public void set_id(int _id) {
        this._id = _id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // GETTER


    public int get_id() {
        return _id;
    }

    public String getTitle() {
        return title;
    }
}
