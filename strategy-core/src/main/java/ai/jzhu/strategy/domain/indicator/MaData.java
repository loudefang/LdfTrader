package ai.jzhu.strategy.domain.indicator;

import java.util.List;

public class MaData {

    private final List<Double> ma5List;
    private final List<Double> ma10List;
    private final List<Double> ma20List;
    private final List<Double> ma30List;
    private final List<Double> ma60List;

    public MaData(List<Double> ma5List, List<Double> ma10List, List<Double> ma20List,
                  List<Double> ma30List, List<Double> ma60List) {
        this.ma5List  = ma5List;
        this.ma10List = ma10List;
        this.ma20List = ma20List;
        this.ma30List = ma30List;
        this.ma60List = ma60List;
    }

    public Double getMa5At(int index)  { return safeGet(ma5List,  index); }
    public Double getMa10At(int index) { return safeGet(ma10List, index); }
    public Double getMa20At(int index) { return safeGet(ma20List, index); }
    public Double getMa30At(int index) { return safeGet(ma30List, index); }
    public Double getMa60At(int index) { return safeGet(ma60List, index); }

    private static Double safeGet(List<Double> list, int index) {
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }
}
