package com.mobilia.stickerPriceCalculator;

import com.mobilia.stock.parser.model.AvePE;
import com.mobilia.stock.parser.service.StockCalculationService;
import com.mobilia.stock.parser.service.morningstar.ParseMorningstarService;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by allen on 6/17/17.
 */
@RestController
public class Controller {
    private Service svc;

    @RequestMapping(value = "/parseMarketPrice", method = RequestMethod.POST)
    public Map<String, String> parseMarketPrice(@RequestBody Map<String, Object> incoming) {
        if (!incoming.containsKey("data"))
            throw new IllegalArgumentException("(parseMarketPrice) the key [data] is not found from incoming JSON");

        String html = incoming.get("data").toString();

        if (StringUtils.isBlank(html))
            throw new IllegalArgumentException("(parseMarketPrice)the key [data] from the JSON payload is empty");

        Map<String, String> map = new HashMap<>();
        map.put("currentPrice", ParseMorningstarService.parseCurrentMarketPrice(new StringBuilder(html)));

        return map;
    }

    @RequestMapping(value = "/parseKeyRatio", method = RequestMethod.POST)
    public KeyRatioModel parseKeyRatio(@RequestBody String incoming) {
        List<CSVRecord> csvRecords = ParseMorningstarService.getCsvRecords(incoming.getBytes(StandardCharsets.UTF_8));
        return svc.processKeyRatioCsv(csvRecords);
    }

    @RequestMapping(value = "/parseProfEpsGrowthRate", method = RequestMethod.POST)
    public Map<String, String> parseProfEpsGrowthRate(@RequestBody Map<String, Object> incoming) {
        if (!incoming.containsKey("ticker") || !incoming.containsKey("html"))
            throw new IllegalArgumentException("(parseProfEpsGrowthRate) the key [ticker, html] are not found from incoming JSON");

        Map<String, Object> map = (Map<String, Object>) incoming.get("html");
        String ticker = incoming.get("ticker").toString();

        if (!map.containsKey("data"))
            throw new IllegalArgumentException("(parseProfEpsGrowthRate) the key [data] is not found from incoming html");

        String html = map.get("data").toString();

        if (StringUtils.isBlank(html))
            throw new IllegalArgumentException("(parseProfEpsGrowthRate)the key [data] from html is empty");

        Map<String, String> out = new HashMap<>();
        out.put("profEpsGrowthRate", ParseMorningstarService.parseProf5YrsEpsGr(new StringBuilder(html), ticker.toUpperCase()));

        return out;
    }

    @RequestMapping(value = "/getFutureEpsGrowthrateDefaultPe", method = RequestMethod.GET)
    public Map<String, String> getFutureEpsGrowthrateDefaultPe(
            @RequestParam(value = "guessEpsGrowthRate", required = true) String guessEpsGrowthRate,
            @RequestParam(value = "profEpsGrowthRate", required = true) String profEpsGrowthRate) {

        BigDecimal guessRate, profRate;
        if (StringUtils.isBlank(guessEpsGrowthRate)) {
            throw new IllegalArgumentException("(calculateFutureEpsGrowthrate) Missing guessEpsGrowthRate");
        }

        try {
            guessRate = new BigDecimal(guessEpsGrowthRate);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("(calculateFutureEpsGrowthrate) guessEpsGrowthRate is not a valid value");
        }

        if (guessRate.equals(BigDecimal.ZERO)) {
            throw new IllegalArgumentException("(calculateFutureEpsGrowthrate) guessEpsGrowthRate shouldn't be zero");
        }

        if (StringUtils.isBlank(profEpsGrowthRate)) {
            profRate = null;
        } else {
            try {
                profRate = new BigDecimal(profEpsGrowthRate);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("(calculateFutureEpsGrowthrate) profEpsGrowthRate is not a valid value");
            }
        }

        Double proRateDouble = (profRate == null) ? null : profRate.doubleValue();
        Double estFutureRate = StockCalculationService.getEstimatedFutureEPSgrowthRate(guessRate.doubleValue(), proRateDouble);
        Double defaultPe = StockCalculationService.getDefaultPE(estFutureRate);
        Map<String, String> response = new HashMap<>();

        response.put("estimatedEpsGrowthRate", estFutureRate.toString());
        response.put("defaultPE", defaultPe.toString());
        return response;
    }

