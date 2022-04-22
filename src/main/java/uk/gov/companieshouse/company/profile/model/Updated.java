package uk.gov.companieshouse.company.profile.model;

import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;



public class Updated {

    @Field("at")
    @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime at;

    private String by;

    public void setBy(String by) {
        this.by = by;
    }

    public void setType(String type) {
        this.type = type;
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

    public void setAt(LocalDateTime at) {
        this.at = at;
    }

    public String getBy() {
        return by;
    }

    public String getType() {
        return type;
    }
}
