package code.groovy;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static tech.tablesaw.aggregate.AggregateFunctions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.numbers.*;
import se.alipsa.gride.chart.*;

import java.io.FileNotFoundException;

public class BarChartTest {

    private static final Logger log = LogManager.getLogger();

    @Test
    public void testJsTransform() throws FileNotFoundException {
        Table table = Table.read().csv(getClass().getResource("/data/tornadoes_1950-2014.csv"));
        NumericColumn<?> logNInjuries = table.numberColumn("injuries").add(1).logN();
        logNInjuries.setName("log injuries");
        table.addColumns(logNInjuries);
        IntColumn scale = table.intColumn("scale");
        scale.set(scale.isLessThan(0), IntColumnType.missingValueIndicator());

        var summaryTable = table.summarize("fatalities", "log injuries", sum).by("Scale");
        log.info("{}", summaryTable);
        var scaleColumn = summaryTable.column("Scale").asStringColumn();
        //var injuriesData = Table.create("Injuries", summaryTable.column("sum [log injuries]"), scaleColumn);
        //var fatalitiesData = Table.create("Fatalities", summaryTable.column("Sum [Fatalities]"), scaleColumn);
        //var chart = BarChart.create("Tornado Impact", ChartType.STACKED, injuriesData, fatalitiesData);
        var chart = BarChart.create("Tornado Impact", ChartType.STACKED, scaleColumn,
            summaryTable.column("sum [log injuries]"),
            summaryTable.column("Sum [Fatalities]"));
        var figure = se.alipsa.gride.chart.Plot.jsPlot(chart);
        assertNotNull(figure);
    }
}