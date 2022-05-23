package consensusBN.circularFusion;

import java.util.function.Consumer;

public class CircularFusionConsumer implements Consumer<CircularDag> {
    @Override
    public void accept(CircularDag circularDag) {

    }

    @Override
    public Consumer<CircularDag> andThen(Consumer<? super CircularDag> after) {
        return Consumer.super.andThen(after);
    }
}
