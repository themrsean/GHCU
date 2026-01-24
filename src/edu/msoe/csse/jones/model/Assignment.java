/*
 * Course: CSC-1110/1020/1120
 * GitHubClassroom Utilities
 * Last Updated: 1/23/2026
 */
package edu.msoe.csse.jones.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class Assignment {
    private final StringProperty shortName = new SimpleStringProperty();
    private final StringProperty fullName = new SimpleStringProperty();
    private final ObservableList<String> files;
    private Rubric rubric;

    public Assignment() {
        this.files = FXCollections.observableArrayList();
        this.rubric = new Rubric();
    }

    public Assignment(String shortName, String fullName) {
        this();
        this.shortName.set(shortName);
        this.fullName.set(fullName);
    }

    public String getShortName() {
        return shortName.get();
    }

    public void setShortName(String value) {
        shortName.set(value);
    }

    public StringProperty shortNameProperty() {
        return shortName;
    }

    public String getFullName() {
        return fullName.get();
    }

    public void setFullName(String value) {
        fullName.set(value);
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public ObservableList<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files.setAll(files);
    }

    public Rubric getRubric() {
        return rubric;
    }

    public void setRubric(Rubric rubric) {
        this.rubric = rubric;
    }


}
