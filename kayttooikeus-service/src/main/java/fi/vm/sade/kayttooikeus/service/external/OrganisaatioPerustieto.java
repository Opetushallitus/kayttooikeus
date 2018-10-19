package fi.vm.sade.kayttooikeus.service.external;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import lombok.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrganisaatioPerustieto {

    private static final Map<String, String> ORGANISAATIOTYYPIT;

    static {
        Map<String, String> tmp = new LinkedHashMap<>();
        tmp.put("KOULUTUSTOIMIJA", "organisaatiotyyppi_01");
        tmp.put("OPPILAITOS", "organisaatiotyyppi_02");
        tmp.put("TOIMIPISTE", "organisaatiotyyppi_03");
        tmp.put("OPPISOPIMUSTOIMIPISTE", "organisaatiotyyppi_04");
        tmp.put("MUU_ORGANISAATIO", "organisaatiotyyppi_05");
        tmp.put("TYOELAMAJARJESTO", "organisaatiotyyppi_06");
        tmp.put("VARHAISKASVATUKSEN_JARJESTAJA", "organisaatiotyyppi_07");
        tmp.put("VARHAISKASVATUKSEN_TOIMIPAIKKA", "organisaatiotyyppi_08");
        ORGANISAATIOTYYPIT = unmodifiableMap(tmp);
    }

    private String oid;
    private String parentOidPath;
    private String oppilaitostyyppi;
    private Map<String, String> nimi = new HashMap<>();
    private List<String> organisaatiotyypit = new ArrayList<>();
    private List<String> tyypit = new ArrayList<>();
    private List<OrganisaatioPerustieto> children = new ArrayList<>();
    @JsonIgnore // avoid recursion if this is returned in JSON
    private OrganisaatioPerustieto parent;
    private OrganisaatioStatus status;
    
    public List<String> getTyypit() {
        if (this.organisaatiotyypit != null && !this.organisaatiotyypit.isEmpty()) {
            return this.organisaatiotyypit;
        }
        return this.tyypit;
    }

    /**
     * Palauttaa organisaation tyypit koodiston koodiUri-muodossa.
     * @return organisaatiotyypit
     */
    @JsonIgnore // pidetään tämä ainakin toistaiseksi piilossa frontilta ettei eri formaatit sekoita
    public List<String> getOrganisaatiotyyppiKoodit() {
        return Optional.ofNullable(organisaatiotyypit)
                .map(tyypit -> tyypit.stream().map(ORGANISAATIOTYYPIT::get).filter(Objects::nonNull).collect(toList()))
                .orElse(emptyList());
    }

    public boolean hasOrganisaatiotyyppiKoodi(String organisaatiotyyppiKoodi) {
        return hasAnyOrganisaatiotyyppiKoodi(singletonList(organisaatiotyyppiKoodi));
    }

    public boolean hasAnyOrganisaatiotyyppiKoodi(Collection<String> organisaatiotyyppiKoodit) {
        return getOrganisaatiotyyppiKoodit().stream().anyMatch(organisaatiotyyppiKoodit::contains);
    }

    public Stream<OrganisaatioPerustieto> andChildren() {
        return Stream.concat(Stream.of(this),
                children.stream().flatMap(OrganisaatioPerustieto::andChildren));
    }

    public Stream<OrganisaatioPerustieto> andParents() {
        return Stream.concat(Stream.of(this), parents());
    }

    public Stream<OrganisaatioPerustieto> parents() {
        return parent == null ? Stream.empty() : parent.andParents();
    }
    
    public int getHierarchyLevel() {
        int level = 1;
        OrganisaatioPerustieto node = this;
        while (node.parent != null) {
            ++level;
            node = node.parent;
        }
        return level;
    }
}
