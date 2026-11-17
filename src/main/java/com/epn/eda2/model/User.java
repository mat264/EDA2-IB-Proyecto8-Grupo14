package com.epn.eda2.model;

import java.util.Objects;

/**
 * Representa un nodo de tipo Usuario (u in U) dentro del grafo bipartito.
 */
public class User implements Comparable<User> {
    private final int userId;

    public User(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public int compareTo(User other) {
        return Integer.compare(this.userId, other.userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return userId == user.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "Usuario U" + userId + " (ID: " + userId + ")";
    }
}
