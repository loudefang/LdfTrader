package ai.jzhu.strategy.domain.indicator;

import java.util.List;

public class BollData {

    private final List<Double> upperList;
    private final List<Double> middleList;
    private final List<Double> lowerList;

    public BollData(List<Double> upperList, List<Double> middleList, List<Double> lowerList) {
        this.upperList  = upperList;
        this.middleList = middleList;
        this.lowerList  = lowerList;
    }

    public Double getUpperAt(int index)  { return safeGet(upperList,  index); }
    public Double getMiddleAt(int index) { return safeGet(middleList, index); }
    public Double getLowerAt(int index)  { return safeGet(lowerList,  index); }

    private static Double safeGet(List<Double> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }
}
