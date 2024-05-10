UPDATE organisaatioviite SET
    organisaatio_tyyppi = 'oppilaitostyyppi_' || organisaatio_tyyppi
WHERE
    -- not already in koodiuri format
    position('oppilaitostyyppi_' in organisaatio_tyyppi) = 0
    -- not oid
    AND position('.' in organisaatio_tyyppi) = 0
    -- not organisaatiotyyppi uri
    AND position('organisaatiotyyppi_' in organisaatio_tyyppi) = 0
