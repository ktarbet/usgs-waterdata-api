.\gradlew clean integrationTest --tests "org.opendcs.usgs.waterdata.UsgsWaterDataApiTest.getContinuousTimeSeriesDuplicateStatistic" --info -PusgsDebug=true --rerun-tasks

:: .\gradlew integrationTest --rerun-tasks --info -PusgsDebug=true

::run one integration test method
::.\gradlew integrationTest --tests "org.opendcs.usgs.waterdata.UsgsWaterDataApiTest.dailyData_userScenario"

 ::.\gradlew  demo --info -PusgsDebug=true