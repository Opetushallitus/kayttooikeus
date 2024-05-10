-- korvataan aiemmassa migraatiossa lisätty käyttöoikeus tarkemmilla
select deletekayttooikeus('VALPAS', 'OPPILAITOS');
select insertkayttooikeus('VALPAS', 'OPPILAITOS_HAKEUTUMINEN', 'Saa käsitellä oppilaitoksen hakeutumisen valvonnan tietoja');
select insertkayttooikeus('VALPAS', 'OPPILAITOS_SUORITTAMINEN', 'Saa käsitellä oppilaitoksen oppivelvollisuuden suorittamisen valvonnan tietoja');
select insertkayttooikeus('VALPAS', 'OPPILAITOS_MAKSUTTOMUUS', 'Saa käsitellä oppilaitoksen opintojen maksuttomuuden määrittelyn tietoja');
