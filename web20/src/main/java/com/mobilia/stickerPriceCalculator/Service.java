package com.mobilia.stickerPriceCalculator;

import com.mobilia.stock.parser.model.*;
import com.mobilia.stock.parser.model.morningstar.Roic;
import com.mobilia.stock.parser.service.morningstar.ParseMorningstarService;
import com.mobilia.stock.parser.service.StockCalculationService;
import org.apache.commons.csv.CSVRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by allen on 6/17/17.
 */
@org.springframework.stereotype.Service
public class Service {

    public KeyRatioModel processKeyRatioCsv(List<CSVRecord> csvRecords) {
        Cashflow cashflow = ParseMorningstarService.parseCashFlow(csvRecords, "");
        Eps eps = ParseMorningstarService.parseEps(csvRecords, "");
        Equity equity = ParseMorningstarService.parseEquity(csvRecords, "");
        Roic roic = ParseMorningstarService.parseRoic(csvRecords, "");
        Sales sales = ParseMorningstarService.parseSales(csvRecords, "");

        cashflow.calculateGrowthRate();
        eps.calculateGrowthRate();
        equity.calculateGrowthRate();
        sales.calculateGrowthRate();
        roic.calculateGrowthRate();

        KeyRatioModel model = new KeyRatioModel();

        //big4
        model.getHistoricalBig4Year().add("");//empty space for the first table column
        for (YrsGrowthRateModel tmpModel : eps.getYrsGrowthRateModels()) {
            String value = (tmpModel.getYear() == 1) ? "TTM" : tmpModel.getYear() + "Yrs";
            model.getBig4Year().add(value);
            model.getHistoricalBig4Year().add(value);
        }
        model.getBig4Data().add(populateChartData("Cashflow", cashflow.getYrsGrowthRateModels()));
        model.getBig4Data().add(populateChartData("EPS", eps.getYrsGrowthRateModels()));
        model.getBig4Data().add(populateChartData("Equity", equity.getYrsGrowthRateModels()));
        model.getBig4Data().add(populateChartData("Sales", sales.getYrsGrowthRateModels()));

        //historical table
        model.setCashflowTbl(getPercentageGrowthRate(cashflow.getYrsGrowthRateModels()));
        model.setEpsTbl(getPercentageGrowthRate(eps.getYrsGrowthRateModels()));
        model.setSalesTbl(getPercentageGrowthRate(sales.getYrsGrowthRateModels()));
        model.setEquityTbl(getPercentageGrowthRate(equity.getYrsGrowthRateModels()));

        //roic
        for (int i = 0; i < roic.getDatePriceModels().size(); i++) {
            DatePriceModel tmpModel = roic.getDatePriceModels().get(i);
            String value = (i == 0) ? "TTM" : tmpModel.getDate().getYear() + "";
            model.getRoicYear().add(value);
        }
        model.getRoicData().add(populateRoicChartData(roic.getDatePriceModels()));

        //current EPS
        model.setCurrentEps(eps.getDatePriceModels().get(0).getValue().toString() + "");

        //guesstimate eps growth rate (avg historical equity growth rate)
        Double guessGrowthRate = StockCalculationService.getEstimatedEPSgrowthRate(equity);
        model.setGuess_eps_growthRate(guessGrowthRate.toString());

        return model;
    }

    private Key4Chart populateChartData(String name, List<YrsGrowthRateModel> models) {
        Key4Chart chart = new Key4Chart();
        chart.setName(name);
        for (YrsGrowthRateModel model : models) {
            BigDecimal bd = new BigDecimal(model.getRate() * 100);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            chart.getData().add(bd.doubleValue());
        }
        return chart;
    }

    private Key4Chart populateRoicChartData(List<DatePriceModel> models){
        Key4Chart chart = new Key4Chart();
        chart.setName("ROIC");
        for(DatePriceModel model : models){
            chart.getData().add(model.getValue().doubleValue());
        }
        return chart;
    }

    public List<String>getPercentageGrowthRate(List<YrsGrowthRateModel> models){
        List<String> list = new ArrayList<>();
        for(YrsGrowthRateModel model : models){
            list.add(model.getRatePercent());
        }
        return list;
    }
}