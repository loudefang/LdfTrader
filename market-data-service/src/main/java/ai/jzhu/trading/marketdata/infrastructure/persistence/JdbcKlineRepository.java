package ai.jzhu.trading.marketdata.infrastructure.persistence;

import ai.jzhu.trading.marketdata.domain.model.Kline;
import ai.jzhu.trading.marketdata.domain.port.KlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcKlineRepository implements KlineRepository {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("America/New_York");

    /**
     * 不同市场的本地时区, 用于把日期映射到 timestamptz.
     * 同一只标的的所有写入/查询都用相同时区, 因此缓存内部自洽;
     * market 列也参与主键, 不同市场互不影响.
     */
    private static final Map<String, ZoneId> MARKET_ZONES = Map.of(
            "us", ZoneId.of("America/New_York"),
            "cn", ZoneId.of("Asia/Shanghai"),
            "hk", ZoneId.of("Asia/Hong_Kong")
    );

    private static final Set<String> ALLOWED_PERIODS = Set.of("daily", "weekly", "monthly");

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Kline> findBySymbolAndDateRange(
            String symbol,
            String market,
            String period,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String table = resolveTable(period);
        ZoneId zone = resolveZone(market);
        String sql = """
                SELECT time, open, high, low, close, volume
                FROM %s
                WHERE symbol = ? AND market = ? AND time BETWEEN ? AND ?
                ORDER BY time ASC
                """.formatted(table);

        Timestamp startTs = toMarketOpenTimestamp(startDate, zone);
        Timestamp endTs = toMarketCloseTimestamp(endDate, zone);

        return jdbcTemplate.query(sql, klineRowMapper(zone), symbol, market, startTs, endTs);
    }

    @Override
    public int saveBatch(String symbol, String market, String period, List<Kline> klines) {
        if (klines == null || klines.isEmpty()) {
            return 0;
        }
        String table = resolveTable(period);
        ZoneId zone = resolveZone(market);
        String sql = """
                INSERT INTO %s (time, symbol, market, open, high, low, close, volume)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (symbol, market, time) DO NOTHING
                """.formatted(table);

        int[] results = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Kline k = klines.get(i);
                ps.setTimestamp(1, toMarketOpenTimestamp(k.date(), zone));
                ps.setString(2, symbol);
                ps.setString(3, market);
                ps.setDouble(4, k.open());
                ps.setDouble(5, k.high());
                ps.setDouble(6, k.low());
                ps.setDouble(7, k.close());
                ps.setLong(8, k.volume());
            }

            @Override
            public int getBatchSize() {
                return klines.size();
            }
        });

        int total = 0;
        for (int r : results) {
            if (r > 0) total += r;
        }
        return total;
    }

    private String resolveTable(String period) {
        String normalized = period == null ? "daily" : period.toLowerCase();
        if (!ALLOWED_PERIODS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported period: " + period);
        }
        return "kline_" + normalized;
    }

    private ZoneId resolveZone(String market) {
        if (market == null) return DEFAULT_ZONE;
        return MARKET_ZONES.getOrDefault(market.toLowerCase(), DEFAULT_ZONE);
    }

    private Timestamp toMarketOpenTimestamp(LocalDate date, ZoneId zone) {
        ZonedDateTime zdt = ZonedDateTime.of(date, LocalTime.of(0, 0), zone);
        return Timestamp.from(zdt.toInstant());
    }

    private Timestamp toMarketCloseTimestamp(LocalDate date, ZoneId zone) {
        ZonedDateTime zdt = ZonedDateTime.of(date, LocalTime.of(23, 59, 59), zone);
        return Timestamp.from(zdt.toInstant());
    }

    private RowMapper<Kline> klineRowMapper(ZoneId zone) {
        return (rs, rowNum) -> {
            LocalDate date = rs.getTimestamp("time").toInstant().atZone(zone).toLocalDate();
            return new Kline(
                    date,
                    rs.getDouble("open"),
                    rs.getDouble("high"),
                    rs.getDouble("low"),
                    rs.getDouble("close"),
                    rs.getLong("volume")
            );
        };
    }
}
