package io.openfactory.api.workpack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class WorkpackMemberId implements Serializable {

    @Column(name = "workpack_id")
    public UUID workpackId;

    @Column(name = "user_id")
    public UUID userId;

    public WorkpackMemberId() {}

    public WorkpackMemberId(UUID workpackId, UUID userId) {
        this.workpackId = workpackId;
        this.userId     = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkpackMemberId that)) return false;
        return Objects.equals(workpackId, that.workpackId)
            && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workpackId, userId);
    }
}
