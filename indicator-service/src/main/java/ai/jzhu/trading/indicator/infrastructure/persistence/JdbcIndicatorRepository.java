package ai.jzhu.trading.indicator.infrastructure.persistence;

import ai.jzhu.trading.common.dto.indicator.BollResult;
import ai.jzhu.trading.common.dto.indicator.MacdResult;
import ai.jzhu.trading.common.dto.indicator.MaResult;
import ai.jzhu.trading.common.dto.indicator.RsiResult;
import ai.jzhu.trading.indicator.domain.model.IndicatorValues;
import ai.jzhu.trading.indicator.domain.port.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcIndicatorRepository implements IndicatorRepository {

    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<IndicatorValues> findByCache(String symbol, String market, String period,
                                                  LocalDate startDate, LocalDate endDate, int expectedCount) {
        if (!"daily".equals(period)) {
            return Optional.empty();
        }

        Timestamp start = toTimestamp(startDate);
        Timestamp end = toTimestamp(endDate);

        var maRows = queryMa(symbol, market, start, end);
        if (maRows.size() != expectedCount) return Optional.empty();

        var macdRows = queryMacd(symbol, market, start, end);
        if (macdRows.size() != expectedCount) return Optional.empty();

        var rsiRows = queryRsi(symbol, market, start, end);
        if (rsiRows.size() != expectedCount) return Optional.empty();

        var bollRows = queryBoll(symbol, market, start, end);
        if (bollRows.size() != expectedCount) return Optional.empty();

        return Optional.of(new IndicatorValues(
                new MacdResult(
                        macdRows.stream().map(r -> r[0]).toList(),
                        macdRows.stream().map(r -> r[1]).toList(),
                        macdRows.stream().map(r -> r[2]).toList()
                ),
                new MaResult(
                        maRows.stream().map(r -> r[0]).toList(),
                        maRows.stream().map(r -> r[1]).toList(),
                        maRows.stream().map(r -> r[2]).toList(),
                        maRows.stream().map(r -> r[3]).toList(),
                        maRows.stream().map(r -> r[4]).toList()
                ),
                new RsiResult(
                        rsiRows.stream().map(r -> r[0]).toList(),
                        rsiRows.stream().map(r -> r[1]).toList(),
                        rsiRows.stream().map(r -> r[2]).toList()
                ),
                new BollResult(
                        bollRows.stream().map(r -> r[0]).toList(),
                        bollRows.stream().map(r -> r[1]).toList(),
                        bollRows.stream().map(r -> r[2]).toList()
                )
        ));
    }

    @Override
    public void saveBatch(String symbol, String market, String period,
                          List<LocalDate> dates, IndicatorValues values) {
        if (!"daily".equals(period)) {
            log.warn("Skipping indicator cache save for non-daily period: {}", period);
            return;
        }

        int n = dates.size();

        jdbcTemplate.batchUpdate(
                "INSERT INTO ma_daily (time, symbol, market, ma5, ma10, ma20, ma30, ma60) VALUES (?,?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setTimestamp(1, toTimestamp(dates.get(i)));
                        ps.setString(2, symbol);
                        ps.setString(3, market);
                        setNullableDouble(ps, 4, values.ma().ma5List().get(i));
                        setNullableDouble(ps, 5, values.ma().ma10List().get(i));
                        setNullableDouble(ps, 6, values.ma().ma20List().get(i));
                        setNullableDouble(ps, 7, values.ma().ma30List().get(i));
                        setNullableDouble(ps, 8, values.ma().ma60List().get(i));
                    }
                    @Override public int getBatchSize() { return n; }
                }
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO macd_daily (time, symbol, market, dif, dea, macd_hist) VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setTimestamp(1, toTimestamp(dates.get(i)));
                        ps.setString(2, symbol);
                        ps.setString(3, market);
                        setNullableDouble(ps, 4, values.macd().difList().get(i));
                        setNullableDouble(ps, 5, values.macd().deaList().get(i));
                        setNullableDouble(ps, 6, values.macd().macdList().get(i));
                    }
                    @Override public int getBatchSize() { return n; }
                }
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO rsi_daily (time, symbol, market, rsi6, rsi12, rsi24) VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setTimestamp(1, toTimestamp(dates.get(i)));
                        ps.setString(2, symbol);
                        ps.setString(3, market);
                        setNullableDouble(ps, 4, values.rsi().rsi6List().get(i));
                        setNullableDouble(ps, 5, values.rsi().rsi12List().get(i));
                        setNullableDouble(ps, 6, values.rsi().rsi24List().get(i));
                    }
                    @Override public int getBatchSize() { return n; }
                }
        );

        jdbcTemplate.batchUpdate(
                "INSERT INTO boll_daily (time, symbol, market, upper_band, middle_band, lower_band) VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setTimestamp(1, toTimestamp(dates.get(i)));
                        ps.setString(2, symbol);
                        ps.setString(3, market);
                        setNullableDouble(ps, 4, values.boll().upperList().get(i));
                        setNullableDouble(ps, 5, values.boll().middleList().get(i));
                        setNullableDouble(ps, 6, values.boll().lowerList().get(i));
                    }
                    @Override public int getBatchSize() { return n; }
                }
        );

        log.info("已缓存指标 symbol={} market={} rows={}", symbol, market, n);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private List<Double[]> queryMa(String symbol, String market, Timestamp start, Timestamp end) {
        return jdbcTemplate.query(
                "SELECT ma5, ma10, ma20, ma30, ma60 FROM ma_daily WHERE symbol=? AND market=? AND time>=? AND time<=? ORDER BY time ASC",
                (rs, i) -> new Double[]{
                        rs.getObject("ma5", Double.class),
                        rs.getObject("ma10", Double.class),
                        rs.getObject("ma20", Double.class),
                        rs.getObject("ma30", Double.class),
                        rs.getObject("ma60", Double.class)
                },
                symbol, market, start, end
        );
    }

    private List<Double[]> queryMacd(String symbol, String market, Timestamp start, Timestamp end) {
        return jdbcTemplate.query(
                "SELECT dif, dea, macd_hist FROM macd_daily WHERE symbol=? AND market=? AND time>=? AND time<=? ORDER BY time ASC",
                (rs, i) -> new Double[]{
                        rs.getObject("dif", Double.class),
                        rs.getObject("dea", Double.class),
                        rs.getObject("macd_hist", Double.class)
                },
                symbol, market, start, end
        );
    }

    private List<Double[]> queryRsi(String symbol, String market, Timestamp start, Timestamp end) {
        return jdbcTemplate.query(
                "SELECT rsi6, rsi12, rsi24 FROM rsi_daily WHERE symbol=? AND market=? AND time>=? AND time<=? ORDER BY time ASC",
                (rs, i) -> new Double[]{
                        rs.getObject("rsi6", Double.class),
                        rs.getObject("rsi12", Double.class),
                        rs.getObject("rsi24", Double.class)
                },
                symbol, market, start, end
        );
    }

    private List<Double[]> queryBoll(String symbol, String market, Timestamp start, Timestamp end) {
        return jdbcTemplate.query(
                "SELECT upper_band, middle_band, lower_band FROM boll_daily WHERE symbol=? AND market=? AND time>=? AND time<=? ORDER BY time ASC",
                (rs, i) -> new Double[]{
                        rs.getObject("upper_band", Double.class),
                        rs.getObject("middle_band", Double.class),
                        rs.getObject("lower_band", Double.class)
                },
                symbol, market, start, end
        );
    }

    private static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(index, value);
        } else {
            ps.setNull(index, Types.DOUBLE);
        }
    }

    private static Timestamp toTimestamp(LocalDate date) {
        return Timestamp.from(date.atStartOfDay(NY_ZONE).toInstant());
    }
}
