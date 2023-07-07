package uk.gov.companieshouse.company.profile.model;

import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

public class Updated {

    @Field("at")
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime at;

    private String by;

    public Updated setBy(String by) {
        this.by = by;
        return this;
    }

    public Updated setType(String type) {
        this.type = type;
        return this;
    }

    private String type;

    /**
     * Instantiate company insolvency updated data.
     * @param at timestamp for the delta change
     * @param by updated by
     * @param type the delta type
     */
    public Updated(LocalDateTime at, String by, String type) {
        this.at = at;
        this.by = by;
        this.type = type;
    }

    public Updated() {
    }

    public LocalDateTime getAt() {
        return at;
    }

    public Updated setAt(LocalDateTime at) {
        this.at = at;
        return this;
    }

    public String getBy() {
        return by;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Updated{");
        sb.append("at=").append(at);
        sb.append(", type=").append(type);
        sb.append(", by=").append(by);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Updated updated = (Updated) obj;
        return at.equals(updated.at) && type.equals(updated.type) && by.equals(updated.by);
    }

    @Override
    public int hashCode() {
        return Objects.hash(at, type, by);
    }
}
