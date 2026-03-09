.\gradlew clean integrationTest --tests "ktarbet.usgs.waterdata.UsgsWaterDataApiTest.getContinuousTimeSeries" --info -PusgsDebug=true --rerun-tasks

:: .\gradlew integrationTest --rerun-tasks --info -PusgsDebug=true

::run one integration test method
::.\gradlew integrationTest --tests "ktarbet.usgs.waterdata.UsgsWaterDataApiTest.dailyData_userScenario"