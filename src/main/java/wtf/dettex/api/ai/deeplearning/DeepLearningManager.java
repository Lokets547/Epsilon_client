package wtf.dettex.api.ai.deeplearning;

import lombok.Getter;

@Getter
public class DeepLearningManager {

    private final MinaraiModel model;
    private final MinaraiModel speedModel;
    private final MinaraiModel slowModel;

    public DeepLearningManager() {
        // Models are expected at: /assets/minecraft/ai/<name>.params
        this.model = new MinaraiModel("model");
        this.model.loadFromResources("/assets/minecraft/ai/model.params");

        this.slowModel = new MinaraiModel("slow");
        this.slowModel.loadFromResources("/assets/minecraft/ai/slow.params");

        this.speedModel = new MinaraiModel("tf-0100");
        this.speedModel.loadFromResources("/assets/minecraft/ai/tf-0100.params");
    }
}

