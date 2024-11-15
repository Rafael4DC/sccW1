package tukano.impl.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.Objects;

@Entity
public class Likes {

    public static final String CONTAINER_NAME = "likes";

    @Id
    @JsonProperty("id")
    String id;

    @Id
    String userId;

    @Id
    String shortId;
    String ownerId;

    public Likes() {
    }

    public Likes(String userId, String shortId, String ownerId) {
        this.id = userId+shortId;
        this.userId = userId;
        this.shortId = shortId;
        this.ownerId = ownerId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    @Override
    public String toString() {
        return "Likes [userId=" + userId + ", shortId=" + shortId + ", ownerId=" + ownerId + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId, shortId, userId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Likes other = (Likes) obj;
        return Objects.equals(ownerId, other.ownerId) && Objects.equals(shortId, other.shortId)
                && Objects.equals(userId, other.userId);
    }


}