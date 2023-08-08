package com.mycompany.gainz.Database.Model;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;


public class Workout {
    int _id;
    String name = "";
    Calendar date = new GregorianCalendar();
    String note = "";
    boolean isRoutine = false;

    public SortedSetMultimap<Integer, Set> setsInRows = TreeMultimap.create();
    public Map<Integer, String> supersetNotes = new HashMap<>();

    public Workout() {
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public void setName(String title) {
        this.name = title;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setIsRoutine(boolean isRoutine) {
        this.isRoutine = isRoutine;
    }

    public int get_id() {
        return _id;
    }

    public String getName() {
        return name;
    }

    public Calendar getDate() {
        return date;
    }

    public String getNote() {
        return note;
    }

    public boolean isRoutine() {
        return isRoutine;
    }
}