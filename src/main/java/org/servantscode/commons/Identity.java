package org.servantscode.commons;

public class Identity {
    private String name;
    private int id;

    public Identity() { }

    public Identity(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public boolean equals(Object o) {
        if(!(o instanceof Identity))
            return false;
        return name.equals(((Identity)o).name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    // ----- Accessors -----
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
