package org.servantscode.commons;

public class Organization {
    private int id;
    private String name;
    private String hostName;
    private String  photoGuid;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getPhotoGuid() { return photoGuid; }
    public void setPhotoGuid(String photoGuid) { this.photoGuid = photoGuid; }
}
