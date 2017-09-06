angular.module('stickerPriceApp', ['AngularChart', 'ngModal', 'autoCompleteModule']);

angular.module('stickerPriceApp').config(function ($sceDelegateProvider) {
    $sceDelegateProvider.resourceUrlWhitelist([
        'self',
        'http://www.morningstar.com/**'
    ]);
});

angular.module('stickerPriceApp').controller('MainCtrl', function ($scope, $http, tickerService, ratioService, peService, epsService) {

    clearAll();

    function renderDropdown(response){
        var results = response.data.m[0].r;
        var matches = _.filter(results, function(obj){
            return ~obj.XI018.indexOf('USA');
        });
        return matches;
    }

    function failedRenderDropdown(response){
        alert(response.message);
    }

    $scope.autoCompleteOptions = {
        minimumChars: 3,
        selectedTextAttr: 'OS001',
        itemTemplate: "<div>{{item.OS001}} - {{item.OS01W}} - {{item.XI018}} - {{item.LS01Z}}</div>",
        data: function (term){
            term = term.toUpperCase();
            var url = "http://www.morningstar.com/api/v1/autocomplete/5/us/" + term;
            return $http.jsonp(url).then(renderDropdown, failedRenderDropdown);
        },
        itemSelected: function (item) {
            $('#txt').focus();
        }
    };


    $scope.myClick = function () {
        $scope.clickable = true;

        var exchangeJson = null;
        var rule1 = {
            currentPrice : "",
            currentEps : "",
            guessEpsGrowthRate : "",
            profEpsGrowthRate : "",
            estimatedEpsGrowthRate : "",
            defaultPE : "",
            historicalPeAvg : "",
            estimatedPe : "",
            stickerPrice : "",
            mos : ""
        };
        clearAll();

        tickerService.getExchange($scope.ticker).then(continueProcess, failedProcess);

        function continueProcess(response){
            exchangeJson = tickerService.processExchangeResponse(response);
            if(exchangeJson.country != "USA"){
                alert("bad request: ticker [" + $scope.ticker + "] is not a US company");
                return;
            }
            
            exchangeJson.morningStarQuery = exchangeJson.exchange + ":" + $scope.ticker.toUpperCase();
            tickerService.getCurrentPrice(exchangeJson)
                .then(function(response){
                    return tickerService.processCurrentPrice(response);
                })
                .then(function(response){
                    var result = response.data;
                    rule1.currentPrice = result.currentPrice;
                    return ratioService.getCsv(exchangeJson);
                })
                .then(function(response){
                    var csv = response.data;
                    return ratioService.processCsv(csv);
                })
                .then(function(response){
                    var result = response.data;

                    //charts
                    $scope.lineChartXData = result.big4Year;
                    $scope.lineChartYData = result.big4Data;
                    $scope.lineChartXData2 = result.roicYear;
                    $scope.lineChartYData2 = result.roicData;

                    //table
                    $scope.historicalYear = result.historicalBig4Year;
                    $scope.historicalCashflow = result.cashflowTbl;
                    $scope.historicalEps = result.epsTbl;
                    $scope.historicalEquity = result.equityTbl;
                    $scope.historicalSales = result.salesTbl;
                    $scope.showTableLink = true;

                    //cache for later use...
                    rule1.currentEps = result.currentEps;
                    rule1.guessEpsGrowthRate = result.guess_eps_growthRate;

                    return epsService.getProfEpsGrowthRate(exchangeJson);
                })
                .then(function(response){
                    return epsService.processProfEpsGrowthRate(response, exchangeJson);
                })
                .then(function(response){
                    var result = response.data;
                    rule1.profEpsGrowthRate =  result.profEpsGrowthRate;
                    return epsService.processFutureEpsGrowthRate(rule1);
                })
                .then(function(response){
                    var result = response.data;
                    rule1.estimatedEpsGrowthRate = result.estimatedEpsGrowthRate + "";
                    rule1.defaultPE = result.defaultPE + "";
                    return peService.getHistoricalPe(exchangeJson);
                })
                .then(function(response){
                    return peService.processHistoricalPe(response);
                })
                .then(function(response){
                    var result = response.data;
                    rule1.historicalPeAvg = result.historicalPeAvg;
                    return peService.processFutureEstimatePe(rule1);
                })
                .then(function(response){
                    var result = response.data;
                    rule1.estimatedPe = result.estimatedPe;
                    return tickerService.processStickerPrice(rule1);
                })
                .then(function(response){
                    var result = response.data;
                    rule1.stickerPrice = result.stickerPrice;
                    rule1.mos = result.mos;

                    updatePriceSet(rule1);
                    console.log(JSON.stringify(rule1));
                })
                .catch(failedProcess)
                .finally(function() {
                    $scope.clickable = false;
                });
            
        }//continueProcess

        function failedProcess(response){
            var msg = "url: " + response.config.url + "\n";
            msg += "status: " + response.status + "\n";
            msg += "data: " + response.data;
            alert("Issue Processing Request: \n" + msg);
        }//failedProcess

        function updatePriceSet(rule1){
            $scope.currentPrice = rule1.currentPrice;
            $scope.showCurrentPrice = (rule1.currentPrice);

            $scope.currentEps = rule1.currentEps;
            
            $scope.guessEpsGrowthRate = rule1.guessEpsGrowthRate;
            $scope.showGuessEpsGrowthRate = (rule1.estimatedEpsGrowthRate == rule1.guessEpsGrowthRate);
            $scope.profEpsGrowthRate = rule1.profEpsGrowthRate;
            $scope.showprofEpsGrowthRate = (rule1.estimatedEpsGrowthRate == rule1.profEpsGrowthRate);

            $scope.defaultPe = rule1.defaultPE;
            $scope.showDefaultPe = (rule1.estimatedPe == rule1.defaultPE);
            $scope.historicalPeAvg = rule1.historicalPeAvg;
            $scope.showHistoricalPeAvg = (rule1.estimatedPe == rule1.historicalPeAvg);
            
            $scope.stickerPrice = rule1.stickerPrice;
            //model window won't show if showStickerPrice = false
            $scope.showStickerPrice = (rule1.stickerPrice);

            $scope.mos = rule1.mos;
            
            /**
             * allen: 
             * 3) add source to sticker price calculator (maybe?)             
             */
        }

    };//click

    $scope.toggleTable = function () {
        $scope.showTable = !$scope.showTable;
        $scope.showTableLabel = ($scope.showTableLabel == 'Detail') ? 'Hide' : 'Detail';
    };

    $scope.toggleModal = function () {
        $scope.dialogShown = !$scope.dialogShown;
    };

    function clearAll(){
        $scope.showCurrentPrice = false;
        $scope.showStickerPrice = false;
        $scope.showTableLink = false;
        $scope.showTable = false;
        $scope.dialogShown = false;
        $scope.showTableLabel = 'Detail';
        $scope.showDefaultEpsGrowthRate = false;
        $scope.showprofEpsGrowthRate = false;
        $scope.showDefaultPe = false;
        $scope.showprofPe = false;
        $scope.lineChartXData = null;
        $scope.lineChartYData = null;
        $scope.lineChartXData2 = null;
        $scope.lineChartYData2 = null;
        $scope.historicalYear = null;
        $scope.historicalCashflow = null;
        $scope.historicalEps = null;
        $scope.historicalEquity = null;
        $scope.historicalSales = null;
        $scope.showTableLink = false;
    }//clearAll
});//stickerPriceApp

