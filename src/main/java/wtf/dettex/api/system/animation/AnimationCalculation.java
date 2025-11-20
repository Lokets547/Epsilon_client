package wtf.dettex.api.system.animation;

public interface AnimationCalculation {
    default double calculation(double value){
        return 0;
    }
}

