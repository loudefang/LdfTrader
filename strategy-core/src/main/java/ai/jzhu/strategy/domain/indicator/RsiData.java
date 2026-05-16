package ai.jzhu.strategy.domain.indicator;

import java.util.List;

public class RsiData {

    private final List<Double> rsi6List;
    private final List<Double> rsi12List;
    private final List<Double> rsi24List;

    public RsiData(List<Double> rsi6List, List<Double> rsi12List, List<Double> rsi24List) {
        this.rsi6List  = rsi6List;
        this.rsi12List = rsi12List;
        this.rsi24List = rsi24List;
    }

    public Double getRsi6At(int index)  { return safeGet(rsi6List,  index); }
    public Double getRsi12At(int index) { return safeGet(rsi12List, index); }
    public Double getRsi24At(int index) { return safeGet(rsi24List, index); }

    private static Double safeGet(List<Double> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }
}
