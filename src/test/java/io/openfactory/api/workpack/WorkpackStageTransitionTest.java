package io.openfactory.api.workpack;

import io.openfactory.api.workpack.model.WorkpackStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkpackStageTransitionTest {

    private WorkpackStage advance(WorkpackStage stage) {
        return switch (stage) {
            case RAW    -> WorkpackStage.DEFINE;
            case DEFINE -> WorkpackStage.SHAPE;
            case SHAPE  -> WorkpackStage.BOX;
            case BOX    -> throw new IllegalStateException("Already at BOX stage.");
        };
    }

    @Test
    void rawAdvancesToDefine() {
        assertEquals(WorkpackStage.DEFINE, advance(WorkpackStage.RAW));
    }

    @Test
    void defineAdvancesToShape() {
        assertEquals(WorkpackStage.SHAPE, advance(WorkpackStage.DEFINE));
    }

    @Test
    void shapeAdvancesToBox() {
        assertEquals(WorkpackStage.BOX, advance(WorkpackStage.SHAPE));
    }

    @Test
    void boxThrowsIllegalState() {
        assertThrows(IllegalStateException.class, () -> advance(WorkpackStage.BOX));
    }

    @Test
    void fullPipelineProgression() {
        WorkpackStage stage = WorkpackStage.RAW;
        stage = advance(stage);
        assertEquals(WorkpackStage.DEFINE, stage);
        stage = advance(stage);
        assertEquals(WorkpackStage.SHAPE, stage);
        stage = advance(stage);
        assertEquals(WorkpackStage.BOX, stage);
    }
}
