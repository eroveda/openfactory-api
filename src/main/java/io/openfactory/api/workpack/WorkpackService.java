package io.openfactory.api.workpack;

import io.openfactory.api.user.model.User;
import io.openfactory.api.workpack.model.Workpack;
import io.openfactory.api.workpack.model.WorkpackStage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Map;

@ApplicationScoped
public class WorkpackService {

    @Transactional
    public Map<String, Object> ingest(Workpack wp, User user,
                                       Map<String, Object> body) {
        // TODO: call openfactory-core ingestion pipeline
        // For now — update stage and return snapshot
        wp.stage = WorkpackStage.DEFINE;
        wp.persist();

        return Map.of(
            "workpackId", wp.id,
            "stage", wp.stage,
            "status", "ingested",
            "message", "Ingestion pipeline will be connected to openfactory-core"
        );
    }

    public Map<String, Object> shape(Workpack wp, User user) {
        // TODO: call openfactory-core shape pipeline
        return Map.of(
            "workpackId", wp.id,
            "status", "shaping",
            "message", "Shape pipeline will be connected to openfactory-core"
        );
    }

    public Map<String, Object> export(Workpack wp, User user) {
        // TODO: call openfactory-core export pipeline
        return Map.of(
            "workpackId", wp.id,
            "status", "exporting",
            "message", "Export pipeline will be connected to openfactory-core"
        );
    }
}