angular.module('stickerPriceApp').service('tickerService', function ($http, $q) {

    /**
     * capture exchange info, returns a Promise
     */
    this.getExchange = function (ticker){
        if (!ticker){
            var deferred = $q.defer();
            deferred.reject("bad request: missing ticker");
            return deferred.promise;
        }
        var url = "http://www.morningstar.com/api/v1/autocomplete/1/us/" + ticker;
        return $http.jsonp(url);
    };

    /**
     * returns a wrapper that parse json off exchange info
     */
    this.processExchangeResponse = function(response){
        var detail = new Object();
        var result = response.data.m[0];
        detail.query = result.k;
        result = result.r[0];
        detail.exchange = result.LS01Z;
        detail.country = result.XI018;
        console.log("[tickerService] " + JSON.stringify(detail));
        return detail;
    };

    /**
     * capture current price using a wrapper, returns a Promise
     */
    this.getCurrentPrice = function(exchangeJson){
        var url = "http://quotes.morningstar.com/stock/c-header?&t={{query}}&region=usa&culture=en-US&version=RET&cur=";
        url = url.replace("{{query}}", exchangeJson.morningStarQuery);
        return $http.get(url);
    };

    /**
     * returns a Promise of current market price based on html obtain from getCurrentPrice
     */
    this.processCurrentPrice = function(html){
        var url = "parseMarketPrice";
        return $http.post(url, html);
    };

    /**
     * returns a Promise of stickerPrice, MOA based on value of currentEps, estimatedEpsGrowthRate, and
     *  estimatedPe wrapped within rule1 object
     */
    this.processStickerPrice = function(rule1){
        var url = "getStickerPrice?currentEPS={{query1}}&estimatedEPSgrowthRate={{query2}}&estimatedPe={{query3}}";
        if(!rule1.currentEps){
            rule1.currentEps = "";
        }
        if(!rule1.estimatedEpsGrowthRate){
            rule1.estimatedEpsGrowthRate = "";
        }
        if(!rule1.estimatedPe){
            rule1.estimatedPe = "";
        }
        url = url.replace("{{query1}}", rule1.currentEps);
        url = url.replace("{{query2}}", rule1.estimatedEpsGrowthRate);
        url = url.replace("{{query3}}", rule1.estimatedPe);
        return $http.get(url);
    };
});//tickerService