    @RequestMapping(value = "/parseHistoricalPe", method = RequestMethod.POST)
    public Map<String,String> parseHistoricalPe(@RequestBody Map<String, Object> incoming) {
        if (!incoming.containsKey("data"))
            throw new IllegalArgumentException("(parseHistoricalPe) the key [data] is not found from incoming JSON");

        String html = incoming.get("data").toString();

        if (StringUtils.isBlank(html))
            throw new IllegalArgumentException("(parseHistoricalPe)the key [data] from the JSON payload is empty");

        AvePE avePE = ParseMorningstarService.parsePE(new StringBuilder(html), "");
        Double pe = StockCalculationService.getHistoricalPe(avePE);
        Map<String, String> map = new HashMap<>();
        map.put("historicalPeAvg", pe.toString());
        return map;
    }

    @RequestMapping(value = "/getFutureEstimatedPe", method = RequestMethod.GET)
    public Map<String, String> getFutureEstimatedPe(
            @RequestParam(value = "defaultPE", required = true)String defaultPE,
            @RequestParam(value = "historicalPeAvg", required = true)String historicalPeAvg){

        Double defaultPeDouble, historicalPeAvgDouble;
        if(StringUtils.isBlank(defaultPE)){
            throw new IllegalArgumentException("(getFutureEstimatedPe) defaultPE can't be blank");
        }
        try{
            BigDecimal bd = new BigDecimal(defaultPE);
            defaultPeDouble = bd.doubleValue();
        }
        catch(NumberFormatException e){
            throw new IllegalArgumentException("(getFutureEstimatedPe) bad defaultPE value: " + defaultPE);
        }

        if(!StringUtils.isBlank(historicalPeAvg)){
            try{
                BigDecimal bd = new BigDecimal(historicalPeAvg);
                historicalPeAvgDouble = bd.doubleValue();
            }
            catch(NumberFormatException e){
                throw new IllegalArgumentException("(getFutureEstimatedPe) bad historicalPeAvg value: " + historicalPeAvg);
            }
        }
        else{
            historicalPeAvgDouble = null;
        }
        Double estimatedPe = StockCalculationService.getEstimatedFuturePE(defaultPeDouble, historicalPeAvgDouble);
        Map<String,String>map = new HashMap<>();
        map.put("estimatedPe", estimatedPe.toString());
        return map;
    }

    @RequestMapping(value = "/getStickerPrice", method = RequestMethod.GET)
    public Map<String,String>getStickerPrice(
            @RequestParam(value = "currentEPS", required = true)String currentEPS,
            @RequestParam(value = "estimatedEPSgrowthRate", required = true)String estimatedEPSgrowthRate,
            @RequestParam(value = "estimatedPe", required = true)String estimatedPe){

        Double currentEPSDouble, estimatedEPSgrowthRateDouble, estimatedPEDouble;
        if(StringUtils.isBlank(currentEPS) || StringUtils.isBlank(estimatedEPSgrowthRate) ||
                StringUtils.isBlank(estimatedPe)){
            throw new IllegalArgumentException("(getStickerPrice) currentEPS, estimatedEPSgrowthRate, estimatedPE can't be blank");
        }
        try{
            BigDecimal bd;
            bd = new BigDecimal(currentEPS);
            currentEPSDouble = bd.doubleValue();
            bd = new BigDecimal(estimatedEPSgrowthRate);
            estimatedEPSgrowthRateDouble = bd.doubleValue();
            bd = new BigDecimal(estimatedPe);
            estimatedPEDouble = bd.doubleValue();
        }
        catch(NumberFormatException e){
            throw new IllegalArgumentException("(getStickerPrice) currentEPS, estimatedEPSgrowthRate, estimatedPE must be numeric/decimal");
        }
        Double price = StockCalculationService.getStickerPrice(currentEPSDouble, estimatedEPSgrowthRateDouble, estimatedPEDouble);
        Double mos = StockCalculationService.getMarginSafety(price);

        Map<String, String> map = new HashMap<>();
        map.put("stickerPrice", price.toString());
        map.put("mos", mos.toString());
        return map;
    }

    @Autowired
    public void setSvc(Service svc) {
        this.svc = svc;
    }
}