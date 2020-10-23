<시계열 데이터에서 추세선 뽑기> 
원리 : 시간 간격을 다르게 함

쿼리문: "SELECT mean("원하는 속성") FROM "데이터베이스" WHERE time >= now() - 6h GROUP BY time(1m) fill(linear);SELECT mean("원하는 속성(위와 같음 값)") FROM "데이터베이스" WHERE time >= now() - 6h GROUP BY time(2h) fill(null)"

예제)

"SELECT mean("meter0/ActivePowerL1") FROM "data" WHERE time >= now() - 6h GROUP BY time(1m) fill(linear);SELECT mean("meter0/ActivePowerL1") FROM "data" WHERE time >= now() - 6h GROUP BY time(2h) fill(null)"