angular.module('stickerPriceApp').service('ratioService', function ($http) {

    /**
     * capture big5 ratio in csv, returns a Promise
     */
    this.getCsv = function (exchangeJson) {
        var url = "http://financials.morningstar.com/ajax/exportKR2CSV.html?&callback=?&t={{query}}&region=usa&culture=en-US&cur=&order=asc";
        url = url.replace("{{query}}", exchangeJson.morningStarQuery);
        return $http.get(url);
    };

    /**
     * returns a Promise of big5 values based on csv obtain from getCsv
     */
    this.processCsv = function(csv){
        var url = "parseKeyRatio";
        var config = {
            headers : {'Content-Type': 'text/plain; charset=UTF-8;'}
        };
        return $http.post(url, csv, config);
    };
});//ratioService


angular.module('stickerPriceApp').service('peService', function ($http) {

    /**
     * capture PE history, returns a Promise
     */
    this.getHistoricalPe = function (exchangeJson) {
        var url = "http://financials.morningstar.com/valuation/valuation-history.action?&t={{query}}&region=usa&culture=en-US&cur=&type=price-earnings";
        url = url.replace("{{query}}", exchangeJson.morningStarQuery);
        return $http.get(url);
    };

    /**
     * returns a Promise of average PE based on history captured in getPe
     */
    this.processHistoricalPe = function(html){
        var url = "parseHistoricalPe";
        return $http.post(url, html);
    };

    /**
     * return a Promise of future estunated PE based on values off
     * historicalPeAvg and defaultPE wrapped within rule1 object
     */
    this.processFutureEstimatePe = function(rule1){
        var url = "getFutureEstimatedPe?defaultPE={{query1}}&historicalPeAvg={{query2}}";
        url = url.replace("{{query1}}", rule1.defaultPE);
        if(!rule1.historicalPeAvg){
            rule1.historicalPeAvg = "";
        }
        url = url.replace("{{query2}}", rule1.historicalPeAvg);
        return $http.get(url);
    };
});//peService


angular.module('stickerPriceApp').service('epsService', function ($http) {

    /**
     * capture prof eps growth rate, returns a Promise
     */
    this.getProfEpsGrowthRate = function(exchangeJson){
        var url = "http://financials.morningstar.com/valuation/forward-comparisons-list.action?&t={{query}}&region=usa&culture=en-US";
        url = url.replace("{{query}}", exchangeJson.morningStarQuery);
        return $http.get(url);
    };

    /**
     * returns a Promise of professional EPS growthrate based on html from getProfEpsGrowthRate
     */
    this.processProfEpsGrowthRate = function(html, exchangeJson){
        var req = {
            method : 'POST',
            url : 'parseProfEpsGrowthRate',
            data : {
                html : html,
                ticker : exchangeJson.query
            }
        };
        
        return $http(req);
    };

    /**
     * returns a Promise of future EPS growth rate based on values off
     * getProfEpsGrowthRate and processProfEpsGrowthRate wrapped within rule1 object
     */
    this.processFutureEpsGrowthRate = function(rule1){
        var url = "getFutureEpsGrowthrateDefaultPe?guessEpsGrowthRate={{query1}}&profEpsGrowthRate={{query2}}";
        url = url.replace("{{query1}}", rule1.guessEpsGrowthRate);
        if(!rule1.profEpsGrowthRate){
            rule1.profEpsGrowthRate = "";
        }
        url = url.replace("{{query2}}", rule1.profEpsGrowthRate);
        return $http.get(url);
    };
});//epsService