package wtf.dettex.api.ai.deeplearning;

public class MinaraiModel extends ModelWrapper<float[], float[]> {
    public MinaraiModel(String name) {
        super(name, new FloatArrayInAndOutTranslator(), 2);
    }
}
