# Flyway skriptien formaatti
VYYYYMMDDhhmmssSSS__<skriptin nimi>.sql
Y=vuosi
M=kuukausi
D=päivä
h=tunti
m=minuutti
s=sekunti
S=millisekunti

Tiedoston nimen voi generoida näin:
  
    echo V`date +"%Y%m%d%H%M%S000"`__my_feature.sql
