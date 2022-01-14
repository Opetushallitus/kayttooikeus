package fi.vm.sade.kayttooikeus.report;

import lombok.Generated;
import lombok.Getter;

import lombok.Setter;

import java.math.BigInteger;
import java.util.Date;

@Generated
@Getter
public class AccessRightReportRow {

    private final BigInteger id;
    private final String personName;
    private final String personOid;
    private final String organisationOid;
    private final String accessRightName;
    private final BigInteger accessRightId;
    private final Date startDate;
    private final Date endDate;
    private final Date modified;
    private final String modifiedBy;
    @Setter
    private String organisationName;

    public AccessRightReportRow(
            BigInteger id,
            String personName,
            String personOid,
            String organisationName,
            String organisationOid,
            String accessRightName,
            BigInteger accessRightId,
            Date startDate,
            Date endDate,
            Date modified,
            String modifiedBy) {
        this.id = id;
        this.personName = personName;
        this.personOid = personOid;
        this.organisationName = organisationName;
        this.organisationOid = organisationOid;
        this.accessRightName = accessRightName;
        this.accessRightId = accessRightId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.modified = modified;
        this.modifiedBy = modifiedBy;
    }
}
