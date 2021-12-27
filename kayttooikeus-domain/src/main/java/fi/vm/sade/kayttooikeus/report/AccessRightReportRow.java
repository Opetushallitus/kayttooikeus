package fi.vm.sade.kayttooikeus.report;

import lombok.Generated;
import lombok.Getter;

import javax.persistence.Id;
import java.math.BigInteger;
import java.util.Date;

@Generated
@Getter
public class AccessRightReportRow {
    @Id
    private BigInteger id;
    private String personName;
    private String personOid;
    private String organisationName;
    private String organisationOid;
    private String accessRightName;
    private BigInteger accessRightId;
    private Date startDate;
    private Date endDate;
    private Date modified;
    private String modifiedBy;

    public AccessRightReportRow() {
    }

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

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }
}
