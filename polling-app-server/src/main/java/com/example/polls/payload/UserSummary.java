package com.example.polls.payload;

public class UserSummary {
    private Long id;
    private String username;
    private String name;
    private String[] roles;

    public UserSummary(Long id, String username, String name) {
        this.id = id;
        this.username = username;
        this.name = name;
    }

    public UserSummary() {
    }

    public UserSummary(Long id, String username, String name, String[] roles) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }
}
