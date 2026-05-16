package ai.jzhu.strategy.domain.indicator;

import java.util.List;

public class MacdData {

    private final List<Double> difList;
    private final List<Double> deaList;
    private final List<Double> macdList;

    public MacdData(List<Double> difList, List<Double> deaList, List<Double> macdList) {
        this.difList = difList;
        this.deaList = deaList;
        this.macdList = macdList;
    }

    public Double getDifAt(int index)  { return safeGet(difList,  index); }
    public Double getDeaAt(int index)  { return safeGet(deaList,  index); }
    public Double getMacdAt(int index) { return safeGet(macdList, index); }

    private static Double safeGet(List<Double> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }
}
